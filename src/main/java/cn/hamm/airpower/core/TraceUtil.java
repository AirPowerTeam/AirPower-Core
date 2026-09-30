package cn.hamm.airpower.core;

import cn.hamm.airpower.core.constant.HttpConstant;
import org.jetbrains.annotations.Nullable;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * <h1>链路追踪工具类</h1>
 *
 * @author Hamm.cn
 * @apiNote TraceID 存放在 {@link MDC} 中，与 {@code HttpConstant.Header.TRACE_ID} 同名，
 * 保证日志与请求头里的追踪 ID 一致
 */
public class TraceUtil {

    /**
     * 重新生成 TraceID
     *
     * @apiNote {@code @Scheduled} 等非请求入口没有上游 TraceID，任务开始前调用本方法
     * 可避免沿用线程池里残留的上一个请求的 TraceID，导致日志串号
     */
    public static void resetTraceId() {
        setTraceId(null);
    }

    /**
     * 获取 TraceID
     *
     * @return 当前线程的 TraceID，未设置时为 {@code null}
     */
    public static String getTraceId() {
        return MDC.get(HttpConstant.Header.TRACE_ID);
    }

    /**
     * 清除 TraceID
     *
     * @apiNote 线程池等会复用线程的场景，任务结束后应调用，避免 TraceID 残留
     */
    public static void clearTraceId() {
        MDC.remove(HttpConstant.Header.TRACE_ID);
    }

    /**
     * 设置 TraceID
     *
     * @param traceId TraceID，为空时自动生成 UUID
     */
    public static void setTraceId(@Nullable String traceId) {
        if (!StringUtil.hasText(traceId)) {
            traceId = UUID.randomUUID().toString();
        }
        MDC.put(HttpConstant.Header.TRACE_ID, traceId);
    }
}
