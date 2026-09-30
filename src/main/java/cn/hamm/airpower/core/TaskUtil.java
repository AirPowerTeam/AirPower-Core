package cn.hamm.airpower.core;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * <h1>异步任务工具类</h1>
 *
 * @author Hamm.cn
 * @apiNote 内部线程池全进程共享，任务内的 TraceID 会自动从提交线程继承并在结束后清理
 */
@Slf4j
public class TaskUtil {
    /**
     * 核心线程数（根据 CPU 核心数动态计算）
     */
    private static final int CORE_POOL_SIZE = Math.max(2, Runtime.getRuntime().availableProcessors());

    /**
     * 最大线程数
     */
    private static final int MAX_POOL_SIZE = CORE_POOL_SIZE * 2;

    /**
     * 共享线程池，队列满时由调用方线程执行（不丢任务、不抛拒绝异常）
     */
    @SuppressWarnings("AlibabaThreadShouldSetName")
    private static final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(
            CORE_POOL_SIZE,
            MAX_POOL_SIZE,
            60L,
            SECONDS,
            new LinkedBlockingQueue<>(1000),
            new ThreadFactory() {
                private final AtomicInteger counter = new AtomicInteger(0);

                @Override
                public Thread newThread(@NotNull Runnable r) {
                    Thread thread = new Thread(r, "airpower-task-" + counter.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    /**
     * 异步执行任务，异常只记录日志、不向调用方抛出
     *
     * @param runnable     任务
     * @param moreRunnable 更多任务
     * @apiNote 线程池使用 {@code CallerRunsPolicy}，队列满时任务会在<b>调用方线程</b>
     * 同步执行，此时不再是异步；每个任务各自提交到线程池，彼此不保证先后顺序
     */
    public static void run(Runnable runnable, Runnable... moreRunnable) {
        String traceId = TraceUtil.getTraceId();
        getRunnableList(runnable, moreRunnable).forEach((run) -> EXECUTOR.submit(() -> {
            try {
                TraceUtil.setTraceId(traceId);
                run.run();
            } catch (Throwable e) {
                // 任务由 submit 提交，异常不会传递给调用方，这里统一兜底记录（含 Error）。
                // 必须传异常对象本身，只打印 getMessage() 会丢掉堆栈，线上问题无从定位
                log.error("异步执行任务失败", e);
            } finally {
                // 线程会被复用，清理 MDC 避免 TraceID 残留到下一个任务
                TraceUtil.clearTraceId();
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
        if (Objects.nonNull(runnable)) {
            runnableList.add(runnable);
        }
        if (Objects.nonNull(moreRunnable)) {
            runnableList.addAll(Arrays.asList(moreRunnable));
        }
        return runnableList;
    }
}
