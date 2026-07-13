package cn.hamm.airpower.core;

import cn.hamm.airpower.core.constant.HttpConstant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>TraceUtil 单元测试</h1>
 *
 * @author Hamm.cn
 */
class TraceUtilTest {

    @BeforeEach
    void setUp() {
        // 每个测试前清除 MDC
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        // 每个测试后清除 MDC
        MDC.clear();
    }

    // ==================== getTraceId 方法测试 ====================

    @Test
    void testGetTraceIdWithNoTraceIdSet() {
        // 没有设置 TraceID 时，应该返回 null
        assertNull(TraceUtil.getTraceId());
    }

    @Test
    void testGetTraceIdAfterSet() {
        String traceId = "test-trace-id-123";
        TraceUtil.setTraceId(traceId);
        assertEquals(traceId, TraceUtil.getTraceId());
    }

    @Test
    void testGetTraceIdAfterReset() {
        // 先设置一个 TraceID
        TraceUtil.setTraceId("original-trace-id");
        // 重置
        TraceUtil.resetTraceId();
        // 重置后应该有一个新的 UUID 格式的 TraceID
        String traceId = TraceUtil.getTraceId();
        assertNotNull(traceId);
        assertFalse(traceId.isEmpty());
    }

    // ==================== setTraceId 方法测试 ====================

    @Test
    void testSetTraceIdWithValidValue() {
        String traceId = "my-trace-id";
        TraceUtil.setTraceId(traceId);
        assertEquals(traceId, TraceUtil.getTraceId());
    }

    @Test
    void testSetTraceIdWithEmptyString() {
        // 传入空字符串，应该生成一个 UUID
        TraceUtil.setTraceId("");
        String traceId = TraceUtil.getTraceId();
        assertNotNull(traceId);
        assertFalse(traceId.isEmpty());
        // 验证是 UUID 格式
        assertDoesNotThrow(() -> UUID.fromString(traceId));
    }

    @Test
    void testSetTraceIdWithNull() {
        // 传入 null，应该生成一个 UUID
        TraceUtil.setTraceId(null);
        String traceId = TraceUtil.getTraceId();
        assertNotNull(traceId);
        assertFalse(traceId.isEmpty());
        // 验证是 UUID 格式
        assertDoesNotThrow(() -> UUID.fromString(traceId));
    }

    @Test
    void testSetTraceIdWithWhitespaceOnly() {
        // 传入只有空格的字符串，应该生成一个 UUID
        TraceUtil.setTraceId("   ");
        String traceId = TraceUtil.getTraceId();
        assertNotNull(traceId);
        assertFalse(traceId.isEmpty());
        // 验证是 UUID 格式
        assertDoesNotThrow(() -> UUID.fromString(traceId));
    }

    @Test
    void testSetTraceIdOverwritesPreviousValue() {
        // 先设置一个值
        TraceUtil.setTraceId("first-trace-id");
        assertEquals("first-trace-id", TraceUtil.getTraceId());

        // 再设置一个新值，应该覆盖旧的
        TraceUtil.setTraceId("second-trace-id");
        assertEquals("second-trace-id", TraceUtil.getTraceId());
    }

    // ==================== resetTraceId 方法测试 ====================

    @Test
    void testResetTraceIdWhenNoTraceIdSet() {
        // 没有设置 TraceID 时重置
        TraceUtil.resetTraceId();
        String traceId = TraceUtil.getTraceId();
        assertNotNull(traceId);
        assertFalse(traceId.isEmpty());
    }

    @Test
    void testResetTraceIdWhenTraceIdSet() {
        // 先设置一个值
        TraceUtil.setTraceId("old-trace-id");
        assertEquals("old-trace-id", TraceUtil.getTraceId());

        // 重置
        TraceUtil.resetTraceId();
        String traceId = TraceUtil.getTraceId();
        assertNotNull(traceId);
        assertFalse(traceId.isEmpty());
        // 重置后应该不是原来的值了
        assertNotEquals("old-trace-id", traceId);
    }

    @Test
    void testResetTraceIdGeneratesUuid() {
        TraceUtil.resetTraceId();
        String traceId = TraceUtil.getTraceId();
        // 验证是有效的 UUID
        assertDoesNotThrow(() -> UUID.fromString(traceId));
    }

    // ==================== MDC 集成测试 ====================

    @Test
    void testTraceIdStoredInMdc() {
        String traceId = "test-mdc-trace-id";
        TraceUtil.setTraceId(traceId);

        // 直接通过 MDC 获取应该能拿到相同的值
        String mdcValue = MDC.get(HttpConstant.Header.TRACE_ID);
        assertEquals(traceId, mdcValue);
    }

    @Test
    void testMultipleSetAndGetOperations() {
        // 多次设置和获取
        for (int i = 0; i < 10; i++) {
            String traceId = "trace-id-" + i;
            TraceUtil.setTraceId(traceId);
            assertEquals(traceId, TraceUtil.getTraceId());
        }
    }

    // ==================== 边界情况测试 ====================

    @Test
    void testSetTraceIdWithLongValue() {
        String longTraceId = "a".repeat(1000);
        TraceUtil.setTraceId(longTraceId);
        assertEquals(longTraceId, TraceUtil.getTraceId());
    }

    @Test
    void testSetTraceIdWithSpecialCharacters() {
        String specialTraceId = "trace-id_123.test+value=special";
        TraceUtil.setTraceId(specialTraceId);
        assertEquals(specialTraceId, TraceUtil.getTraceId());
    }

    @Test
    void testSetTraceIdWithUnicode() {
        String unicodeTraceId = "跟踪ID-测试-123";
        TraceUtil.setTraceId(unicodeTraceId);
        assertEquals(unicodeTraceId, TraceUtil.getTraceId());
    }

    @Test
    void testConsecutiveResetsGenerateDifferentIds() {
        TraceUtil.resetTraceId();
        String firstId = TraceUtil.getTraceId();

        TraceUtil.resetTraceId();
        String secondId = TraceUtil.getTraceId();

        // 两次重置应该生成不同的 ID
        assertNotEquals(firstId, secondId);
    }

    @Test
    void testSetTraceIdWithSingleCharacter() {
        String traceId = "x";
        TraceUtil.setTraceId(traceId);
        assertEquals(traceId, TraceUtil.getTraceId());
    }
}
