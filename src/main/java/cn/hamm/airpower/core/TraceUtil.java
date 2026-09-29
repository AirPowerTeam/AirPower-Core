package cn.hamm.airpower.core;

import cn.hamm.airpower.core.constant.HttpConstant;
import org.jetbrains.annotations.Nullable;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * <h1>Trace 工具类</h1>
 *
 * @author Hamm.cn
 */
public class TraceUtil {

    /**
     * 重置 TraceID
     */
    public static void resetTraceId() {
        setTraceId(null);
    }

    /**
     * 获取 TraceID
     *
     * @return TraceID
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
     * @param traceId TraceID
     */
    public static void setTraceId(@Nullable String traceId) {
        if (!StringUtil.hasText(traceId)) {
            traceId = UUID.randomUUID().toString();
        }
        MDC.put(HttpConstant.Header.TRACE_ID, traceId);
    }
}
