package cn.hamm.airpower.core;

import cn.hamm.airpower.core.exception.ServiceException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>AccessTokenUtil 单元测试</h1>
 *
 * @author Hamm.cn
 */
class AccessTokenUtilTest {

    private static final String TEST_SECRET = "test-secret-key-1234567890";
    private static final Long TEST_ID = 12345L;

    // ==================== build 方法测试 ====================

    @Test
    void testBuildWithValidPayload() {
        String token = AccessTokenUtil.create()
                .setPayloadId(TEST_ID)
                .build(TEST_SECRET);
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void testBuildWithMultiplePayloads() {
        String token = AccessTokenUtil.create()
                .setPayloadId(TEST_ID)
                .addPayload("username", "testUser")
                .addPayload("role", "admin")
                .build(TEST_SECRET);
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void testBuildWithExpireMillisecond() {
        String token = AccessTokenUtil.create()
                .setPayloadId(TEST_ID)
                .setExpireMillisecond(3600000)
                .build(TEST_SECRET);
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void testBuildWithExpireSecond() {
        String token = AccessTokenUtil.create()
                .setPayloadId(TEST_ID)
                .setExpireSecond(3600)
                .build(TEST_SECRET);
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void testBuildWithEmptySecret() {
        assertThrows(ServiceException.class, () ->
                AccessTokenUtil.create()
                        .setPayloadId(TEST_ID)
                        .build("")
        );
    }

    @Test
    void testBuildWithNullSecret() {
        assertThrows(ServiceException.class, () ->
                AccessTokenUtil.create()
                        .setPayloadId(TEST_ID)
                        .build(null)
        );
    }

    @Test
    void testBuildWithNoPayload() {
        assertThrows(ServiceException.class, () ->
                AccessTokenUtil.create()
                        .build(TEST_SECRET)
        );
    }

    @Test
    void testBuildWithExpireMillisecondZero() {
        assertThrows(ServiceException.class, () ->
                AccessTokenUtil.create()
                        .setPayloadId(TEST_ID)
                        .setExpireMillisecond(0)
        );
    }

    @Test
    void testBuildWithExpireMillisecondNegative() {
        assertThrows(ServiceException.class, () ->
                AccessTokenUtil.create()
                        .setPayloadId(TEST_ID)
                        .setExpireMillisecond(-1)
        );
    }

    @Test
    void testBuildWithExpireSecondZero() {
        assertThrows(ServiceException.class, () ->
                AccessTokenUtil.create()
                        .setPayloadId(TEST_ID)
                        .setExpireSecond(0)
        );
    }

    @Test
    void testBuildWithExpireSecondNegative() {
        assertThrows(ServiceException.class, () ->
                AccessTokenUtil.create()
                        .setPayloadId(TEST_ID)
                        .setExpireSecond(-1)
        );
    }

    // ==================== verify 方法测试 ====================

    @Test
    void testVerifyValidToken() {
        String token = AccessTokenUtil.create()
                .setPayloadId(TEST_ID)
                .build(TEST_SECRET);
        AccessTokenUtil.VerifiedToken verified = AccessTokenUtil.create().verify(token, TEST_SECRET);
        assertNotNull(verified);
        assertEquals(TEST_ID, verified.getPayloadId());
    }

    @Test
    void testVerifyTokenWithMultiplePayloads() {
        String token = AccessTokenUtil.create()
                .setPayloadId(TEST_ID)
                .addPayload("username", "testUser")
                .addPayload("role", "admin")
                .build(TEST_SECRET);
        AccessTokenUtil.VerifiedToken verified = AccessTokenUtil.create().verify(token, TEST_SECRET);
        assertNotNull(verified);
        assertEquals(TEST_ID, verified.getPayloadId());
        assertEquals("testUser", verified.getPayload("username"));
        assertEquals("admin", verified.getPayload("role"));
    }

    @Test
    void testVerifyExpiredToken() {
        String token = AccessTokenUtil.create()
                .setPayloadId(TEST_ID)
                .setExpireMillisecond(1)
                .build(TEST_SECRET);
        // 等待令牌过期
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        assertThrows(ServiceException.class, () ->
                AccessTokenUtil.create().verify(token, TEST_SECRET)
        );
    }

    @Test
    void testVerifyWithWrongSecret() {
        String token = AccessTokenUtil.create()
                .setPayloadId(TEST_ID)
                .build(TEST_SECRET);
        assertThrows(ServiceException.class, () ->
                AccessTokenUtil.create().verify(token, "wrong-secret")
        );
    }

    @Test
    void testVerifyWithEmptySecret() {
        String token = AccessTokenUtil.create()
                .setPayloadId(TEST_ID)
                .build(TEST_SECRET);
        assertThrows(ServiceException.class, () ->
                AccessTokenUtil.create().verify(token, "")
        );
    }

    @Test
    void testVerifyWithNullSecret() {
        String token = AccessTokenUtil.create()
                .setPayloadId(TEST_ID)
                .build(TEST_SECRET);
        assertThrows(ServiceException.class, () ->
                AccessTokenUtil.create().verify(token, null)
        );
    }

    @Test
    void testVerifyWithInvalidToken() {
        assertThrows(ServiceException.class, () ->
                AccessTokenUtil.create().verify("invalid-token", TEST_SECRET)
        );
    }

    @Test
    void testVerifyWithEmptyToken() {
        assertThrows(ServiceException.class, () ->
                AccessTokenUtil.create().verify("", TEST_SECRET)
        );
    }

    @Test
    void testVerifyWithMalformedToken() {
        assertThrows(ServiceException.class, () ->
                AccessTokenUtil.create().verify("abc.def", TEST_SECRET)
        );
    }

    // ==================== addPayload / removePayload 测试 ====================

    @Test
    void testAddAndRemovePayload() {
        String token = AccessTokenUtil.create()
                .setPayloadId(TEST_ID)
                .addPayload("key1", "value1")
                .addPayload("key2", "value2")
                .removePayload("key1")
                .build(TEST_SECRET);
        AccessTokenUtil.VerifiedToken verified = AccessTokenUtil.create().verify(token, TEST_SECRET);
        assertNull(verified.getPayload("key1"));
        assertEquals("value2", verified.getPayload("key2"));
    }

    @Test
    void testRemoveNonExistentPayload() {
        String token = AccessTokenUtil.create()
                .setPayloadId(TEST_ID)
                .removePayload("non-existent")
                .build(TEST_SECRET);
        AccessTokenUtil.VerifiedToken verified = AccessTokenUtil.create().verify(token, TEST_SECRET);
        assertEquals(TEST_ID, verified.getPayloadId());
    }

    // ==================== VerifiedToken 测试 ====================

    @Test
    void testVerifiedTokenGetPayloadId() {
        String token = AccessTokenUtil.create()
                .setPayloadId(TEST_ID)
                .build(TEST_SECRET);
        AccessTokenUtil.VerifiedToken verified = AccessTokenUtil.create().verify(token, TEST_SECRET);
        assertEquals(TEST_ID, verified.getPayloadId());
    }

    @Test
    void testVerifiedTokenGetPayloadIdWithNullId() {
        String token = AccessTokenUtil.create()
                .addPayload("other", "value")
                .build(TEST_SECRET);
        AccessTokenUtil.VerifiedToken verified = AccessTokenUtil.create().verify(token, TEST_SECRET);
        assertThrows(ServiceException.class, verified::getPayloadId);
    }

    @Test
    void testVerifiedTokenGetPayloadWithNullKey() {
        String token = AccessTokenUtil.create()
                .setPayloadId(TEST_ID)
                .build(TEST_SECRET);
        AccessTokenUtil.VerifiedToken verified = AccessTokenUtil.create().verify(token, TEST_SECRET);
        assertNull(verified.getPayload("non-existent"));
    }

    @Test
    void testVerifiedTokenPayloadsNotNull() {
        AccessTokenUtil.VerifiedToken token = new AccessTokenUtil.VerifiedToken();
        assertNotNull(token.getPayloads());
    }

    @Test
    void testVerifiedTokenExpireTimestampsDefault() {
        AccessTokenUtil.VerifiedToken token = new AccessTokenUtil.VerifiedToken();
        assertEquals(0, token.getExpireTimestamps());
    }

    @Test
    void testVerifiedTokenSetExpireTimestamps() {
        AccessTokenUtil.VerifiedToken token = new AccessTokenUtil.VerifiedToken();
        token.setExpireTimestamps(1234567890L);
        assertEquals(1234567890L, token.getExpireTimestamps());
    }

    // ==================== 边界情况测试 ====================

    @Test
    void testBuildWithLongSecret() {
        String longSecret = "a".repeat(1000);
        String token = AccessTokenUtil.create()
                .setPayloadId(TEST_ID)
                .build(longSecret);
        assertNotNull(token);
        AccessTokenUtil.VerifiedToken verified = AccessTokenUtil.create().verify(token, longSecret);
        assertEquals(TEST_ID, verified.getPayloadId());
    }

    @Test
    void testBuildWithSpecialCharactersInPayload() {
        String token = AccessTokenUtil.create()
                .setPayloadId(TEST_ID)
                .addPayload("special", "!@#$%^&*()_+{}|:<>?[]")
                .build(TEST_SECRET);
        AccessTokenUtil.VerifiedToken verified = AccessTokenUtil.create().verify(token, TEST_SECRET);
        assertEquals("!@#$%^&*()_+{}|:<>?[]", verified.getPayload("special"));
    }

    @Test
    void testBuildWithUnicodePayload() {
        String token = AccessTokenUtil.create()
                .setPayloadId(TEST_ID)
                .addPayload("unicode", "中文测试🎉")
                .build(TEST_SECRET);
        AccessTokenUtil.VerifiedToken verified = AccessTokenUtil.create().verify(token, TEST_SECRET);
        assertEquals("中文测试🎉", verified.getPayload("unicode"));
    }

    @Test
    void testBuildWithNumberPayload() {
        String token = AccessTokenUtil.create()
                .setPayloadId(TEST_ID)
                .addPayload("number", 42)
                .build(TEST_SECRET);
        AccessTokenUtil.VerifiedToken verified = AccessTokenUtil.create().verify(token, TEST_SECRET);
        assertEquals(42, verified.getPayload("number"));
    }

    @Test
    void testBuildWithBooleanPayload() {
        String token = AccessTokenUtil.create()
                .setPayloadId(TEST_ID)
                .addPayload("flag", true)
                .build(TEST_SECRET);
        AccessTokenUtil.VerifiedToken verified = AccessTokenUtil.create().verify(token, TEST_SECRET);
        assertEquals(true, verified.getPayload("flag"));
    }

    @Test
    void testBuildWithNullPayloadValue() {
        String token = AccessTokenUtil.create()
                .setPayloadId(TEST_ID)
                .addPayload("nullKey", null)
                .build(TEST_SECRET);
        AccessTokenUtil.VerifiedToken verified = AccessTokenUtil.create().verify(token, TEST_SECRET);
        assertNull(verified.getPayload("nullKey"));
    }

    @Test
    void testBuildWithEmptyPayloadValue() {
        String token = AccessTokenUtil.create()
                .setPayloadId(TEST_ID)
                .addPayload("empty", "")
                .build(TEST_SECRET);
        AccessTokenUtil.VerifiedToken verified = AccessTokenUtil.create().verify(token, TEST_SECRET);
        assertEquals("", verified.getPayload("empty"));
    }

    @Test
    void testVerifyWithTamperedToken() {
        String token = AccessTokenUtil.create()
                .setPayloadId(TEST_ID)
                .build(TEST_SECRET);
        // 篡改token
        String tamperedToken = token + "tampered";
        assertThrows(ServiceException.class, () ->
                AccessTokenUtil.create().verify(tamperedToken, TEST_SECRET)
        );
    }

    @Test
    void testBuildAndVerifyWithNoExpiration() {
        String token = AccessTokenUtil.create()
                .setPayloadId(TEST_ID)
                .build(TEST_SECRET);
        AccessTokenUtil.VerifiedToken verified = AccessTokenUtil.create().verify(token, TEST_SECRET);
        assertEquals(TEST_ID, verified.getPayloadId());
        assertEquals(0, verified.getExpireTimestamps());
    }

    @Test
    void testChainingMethods() {
        AccessTokenUtil util = AccessTokenUtil.create();
        AccessTokenUtil result = util.setPayloadId(TEST_ID)
                .addPayload("key", "value")
                .removePayload("key")
                .setExpireMillisecond(3600000)
                .setExpireSecond(1);
        assertNotNull(result);
        String token = result.build(TEST_SECRET);
        assertNotNull(token);
    }
}
