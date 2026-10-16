package cn.hamm.airpower.core;

import cn.hamm.airpower.core.exception.ServiceException;
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
 * <h2>TaskUtil 单元测试</h2>
 *
 * <p>任务在线程池中异步执行，统一使用 CountDownLatch 等待结果，并为每个用例设置 15 秒超时防止挂死。</p>
 *
 * @author Hamm.cn
 */
@DisplayName("TaskUtil 异步任务工具类单元测试")
@Timeout(15)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
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

    /**
     * 池与队列的总容量：最大线程数 + 有界队列长度，与 TaskUtil 的配置一致
     */
    private static final int SATURATION = Math.max(2, Runtime.getRuntime().availableProcessors()) * 2 + 1000;

    /**
     * 灌满线程池与队列
     *
     * @param release 解除阻塞的闸门
     * @apiNote 循环中途被拒说明已经饱和，正是饱和用例要的前置条件，直接收手
     */
    private static void fill(CountDownLatch release) {
        for (int i = 0; i < SATURATION; i++) {
            try {
                // 无限期阻塞：带超时的阻塞会让先入队的任务在断言之前自行超时结束，
                // 队列腾出空位，「必须被拒绝」就变成偶发失败。释放时机由用例的 finally 决定。
                TaskUtil.run(() -> {
                    try {
                        release.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            } catch (ServiceException e) {
                return;
            }
        }
    }

    /**
     * 等待线程池进入饱和（探测任务被拒绝）
     *
     * @throws InterruptedException 中断异常
     */
    private static void awaitSaturation() throws InterruptedException {
        for (int i = 0; i < 200; i++) {
            if (isRejected()) {
                return;
            }
            Thread.sleep(50L);
        }
        fail("线程池在预期时间内没有进入饱和状态，无法验证拒绝路径");
    }

    /**
     * 等待线程池排空
     *
     * @throws InterruptedException 中断异常
     * @apiNote 线程池是全进程共享的静态单例，塞满后必须等它排空，
     * 否则后续用例会撞上拒绝，把「异步任务应跑在线程池线程上」这类断言
     * 弄成偶发失败
     */
    private static void awaitDrain() throws InterruptedException {
        for (int i = 0; i < 400; i++) {
            if (TaskUtil.isIdle()) {
                return;
            }
            Thread.sleep(50L);
        }
        fail("线程池在预期时间内没有排空，会影响后续用例");
    }

    /**
     * 判断线程池当前是否已饱和（任务提交即被拒绝）
     *
     * @return 是否已饱和
     * @apiNote 探测任务不阻塞：即使被拒绝也只是抛异常，不会占用调用方线程
     */
    private static boolean isRejected() {
        try {
            TaskUtil.run(() -> {
            });
            return false;
        } catch (ServiceException e) {
            return true;
        }
    }

    @Nested
    @DisplayName("run 异步执行任务")
    @Order(1)
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
    @Order(2)
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
    @Order(3)
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
    @DisplayName("饱和拒绝路径")
    @Order(4)
    class SaturationTest {

        @Test
        @DisplayName("队列满时抛业务异常，而不是回退到调用方线程执行")
        void mustRejectInsteadOfRunningOnCallerThread() throws InterruptedException {
            CountDownLatch release = new CountDownLatch(1);
            AtomicReference<Thread> executedOn = new AtomicReference<>();
            try {
                fill(release);
                awaitSaturation();

                // 前置条件：确实已经饱和
                assertTrue(isRejected(), "前置条件不成立：线程池并未饱和，断言会空过");

                // 关键断言：调用方线程不得被执行任务
                ServiceException e = assertThrows(ServiceException.class, () -> {
                    TaskUtil.run(() -> executedOn.set(Thread.currentThread()));
                }, "队列满时应抛业务异常，让调用方感知系统繁忙");
                assertTrue(e.getMessage().contains("繁忙"), "异常消息应说明是繁忙导致，具体为：" + e.getMessage());
                assertNull(executedOn.get(), "任务被拒绝，绝不能在调用方线程上执行");
                assertNull(e.getData(), "data 不得携带异常（铁律 0.9）");
                assertNull(e.getCause(), "拒绝不是异常场景，不应挂 cause");
            } finally {
                release.countDown();
                awaitDrain();
                MDC.clear();
            }
        }

        @Test
        @DisplayName("饱和拒绝不得改变调用方线程的 TraceID")
        void rejectionMustNotTouchCallerTraceId() throws InterruptedException {
            CountDownLatch release = new CountDownLatch(1);
            try {
                fill(release);
                awaitSaturation();

                String expected = "调用方-TraceId";
                TraceUtil.setTraceId(expected);
                assertThrows(ServiceException.class, () -> TaskUtil.run(() -> {
                }), "饱和时任务应被拒绝");
                assertEquals(expected, TraceUtil.getTraceId(),
                        "拒绝路径不应动调用方线程的 MDC");
            } finally {
                release.countDown();
                awaitDrain();
                MDC.clear();
            }
        }
    }

    /**
     * <h2>isIdle 线程池空闲判定</h2>
     *
     * <p>仅凭「提交一次成功」判断排空不可靠：那一刻队列里可能还压着任务，
     * 所以饱和判定必须同时看运行中任务数与队列长度。</p>
     */
    @Nested
    @DisplayName("isIdle 空闲判定")
    @Order(5)
    class IdleTest {

        @Test
        @DisplayName("没有任务时为空闲")
        void idleWhenNoTask() throws InterruptedException {
            // 先确认上一组用例留下的任务已经排空，否则本断言会依赖执行顺序
            for (int i = 0; i < 200 && !TaskUtil.isIdle(); i++) {
                Thread.sleep(50L);
            }
            assertTrue(TaskUtil.isIdle(), "无运行任务且队列为空时才算空闲");
        }

        @Test
        @DisplayName("任务在队列中等待时不算空闲")
        void notIdleWhenQueued() throws InterruptedException {
            CountDownLatch release = new CountDownLatch(1);
            try {
                fill(release);
                awaitSaturation();
                assertFalse(TaskUtil.isIdle(), "队列里压着任务时不得判定为空闲");
            } finally {
                release.countDown();
                awaitDrain();
            }
        }
    }

    /**
     * <h2>shutdownAndAwait 优雅停机</h2>
     *
     * <p>本组用例会真正关掉<b>全进程共享</b>的线程池，且关掉后不可复原，
     * 因此必须排在本类的最后执行（靠 {@code @Order} + {@code @TestMethodOrder} 保证）。</p>
     */
    @Nested
    @Order(6)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @DisplayName("shutdownAndAwait 优雅停机")
    class ShutdownTest {

        @Test
        @Order(1)
        @DisplayName("等待在途任务排空后返回 true")
        void drainsInFlightTasks() throws InterruptedException {
            AtomicInteger done = new AtomicInteger();
            TaskUtil.run(() -> {
                try {
                    Thread.sleep(200L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                done.incrementAndGet();
            });
            assertTrue(TaskUtil.shutdownAndAwait(AWAIT_SECONDS, TimeUnit.SECONDS),
                    "在途任务应在超时前排空并返回 true");
            assertEquals(1, done.get(), "停机返回前在途任务必须已经执行完毕");
        }

        @Test
        @Order(2)
        @DisplayName("关闭后提交任务按「系统繁忙」拒绝，而不是静默丢弃")
        void runAfterShutdownIsRejected() {
            AtomicReference<Boolean> executed = new AtomicReference<>(Boolean.FALSE);
            ServiceException e = assertThrows(ServiceException.class,
                    () -> TaskUtil.run(() -> executed.set(Boolean.TRUE)),
                    "线程池已关闭时提交必须让调用方感知");
            assertTrue(e.getMessage().contains("繁忙"), "异常消息应说明是繁忙导致，具体为：" + e.getMessage());
            assertEquals(Boolean.FALSE, executed.get(), "被拒绝的任务绝不能被执行");
        }

        @Test
        @Order(3)
        @DisplayName("线程池已关闭时重复调用同样返回 true")
        void idempotentAfterShutdown() {
            assertTrue(TaskUtil.shutdownAndAwait(AWAIT_SECONDS, TimeUnit.SECONDS),
                    "已终止的线程池再次 awaitTermination 立即返回 true");
        }

        @Test
        @Order(4)
        @DisplayName("调用方已处于中断态时，中断位不得被吞掉")
        void interruptFlagIsNotSwallowed() {
            // 线程池已终止时 awaitTermination 在第一次状态检查就返回，不会进入
            // 可中断的等待，因此本用例只能锁定「中断位不被清除」这一条；
            // 返回 false 的分支要求线程池仍处于 SHUTDOWN 但未 TERMINATED，
            // 也就是必须还有在途任务——而停机不可逆，同一 JVM 里无法再构造。
            Thread.currentThread().interrupt();
            try {
                TaskUtil.shutdownAndAwait(AWAIT_SECONDS, TimeUnit.SECONDS);
                assertTrue(Thread.currentThread().isInterrupted(),
                        "停机等待不得清除调用方的中断位：被清除后 JDK 内部的锁与信号量"
                                + "会停止响应中断，优雅停机会被拖长");
            } finally {
                Thread.interrupted();
            }
        }
    }
}
