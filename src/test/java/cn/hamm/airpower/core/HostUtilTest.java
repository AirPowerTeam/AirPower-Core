package cn.hamm.airpower.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>HostUtil 单元测试</h1>
 *
 * @author Hamm.cn
 */
class HostUtilTest {

    // ==================== getHostName 方法测试 ====================

    @Test
    void testGetHostName() {
        String result = HostUtil.getHostName();
        // 主机名可能为null（如果所有方法都失败）
        // 但至少应该能获取到，或者返回null而不抛出异常
        if (result != null) {
            assertFalse(result.trim().isEmpty(), "主机名不应为空字符串");
        }
    }

    @Test
    void testGetHostNameDoesNotThrowException() {
        // 确保方法不会抛出异常
        assertDoesNotThrow(HostUtil::getHostName);
    }

    @Test
    void testGetHostNameReturnsConsistentResult() {
        // 多次调用应该返回相同的结果
        String result1 = HostUtil.getHostName();
        String result2 = HostUtil.getHostName();
        assertEquals(result1, result2, "多次调用应返回相同的主机名");
    }

    @Test
    void testGetHostNameNotEmptyWhenAvailable() {
        String result = HostUtil.getHostName();
        // 如果能获取到主机名，应该包含有效字符
        if (result != null) {
            assertFalse(result.isEmpty(), "主机名应有长度");
        }
    }
}
