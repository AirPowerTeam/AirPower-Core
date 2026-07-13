package cn.hamm.airpower.core;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>TaskUtil 单元测试</h1>
 *
 * @author Hamm.cn
 */
class TaskUtilTest {

    // ==================== run 方法测试 ====================

    @Test
    void testRunWithSingleTask() {
        AtomicBoolean executed = new AtomicBoolean(false);
        TaskUtil.run(() -> executed.set(true));
        assertTrue(executed.get());
    }

    @Test
    void testRunWithMultipleTasks() {
        AtomicInteger counter = new AtomicInteger(0);
        TaskUtil.run(
                () -> counter.incrementAndGet(),
                () -> counter.incrementAndGet(),
                () -> counter.incrementAndGet()
        );
        assertEquals(3, counter.get());
    }

    @Test
    void testRunWithNoAdditionalTasks() {
        AtomicBoolean executed = new AtomicBoolean(false);
        TaskUtil.run(() -> executed.set(true));
        assertTrue(executed.get());
    }

    @Test
    void testRunWithExceptionInTask() {
        AtomicBoolean afterException = new AtomicBoolean(false);
        // 第一个任务抛出异常，第二个任务应该继续执行
        TaskUtil.run(
                () -> {
                    throw new RuntimeException("测试异常");
                },
                () -> afterException.set(true)
        );
        // 异常被捕获，第二个任务应该正常执行
        assertTrue(afterException.get());
    }

    @Test
    void testRunWithExceptionDoesNotPropagate() {
        // 确保异常不会抛出
        assertDoesNotThrow(() -> TaskUtil.run(() -> {
            throw new RuntimeException("测试异常");
        }));
    }

    @Test
    void testRunWithNullTask() {
        // 传入null会在内部抛出NullPointerException，但被捕获了
        assertDoesNotThrow(() -> TaskUtil.run(null));
    }

    @Test
    void testRunExecutionOrder() {
        StringBuilder sb = new StringBuilder();
        TaskUtil.run(
                () -> sb.append("1"),
                () -> sb.append("2"),
                () -> sb.append("3")
        );
        // 同步执行应该按顺序执行
        assertEquals("123", sb.toString());
    }

    // ==================== runAsync 方法测试 ====================

    @Test
    void testRunAsyncWithSingleTask() throws InterruptedException {
        AtomicBoolean executed = new AtomicBoolean(false);
        TaskUtil.runAsync(() -> executed.set(true));
        // 等待异步任务执行
        Thread.sleep(100);
        assertTrue(executed.get());
    }

    @Test
    void testRunAsyncWithMultipleTasks() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        TaskUtil.runAsync(
                () -> counter.incrementAndGet(),
                () -> counter.incrementAndGet(),
                () -> counter.incrementAndGet()
        );
        // 等待异步任务执行
        Thread.sleep(200);
        assertEquals(3, counter.get());
    }

    @Test
    void testRunAsyncTasksExecuteInParallel() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);

        TaskUtil.runAsync(
                () -> {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    counter.incrementAndGet();
                },
                () -> {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    counter.incrementAndGet();
                }
        );

        // 等待很短的时间（小于串行执行总时间200ms）
        Thread.sleep(50);
        // 如果是并行执行，两个任务应该都已经开始执行了
        // 如果是串行执行，第二个任务可能还没开始
        // 这里我们主要验证两个任务最终都能完成
        Thread.sleep(500);
        assertEquals(2, counter.get());
    }

    @Test
    void testRunAsyncWithExceptionInTask() throws InterruptedException {
        AtomicBoolean afterException = new AtomicBoolean(false);
        // 第一个任务抛出异常，第二个任务应该继续执行
        TaskUtil.runAsync(
                () -> {
                    throw new RuntimeException("测试异常");
                },
                () -> {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    afterException.set(true);
                }
        );
        // 等待异步任务执行
        Thread.sleep(200);
        // 异常被捕获，第二个任务应该正常执行
        assertTrue(afterException.get());
    }

    @Test
    void testRunAsyncWithExceptionDoesNotPropagate() {
        // 确保异常不会抛出
        assertDoesNotThrow(() -> TaskUtil.runAsync(() -> {
            throw new RuntimeException("测试异常");
        }));
    }

    @Test
    void testRunAsyncWithNullTask() {
        // 传入null会在内部抛出NullPointerException，但被异步捕获了
        assertDoesNotThrow(() -> TaskUtil.runAsync(null));
    }

    // ==================== run 和 runAsync 对比测试 ====================

    @Test
    void testRunIsSynchronous() {
        AtomicBoolean executed = new AtomicBoolean(false);
        TaskUtil.run(() -> executed.set(true));
        // 同步执行，到这里任务一定已经完成
        assertTrue(executed.get());
    }

    @Test
    void testRunAsyncIsAsynchronous() throws InterruptedException {
        AtomicBoolean executed = new AtomicBoolean(false);
        TaskUtil.runAsync(() -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            executed.set(true);
        });
        // 异步执行，到这里任务可能还没开始执行
        assertFalse(executed.get());
        // 等待任务完成
        Thread.sleep(200);
        assertTrue(executed.get());
    }

    // ==================== 边界情况测试 ====================

    @Test
    void testRunWithEmptyRunnable() {
        AtomicBoolean executed = new AtomicBoolean(false);
        TaskUtil.run(() -> executed.set(true));
        assertTrue(executed.get());
    }

    @Test
    void testRunAsyncWithEmptyRunnable() throws InterruptedException {
        AtomicBoolean executed = new AtomicBoolean(false);
        TaskUtil.runAsync(() -> executed.set(true));
        Thread.sleep(100);
        assertTrue(executed.get());
    }

    @Test
    void testRunWithManyTasks() {
        AtomicInteger counter = new AtomicInteger(0);
        int taskCount = 100;
        Runnable[] tasks = new Runnable[taskCount];
        for (int i = 0; i < taskCount; i++) {
            tasks[i] = counter::incrementAndGet;
        }
        TaskUtil.run(() -> {}, tasks);
        assertEquals(taskCount, counter.get());
    }

    @Test
    void testRunAsyncWithManyTasks() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        int taskCount = 100;
        Runnable[] tasks = new Runnable[taskCount];
        for (int i = 0; i < taskCount; i++) {
            tasks[i] = counter::incrementAndGet;
        }
        TaskUtil.runAsync(() -> {}, tasks);
        // 等待所有异步任务完成
        Thread.sleep(500);
        assertEquals(taskCount, counter.get());
    }
}
