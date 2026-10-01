package cn.hamm.airpower.core;

import org.junit.jupiter.api.*;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>TaskUtil 单元测试</h1>
 *
 * <p>任务在线程池中异步执行，统一使用 CountDownLatch 等待结果，并为每个用例设置 15 秒超时防止挂死。</p>
 *
 * @author Hamm.cn
 */
@DisplayName("TaskUtil 异步任务工具类单元测试")
@Timeout(15)
class TaskUtilTest {

    /**
     * 等待异步任务完成的超时时间（秒）
     */
    private static final long AWAIT_SECONDS = 10L;

    @BeforeEach
    @DisplayName("用例执行前清空 MDC，避免用例间互相污染")
    void clearMdcBefore() {
        MDC.clear();
    }

    @AfterEach
    @DisplayName("用例执行后清理 MDC")
    void clearMdcAfter() {
        TraceUtil.resetTraceId();
        MDC.clear();
    }

    @Nested
    @DisplayName("run 异步执行任务")
    class RunTest {

        @Test
        @DisplayName("正常路径：单个任务被执行且不阻塞调用方")
        void testRunSingleTask() throws InterruptedException {
            AtomicReference<String> result = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);

            TaskUtil.run(() -> {
                result.set("已完成");
                latch.countDown();
            });

            assertNull(result.get(), "run 提交后应立即返回，任务结果此刻通常还未写入");
            assertTrue(latch.await(AWAIT_SECONDS, TimeUnit.SECONDS), "等待单个异步任务完成超时");
            assertEquals("已完成", result.get(), "异步任务应被完整执行一次");
        }

        @Test
        @DisplayName("正常路径：多个任务全部执行")
        void testRunMultipleTasks() throws InterruptedException {
            int taskCount = 5;
            CountDownLatch latch = new CountDownLatch(taskCount);
            List<String> resultList = Collections.synchronizedList(new ArrayList<>());
            AtomicInteger counter = new AtomicInteger(0);

            TaskUtil.run(
                    () -> {
                        resultList.add("任务" + counter.incrementAndGet());
                        latch.countDown();
                    },
                    () -> {
                        resultList.add("任务" + counter.incrementAndGet());
                        latch.countDown();
                    },
                    () -> {
                        resultList.add("任务" + counter.incrementAndGet());
                        latch.countDown();
                    },
                    () -> {
                        resultList.add("任务" + counter.incrementAndGet());
                        latch.countDown();
                    },
                    () -> {
                        resultList.add("任务" + counter.incrementAndGet());
                        latch.countDown();
                    }
            );

            assertTrue(latch.await(AWAIT_SECONDS, TimeUnit.SECONDS), "等待 5 个异步任务全部完成超时");
            assertEquals(taskCount, resultList.size(), "传入的 5 个任务应全部被执行");
        }

        @Test
        @DisplayName("边界值：只传一个任务时等价于单任务重载")
        void testRunWithVarargsOfOne() throws InterruptedException {
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<String> result = new AtomicReference<>();

            TaskUtil.run(() -> {
                result.set("唯一任务");
                latch.countDown();
            }, new Runnable[0]);

            assertTrue(latch.await(AWAIT_SECONDS, TimeUnit.SECONDS), "等待异步任务完成超时");
            assertEquals("唯一任务", result.get(), "varargs 为空数组时仍应执行首个任务");
        }

        @Test
        @DisplayName("边界值：任务可并发执行，无需等待前一个完成")
        void testTasksRunConcurrently() throws InterruptedException {
            CountDownLatch allStarted = new CountDownLatch(3);
            CountDownLatch release = new CountDownLatch(1);

            Runnable blockingTask = () -> {
                allStarted.countDown();
                try {
                    release.await(AWAIT_SECONDS, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            };

            TaskUtil.run(blockingTask, blockingTask, blockingTask);

            assertTrue(allStarted.await(AWAIT_SECONDS, TimeUnit.SECONDS), "3 个阻塞任务应能被并发调度");
            release.countDown();
        }
    }

    @Nested
    @DisplayName("异常处理")
    class ExceptionTest {

        @Test
        @DisplayName("异常分支：任务抛出异常不会抛给调用方")
        void testTaskExceptionDoesNotPropagate() {
            assertDoesNotThrow(() -> TaskUtil.run(() -> {
                throw new IllegalStateException("任务内部异常");
            }), "run 的契约是不抛出异常，任务异常只应在日志中记录");
        }

        @Test
        @DisplayName("异常分支：单个任务失败不影响其他任务执行")
        void testFailingTaskDoesNotAffectOthers() throws InterruptedException {
            CountDownLatch latch = new CountDownLatch(2);
            AtomicReference<String> first = new AtomicReference<>();
            AtomicReference<String> second = new AtomicReference<>();

            TaskUtil.run(
                    () -> {
                        first.set("第一个任务");
                        latch.countDown();
                    },
                    () -> {
                        throw new RuntimeException("第二个任务故意失败");
                    },
                    () -> {
                        second.set("第三个任务");
                        latch.countDown();
                    }
            );

            assertTrue(latch.await(AWAIT_SECONDS, TimeUnit.SECONDS), "等待未失败的两个任务完成超时");
            assertEquals("第一个任务", first.get(), "失败任务之前的任务应正常执行");
            assertEquals("第三个任务", second.get(), "失败任务之后的任务也应正常执行");
        }

        @Test
        @DisplayName("异常分支：线程池在任务失败后仍可继续使用")
        void testPoolStillUsableAfterFailure() throws InterruptedException {
            TaskUtil.run(() -> {
                throw new RuntimeException("故意失败");
            });

            AtomicReference<String> result = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);
            TaskUtil.run(() -> {
                result.set("失败后的任务");
                latch.countDown();
            });

            assertTrue(latch.await(AWAIT_SECONDS, TimeUnit.SECONDS), "任务失败后线程池应仍能接收新任务");
            assertEquals("失败后的任务", result.get(), "失败任务不应影响后续任务执行");
        }

        @Test
        @DisplayName("异常分支：任务为 null 时不会抛给调用方")
        void testNullTaskDoesNotPropagate() throws InterruptedException {
            assertDoesNotThrow(() -> TaskUtil.run((Runnable) null), "任务为 null 时内部异常应被捕获，不应抛给调用方");

            AtomicReference<String> result = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);
            TaskUtil.run(() -> {
                result.set("空任务之后的任务");
                latch.countDown();
            });
            assertTrue(latch.await(AWAIT_SECONDS, TimeUnit.SECONDS), "等待后续任务完成超时");
            assertEquals("空任务之后的任务", result.get(), "空任务不应影响后续任务执行");
        }
    }

    @Nested
    @DisplayName("TraceID 传递")
    class TraceIdTest {

        @Test
        @DisplayName("正常路径：调用方的 TraceID 被传递到异步线程")
        void testTraceIdIsPropagated() throws InterruptedException {
            String expected = "测试-TraceId";
            TraceUtil.setTraceId(expected);
            AtomicReference<String> asyncTraceId = new AtomicReference<>();
            AtomicReference<String> asyncThreadName = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);

            TaskUtil.run(() -> {
                asyncTraceId.set(TraceUtil.getTraceId());
                asyncThreadName.set(Thread.currentThread().getName());
                latch.countDown();
            });

            assertTrue(latch.await(AWAIT_SECONDS, TimeUnit.SECONDS), "等待异步任务完成超时");
            assertEquals(expected, asyncTraceId.get(), "异步线程中应能读到与调用方相同的 TraceID");
            assertTrue(asyncThreadName.get().startsWith("airpower-task-"),
                    "异步任务应在 airpower-task- 前缀的线程池线程中执行，实际为：" + asyncThreadName.get());
            assertEquals(expected, TraceUtil.getTraceId(), "异步执行不应影响调用方线程的 TraceID");
        }

        @Test
        @DisplayName("空值分支：调用方无 TraceID 时异步线程自动生成 UUID")
        void testTraceIdGeneratedWhenAbsent() throws InterruptedException {
            MDC.clear();
            assertNull(TraceUtil.getTraceId(), "用例开始时调用方线程不应有 TraceID");

            AtomicReference<String> asyncTraceId = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);
            TaskUtil.run(() -> {
                asyncTraceId.set(TraceUtil.getTraceId());
                latch.countDown();
            });

            assertTrue(latch.await(AWAIT_SECONDS, TimeUnit.SECONDS), "等待异步任务完成超时");
            String traceId = asyncTraceId.get();
            assertNotNull(traceId, "调用方 TraceID 为 null 时，异步线程应自行生成 UUID");
            assertDoesNotThrow(() -> UUID.fromString(traceId), "异步线程生成的 TraceID 应为合法 UUID：" + traceId);
        }

        @Test
        @DisplayName("正常路径：多个任务各自持有相同的 TraceID")
        void testTraceIdSharedByMultipleTasks() throws InterruptedException {
            String expected = "multi-task-trace";
            TraceUtil.setTraceId(expected);
            CountDownLatch latch = new CountDownLatch(4);
            List<String> collected = Collections.synchronizedList(new ArrayList<>());

            Runnable task = () -> {
                collected.add(TraceUtil.getTraceId());
                latch.countDown();
            };
            TaskUtil.run(task, task, task, task);

            assertTrue(latch.await(AWAIT_SECONDS, TimeUnit.SECONDS), "等待 4 个异步任务完成超时");
            assertEquals(4, collected.size(), "4 个任务都应完成");
            for (String traceId : collected) {
                assertEquals(expected, traceId, "每个异步任务内的 TraceID 都应与调用方一致");
            }
        }

        @Test
        @DisplayName("任务结束后线程池线程的 TraceID 被清理，避免残留到下一个任务")
        void testTraceIdClearedAfterTask() throws InterruptedException {
            TraceUtil.setTraceId("to-be-cleared");
            CountDownLatch done = new CountDownLatch(1);
            TaskUtil.run(done::countDown);
            assertTrue(done.await(AWAIT_SECONDS, TimeUnit.SECONDS), "等待异步任务完成超时");

            // 用一个独立线程读取线程池线程清理后的状态：连续执行两个任务，
            // 第二个任务开始时若残留了上一个任务的 TraceID，这里能观察到
            AtomicReference<String> firstTaskTrace = new AtomicReference<>();
            CountDownLatch ready = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            TaskUtil.run(() -> {
                firstTaskTrace.set(TraceUtil.getTraceId());
                ready.countDown();
                // 保持线程占用，等待清理完成
                try {
                    release.await(AWAIT_SECONDS, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            assertTrue(ready.await(AWAIT_SECONDS, TimeUnit.SECONDS), "第一个任务未按时启动");
            release.countDown();
            assertNotNull(firstTaskTrace.get(), "任务执行期间应能读到 TraceID");
        }
    }

    @Nested
    @DisplayName("CallerRunsPolicy 饱和路径")
    class CallerRunsTest {

        /**
         * 池与队列的总容量：最大线程数 + 有界队列长度，与 TaskUtil 的配置一致
         */
        private static final int SATURATION = Math.max(2, Runtime.getRuntime().availableProcessors()) * 2 + 1000;

        /**
         * 堵住线程池的阻塞时长（秒）
         */
        private static final long BLOCK_SECONDS = 10L;

        /**
         * 等待线程池进入饱和（探测任务开始跑在调用方线程上）
         *
         * @throws InterruptedException 中断异常
         */
        private void awaitSaturation() throws InterruptedException {
            for (int i = 0; i < 200; i++) {
                if (isCallerRuns()) {
                    return;
                }
                Thread.sleep(50L);
            }
            fail("线程池在预期时间内没有进入饱和状态，无法验证 CallerRuns 路径");
        }

        /**
         * 等待线程池排空
         *
         * @throws InterruptedException 中断异常
         * @apiNote 线程池是全进程共享的静态单例，本用例把它塞满后必须等它排空，
         * 否则紧随其后的用例会撞上 CallerRuns，把「异步任务应跑在线程池线程上」
         * 这类断言弄成偶发失败
         */
        private void awaitDrain() throws InterruptedException {
            for (int i = 0; i < 400; i++) {
                if (!isCallerRuns()) {
                    return;
                }
                Thread.sleep(50L);
            }
            fail("线程池在预期时间内没有排空，会影响后续用例");
        }

        /**
         * 判断当前是否处于 CallerRuns（任务跑在调用方线程上）
         *
         * @return 是否为 CallerRuns
         * @apiNote 探测任务不阻塞：即便 CallerRuns 生效也会立刻返回
         */
        private boolean isCallerRuns() {
            AtomicReference<Thread> probe = new AtomicReference<>();
            TaskUtil.run(() -> probe.set(Thread.currentThread()));
            return probe.get() == Thread.currentThread();
        }

        @Test
        @DisplayName("队列打满时不得擦掉调用线程自己的 TraceID")
        void mustNotClearCallerTraceId() throws InterruptedException {
            CountDownLatch release = new CountDownLatch(1);
            try {
                // 把池线程与队列全部占满：这些任务都阻塞在 release 上
                for (int i = 0; i < SATURATION; i++) {
                    TaskUtil.run(() -> {
                        try {
                            release.await(BLOCK_SECONDS, TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    });
                }
                awaitSaturation();

                // 饱和确认之后才设置 TraceID，避免被前面的探测任务影响
                String expected = "调用方-TraceId";
                TraceUtil.setTraceId(expected);
                AtomicReference<Thread> executedOn = new AtomicReference<>();
                TaskUtil.run(() -> executedOn.set(Thread.currentThread()));

                assertSame(Thread.currentThread(), executedOn.get(),
                        "前置条件不成立：这一步的任务没有跑在调用方线程上，断言会空过");
                assertEquals(expected, TraceUtil.getTraceId(),
                        "CallerRuns 下任务跑在调用方线程上，finally 里清 MDC 会擦掉该请求自己的 TraceID");
            } finally {
                release.countDown();
                awaitDrain();
                MDC.clear();
            }
        }

        @Test
        @DisplayName("饱和路径不应影响非饱和路径的 TraceID 继承")
        void stillPropagatesTraceIdUnderSaturation() throws InterruptedException {
            CountDownLatch release = new CountDownLatch(1);
            try {
                for (int i = 0; i < SATURATION; i++) {
                    TaskUtil.run(() -> {
                        try {
                            release.await(BLOCK_SECONDS, TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    });
                }
                awaitSaturation();

                String expected = "饱和时的-TraceId";
                TraceUtil.setTraceId(expected);
                AtomicReference<String> seen = new AtomicReference<>("未执行");
                TaskUtil.run(() -> seen.set(TraceUtil.getTraceId()));

                assertEquals(expected, seen.get(), "即使走 CallerRuns，任务内部仍应读到调用方的 TraceID");
                assertEquals(expected, TraceUtil.getTraceId(), "调用方线程的 TraceID 不应被改变");
            } finally {
                release.countDown();
                awaitDrain();
                MDC.clear();
            }
        }
    }
}
