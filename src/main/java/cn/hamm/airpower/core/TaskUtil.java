package cn.hamm.airpower.core;

import cn.hamm.airpower.core.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
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
     * 队列容量
     */
    private static final int QUEUE_CAPACITY = 1000;

    /**
     * 共享线程池
     *
     * @apiNote 队列满时<b>立即拒绝</b>而不是回退到调用方线程执行。
     * 回退执行（{@code CallerRunsPolicy}）会把调用方的 Web 线程一起占住：
     * 队列满 → 请求线程陪着执行 → 导出更慢 → 队列更难排空 → 更多请求进入回退，
     * 这个正反馈会让 Tomcat 工作线程被逐个耗尽，最终全站接口一起卡住。
     * 宁可让调用方收到「系统繁忙」，也不要拖垮整个 Web 层。
     * 需要「绝不丢任务」的场景请自行持有队列，不要依赖本类
     */
    private static final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(
            CORE_POOL_SIZE,
            MAX_POOL_SIZE,
            60L,
            SECONDS,
            new LinkedBlockingQueue<>(QUEUE_CAPACITY),
            new ThreadFactory() {
                private final AtomicInteger counter = new AtomicInteger(0);

                @Override
                public Thread newThread(@NotNull Runnable r) {
                    Thread thread = new Thread(r, "airpower-task-" + counter.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                }
            },
            new ThreadPoolExecutor.AbortPolicy()
    );

    /**
     * 异步执行任务
     *
     * @param runnable     任务
     * @param moreRunnable 更多任务
     * @apiNote 任务<b>内部</b>的异常只记录日志、不向调用方抛出；
     * 但线程池与队列都满时任务<b>根本不会执行</b>，此时抛
     * {@link ServiceException} 让调用方明确感知「系统繁忙」，
     * 而不是让它以为任务已提交
     */
    public static void run(Runnable runnable, Runnable... moreRunnable) {
        String traceId = TraceUtil.getTraceId();
        for (Runnable run : getRunnableList(runnable, moreRunnable)) {
            try {
                EXECUTOR.execute(() -> {
                    try {
                        TraceUtil.setTraceId(traceId);
                        run.run();
                    } catch (Throwable e) {
                        // 必须传异常对象本身，只打印 getMessage() 会丢掉堆栈，线上问题无从定位
                        log.error("异步执行任务失败", e);
                    } finally {
                        // 池线程会被复用，清理 MDC 避免 TraceID 残留到下一个任务
                        TraceUtil.clearTraceId();
                    }
                });
            } catch (RejectedExecutionException e) {
                // 队列已满。不回退到调用方线程执行，否则会占住 Web 请求线程
                log.warn("异步任务队列已满（容量 {}），任务被拒绝", QUEUE_CAPACITY);
                throw new ServiceException("系统繁忙，请稍后重试");
            }
        }
    }

    /**
     * 线程池是否空闲（无运行中任务且队列为空）
     *
     * @return 是否空闲
     * @apiNote 供测试判定饱和与排空用。仅凭「提交一次成功」判断排空不可靠：
     * 那一刻队列里可能还压着任务
     */
    static boolean isIdle() {
        return EXECUTOR.getActiveCount() == 0 && EXECUTOR.getQueue().isEmpty();
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
