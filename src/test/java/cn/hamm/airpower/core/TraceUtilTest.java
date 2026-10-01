package cn.hamm.airpower.core;

import cn.hamm.airpower.core.constant.HttpConstant;
import org.junit.jupiter.api.*;
import org.slf4j.MDC;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>TraceUtil 单元测试</h1>
 *
 * <p>MDC 为线程本地存储，因此所有读写断言均在同一线程内完成，异步线程场景单独通过 CountDownLatch 等待结果。</p>
 *
 * @author Hamm.cn
 */
@DisplayName("TraceUtil 链路追踪工具类单元测试")
class TraceUtilTest {

    /**
     * 等待异步线程的超时时间（秒）
     */
    private static final long AWAIT_SECONDS = 5L;

    @BeforeEach
    @DisplayName("用例执行前清空 MDC，避免用例间互相污染")
    void clearMdcBefore() {
        MDC.clear();
    }

    @AfterEach
    @DisplayName("用例执行后清理 MDC")
    void clearMdcAfter() {
        MDC.clear();
    }

    @Nested
    @DisplayName("setTraceId 设置 TraceID")
    class SetTraceIdTest {

        @Test
        @DisplayName("正常路径：设置指定值后可原样读取")
        void testSetSpecifiedTraceId() {
            TraceUtil.setTraceId("abc");
            assertEquals("abc", TraceUtil.getTraceId(), "应原样返回写入的 TraceID");
        }

        @Test
        @DisplayName("正常路径：重复设置时后写值覆盖先写值")
        void testSetOverridePreviousValue() {
            TraceUtil.setTraceId("first");
            TraceUtil.setTraceId("second");
            assertEquals("second", TraceUtil.getTraceId(), "重复设置应以后一次为准");
        }

        @Test
        @DisplayName("空值分支：传 null 时生成新的 UUID")
        void testSetNullGeneratesUuid() {
            TraceUtil.setTraceId(null);
            String traceId = TraceUtil.getTraceId();
            assertNotNull(traceId, "传 null 时应生成 UUID 而不是置空");
            assertDoesNotThrow(() -> UUID.fromString(traceId), "生成的 TraceID 应为合法 UUID：" + traceId);
        }

        @Test
        @DisplayName("空串分支：传空串时生成新的 UUID")
        void testSetEmptyStringGeneratesUuid() {
            TraceUtil.setTraceId("");
            String traceId = TraceUtil.getTraceId();
            assertNotNull(traceId, "传空串时按 hasText 判定应生成 UUID");
            assertDoesNotThrow(() -> UUID.fromString(traceId), "生成的 TraceID 应为合法 UUID：" + traceId);
        }

        @Test
        @DisplayName("空白分支：传纯空白时生成新的 UUID")
        void testSetBlankStringGeneratesUuid() {
            TraceUtil.setTraceId("   \t ");
            String traceId = TraceUtil.getTraceId();
            assertNotNull(traceId, "传纯空白时按 hasText 判定应生成 UUID");
            assertDoesNotThrow(() -> UUID.fromString(traceId), "生成的 TraceID 应为合法 UUID：" + traceId);
        }

        @Test
        @DisplayName("覆盖分支：null 入参会覆盖已有的显式值")
        void testNullOverridesSpecifiedValue() {
            TraceUtil.setTraceId("explicit");
            TraceUtil.setTraceId(null);
            String traceId = TraceUtil.getTraceId();
            assertNotEquals("explicit", traceId, "传 null 后原有显式值应被新生成的 UUID 覆盖");
        }
    }

    @Nested
    @DisplayName("getTraceId 读取 TraceID")
    class GetTraceIdTest {

        @Test
        @DisplayName("边界值：MDC 未设置时返回 null")
        void testGetWhenNotSet() {
            assertNull(TraceUtil.getTraceId(), "MDC 中没有该键时应返回 null");
        }

        @Test
        @DisplayName("正常路径：读取的键为 HttpConstant.Header.TRACE_ID")
        void testGetFromMdcWithHeaderKey() {
            TraceUtil.setTraceId("key-check");
            assertEquals("X-Trace-ID", HttpConstant.Header.TRACE_ID, "MDC 使用的键名应为 X-Trace-ID");
            assertEquals("key-check", MDC.get(HttpConstant.Header.TRACE_ID), "MDC 中应存放刚写入的 TraceID");
        }

        @Test
        @DisplayName("线程隔离：其他线程读不到本线程的 TraceID")
        void testTraceIdIsThreadLocal() throws InterruptedException {
            TraceUtil.setTraceId("main-thread-trace");
            AtomicReference<String> otherThreadTraceId = new AtomicReference<>("not-null");
            CountDownLatch latch = new CountDownLatch(1);
            Thread thread = new Thread(() -> {
                otherThreadTraceId.set(TraceUtil.getTraceId());
                latch.countDown();
            }, "trace-util-test-other");
            thread.setDaemon(true);
            thread.start();
            assertTrue(latch.await(AWAIT_SECONDS, TimeUnit.SECONDS), "等待其他线程读取 TraceID 超时");
            assertNull(otherThreadTraceId.get(), "MDC 是线程本地的，其他线程不应读到本线程的 TraceID");
            assertEquals("main-thread-trace", TraceUtil.getTraceId(), "本线程的 TraceID 不应被其他线程影响");
        }
    }

    @Nested
    @DisplayName("resetTraceId 重置 TraceID")
    class ResetTraceIdTest {

        @Test
        @DisplayName("重置后仍为非空 UUID：内部等价于 setTraceId(null)")
        void testResetGeneratesUuid() {
            TraceUtil.setTraceId("before-reset");
            TraceUtil.resetTraceId();
            String traceId = TraceUtil.getTraceId();
            assertNotNull(traceId, "resetTraceId 内部调用 setTraceId(null)，应生成 UUID 而不是清空");
            assertDoesNotThrow(() -> UUID.fromString(traceId), "重置后生成的 TraceID 应为合法 UUID：" + traceId);
        }

        @Test
        @DisplayName("重置覆盖原值：与重置前的值不相等")
        void testResetOverridesPreviousValue() {
            TraceUtil.setTraceId("before-reset");
            TraceUtil.resetTraceId();
            assertNotEquals("before-reset", TraceUtil.getTraceId(), "重置后应生成新值覆盖原值");
        }

        @Test
        @DisplayName("连续两次重置：每次都会得到不同的新值")
        void testResetTwiceGeneratesDifferentIds() {
            TraceUtil.resetTraceId();
            String first = TraceUtil.getTraceId();
            TraceUtil.resetTraceId();
            String second = TraceUtil.getTraceId();
            assertNotEquals(first, second, "每次 resetTraceId 都应重新生成 UUID，两次结果不应相同");
        }
    }

    @Nested
    @DisplayName("clearTraceId 清除 TraceID")
    class ClearTraceIdTest {

        @Test
        @DisplayName("正常路径：清除后读取为 null")
        void testClearRemovesValue() {
            TraceUtil.setTraceId("to-be-cleared");
            TraceUtil.clearTraceId();
            assertNull(TraceUtil.getTraceId(), "清除后 MDC 中不应再保留 TraceID");
        }

        @Test
        @DisplayName("未设置时重复清除不抛异常")
        void testClearWhenNotSet() {
            assertDoesNotThrow(TraceUtil::clearTraceId, "未设置 TraceID 时清除应安全无副作用");
            assertNull(TraceUtil.getTraceId(), "清除后仍应为 null");
        }

        @Test
        @DisplayName("只清除 TraceID，不影响 MDC 中的其他键")
        void testClearOnlyRemovesTraceId() {
            MDC.put("其他键", "其他值");
            TraceUtil.setTraceId("only-trace");
            TraceUtil.clearTraceId();
            assertNull(TraceUtil.getTraceId(), "TraceID 应被清除");
            assertEquals("其他值", MDC.get("其他键"), "其他 MDC 键不应受影响");
        }

        @Test
        @DisplayName("与 resetTraceId 的区别：reset 生成新值，clear 才是真正清空")
        void testDifferenceWithReset() {
            TraceUtil.resetTraceId();
            assertNotNull(TraceUtil.getTraceId(), "resetTraceId 会重新生成 UUID");
            TraceUtil.clearTraceId();
            assertNull(TraceUtil.getTraceId(), "clearTraceId 才是真正清空 MDC");
        }
    }
}
