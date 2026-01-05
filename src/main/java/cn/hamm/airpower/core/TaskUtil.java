package cn.hamm.airpower.core;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * <h1>任务流程工具类</h1>
 *
 * @author Hamm.cn
 */
@Slf4j
public class TaskUtil {
    /**
     * 线程池
     */
    @SuppressWarnings("AlibabaThreadShouldSetName")
    private static final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(
            5,
            20,
            3600L,
            SECONDS,
            new LinkedBlockingQueue<>()
    );

    /**
     * 执行任务 {@code 不会抛出异常}
     *
     * @param runnable     任务
     * @param moreRunnable 更多任务
     */
    public static void run(Runnable runnable, Runnable... moreRunnable) {
        getRunnableList(runnable, moreRunnable).forEach(run -> {
            try {
                run.run();
            } catch (Exception exception) {
                log.error(exception.getMessage(), exception);
            }
        });
    }

    /**
     * 异步执行任务 {@code 不会抛出异常}
     *
     * @param runnable     任务
     * @param moreRunnable 更多任务
     */
    public static void runAsync(Runnable runnable, Runnable... moreRunnable) {
        getRunnableList(runnable, moreRunnable).forEach((run) -> EXECUTOR.submit(() -> {
            try {
                run.run();
            } catch (Exception exception) {
                log.error("异步执行任务失败, {}", exception.getMessage(), exception);
            }
        }));
    }

    /**
     * 获取任务列表
     *
     * @param runnable     任务
     * @param moreRunnable 更多任务
     * @return 任务列表
     */
    private static @NotNull List<Runnable> getRunnableList(Runnable runnable, Runnable[] moreRunnable) {
        List<Runnable> runnableList = new ArrayList<>();
        runnableList.add(runnable);
        runnableList.addAll(Arrays.asList(moreRunnable));
        return runnableList;
    }
}
