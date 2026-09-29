package cn.hamm.airpower.core;

import cn.hamm.airpower.core.constant.Constant;
import cn.hamm.airpower.core.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>AccessToken 身份令牌工具类测试</h1>
 * <p>
 * 覆盖：链式配置、负载增删、过期时间设置、令牌生成与校验、
 * 各类异常分支，以及 {@link AccessTokenUtil.VerifiedToken} 的取值方法。
 * </p>
 *
 * @author Hamm
 */
@DisplayName("AccessTokenUtil 身份令牌工具类测试")
class AccessTokenUtilTest {

    /**
     * 测试用密钥，优先从环境变量 {@code airpower.accessTokenSecret} 读取
     */
    private static final String SECRET = getSecret();

    /**
     * 无效令牌的统一错误信息
     */
    private static final String INVALID_MESSAGE = "身份令牌无效，请重新获取身份令牌";

    /**
     * 未配置密钥的错误信息
     */
    private static final String NO_SECRET_MESSAGE = "请在环境变量配置 airpower.accessTokenSecret";

    /**
     * 待测实例，每个用例独立创建
     */
    private AccessTokenUtil accessTokenUtil;

    /**
     * 读取环境变量中的密钥，未配置时使用测试默认值
     *
     * @return 密钥
     */
    private static String getSecret() {
        String fromEnv = System.getenv("airpower.accessTokenSecret");
        return fromEnv == null || fromEnv.isBlank() ? "airpower-test-secret-for-unit-test" : fromEnv;
    }

    /**
     * 计算 HMAC-SHA256 的十六进制签名（与源码实现保持一致）
     *
     * @param secret  密钥
     * @param content 内容
     * @return 十六进制签名
     */
    private static String hmacSha256(String secret, String content) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(UTF_8), "HmacSHA256"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : mac.doFinal(content.getBytes(UTF_8))) {
                hexString.append(String.format("%02x", b & 0xff));
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new IllegalStateException("计算 HMAC-SHA256 失败", e);
        }
    }

    /**
     * 手工组装一个结构合法且签名正确的令牌（用于构造边界场景）
     *
     * @param secret      密钥
     * @param expirePart  过期时间戳字符串（可传非数字以测试异常分支）
     * @param payloadJson 负载 JSON
     * @return 令牌
     */
    private static String buildRawToken(String secret, String expirePart, String payloadJson) {
        String payloadBase = Base64.getUrlEncoder().encodeToString(payloadJson.getBytes(UTF_8));
        String content = expirePart + "." + hmacSha256(secret, expirePart + "." + payloadBase) + "." + payloadBase;
        return Base64.getUrlEncoder().encodeToString(content.getBytes(UTF_8));
    }

    /**
     * 每个用例前创建一个干净的实例
     */
    @BeforeEach
    void setUp() {
        accessTokenUtil = AccessTokenUtil.create();
    }

    /**
     * 生成一个可用的令牌
     *
     * @return 令牌
     */
    private String buildNormalToken() {
        return accessTokenUtil.setPayloadId(1001L)
                .addPayload("name", "Hamm")
                .setExpireSecond(60)
                .build(SECRET);
    }

    @Nested
    @DisplayName("1. 实例创建与链式配置")
    class CreateAndChain {

        @Test
        @DisplayName("create() 每次返回全新的实例")
        void testCreateReturnsNewInstance() {
            assertNotSame(AccessTokenUtil.create(), AccessTokenUtil.create(), "create() 每次都应返回一个新的实例");
        }

        @Test
        @DisplayName("addPayload / removePayload 返回 this")
        void testChainReturnSelf() {
            AccessTokenUtil instance = AccessTokenUtil.create();
            assertSame(instance, instance.addPayload("k", "v"), "addPayload 应返回 this");
            assertSame(instance, instance.removePayload("k"), "removePayload 应返回 this");
        }

        @Test
        @DisplayName("setPayloadId / setExpireMillisecond / setExpireSecond 返回 this")
        void testChainReturnSelfOfSetters() {
            AccessTokenUtil instance = AccessTokenUtil.create();
            assertSame(instance, instance.setPayloadId(1L), "setPayloadId 应返回 this");
            assertSame(instance, instance.setExpireMillisecond(1000), "setExpireMillisecond 应返回 this");
            assertSame(instance, instance.setExpireSecond(1), "setExpireSecond 应返回 this");
        }

        @Test
        @DisplayName("removePayload 移除不存在的 Key 不会报错")
        void testRemoveUnknownPayload() {
            String token = accessTokenUtil.setPayloadId(2L)
                    .removePayload("不存在的键")
                    .setExpireSecond(30)
                    .build(SECRET);
            assertNotNull(token, "移除不存在的负载后仍应能正常生成令牌");
        }

        @Test
        @DisplayName("addPayload 同名 Key 后写覆盖前写")
        void testAddPayloadOverride() {
            String token = accessTokenUtil.addPayload("name", "第一次")
                    .addPayload("name", "第二次")
                    .setExpireSecond(30)
                    .build(SECRET);
            AccessTokenUtil.VerifiedToken verified = AccessTokenUtil.create().verify(token, SECRET);
            assertEquals("第二次", verified.getPayload("name"), "同名负载应以后写入的值为准");
        }
    }

    @Nested
    @DisplayName("2. 过期时间设置")
    class Expire {

        @Test
        @DisplayName("setExpireMillisecond(0) 抛出 ServiceException")
        void testZeroMillisecond() {
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> accessTokenUtil.setExpireMillisecond(0),
                    "过期毫秒数为 0 时应抛出 ServiceException");
            assertEquals("过期毫秒数必须大于0", exception.getMessage(), "异常信息应为：过期毫秒数必须大于0");
        }

        @Test
        @DisplayName("setExpireMillisecond(负数) 抛出 ServiceException")
        void testNegativeMillisecond() {
            assertThrows(ServiceException.class, () -> accessTokenUtil.setExpireMillisecond(-1),
                    "过期毫秒数为负数时应抛出 ServiceException");
        }

        @Test
        @DisplayName("setExpireSecond(0) 抛出 ServiceException")
        void testZeroSecond() {
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> accessTokenUtil.setExpireSecond(0),
                    "过期秒数为 0 时应抛出 ServiceException");
            assertEquals("过期秒数必须大于0", exception.getMessage(), "异常信息应为：过期秒数必须大于0");
        }

        @Test
        @DisplayName("setExpireSecond(负数) 抛出 ServiceException")
        void testNegativeSecond() {
            assertThrows(ServiceException.class, () -> accessTokenUtil.setExpireSecond(-60),
                    "过期秒数为负数时应抛出 ServiceException");
        }

        @Test
        @DisplayName("setExpireMillisecond 生效，时间戳落在设定区间内")
        void testMillisecondApplied() {
            long before = System.currentTimeMillis();
            String token = accessTokenUtil.setPayloadId(3L).setExpireMillisecond(60_000).build(SECRET);
            long after = System.currentTimeMillis();
            long expire = AccessTokenUtil.create().verify(token, SECRET).getExpireTimestamps();
            assertAll("过期时间戳应等于当前时间 + 60000",
                    () -> assertTrue(expire >= before + 60_000, "过期时间戳不应早于设定值"),
                    () -> assertTrue(expire <= after + 60_000, "过期时间戳不应晚于设定值"));
        }

        @Test
        @DisplayName("setExpireSecond 生效，内部按 1000 毫秒换算")
        void testSecondApplied() {
            long before = System.currentTimeMillis();
            String token = accessTokenUtil.setPayloadId(3L).setExpireSecond(60).build(SECRET);
            long after = System.currentTimeMillis();
            long expire = AccessTokenUtil.create().verify(token, SECRET).getExpireTimestamps();
            assertAll("1 秒应换算为 1000 毫秒",
                    () -> assertTrue(expire >= before + 60_000, "过期时间戳不应早于设定值"),
                    () -> assertTrue(expire <= after + 60_000, "过期时间戳不应晚于设定值"));
        }

        @Test
        @DisplayName("后设置的过期时间覆盖先设置的值")
        void testExpireOverride() {
            long before = System.currentTimeMillis();
            String token = accessTokenUtil.setPayloadId(3L)
                    .setExpireSecond(60)
                    .setExpireMillisecond(5_000)
                    .build(SECRET);
            long expire = AccessTokenUtil.create().verify(token, SECRET).getExpireTimestamps();
            assertTrue(expire <= before + 5_000, "后设置的过期时间应覆盖先前的值");
        }
    }

    @Nested
    @DisplayName("3. build 的参数校验")
    class BuildValidation {

        @Test
        @DisplayName("secret 为 null 时抛出 401 ServiceException")
        void testNullSecret() {
            accessTokenUtil.setPayloadId(1L).setExpireSecond(60);
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> accessTokenUtil.build(null),
                    "secret 为 null 时应抛出 ServiceException");
            assertAll("异常应为 401 且带明确提示",
                    () -> assertEquals(Json.UNAUTHORIZED_CODE, exception.getCode(), "错误码应为 401"),
                    () -> assertEquals("身份令牌创建失败，" + NO_SECRET_MESSAGE, exception.getMessage(),
                            "异常信息应提示配置环境变量"));
        }

        @Test
        @DisplayName("secret 为空串或空白串时抛出 401 ServiceException")
        void testBlankSecret() {
            accessTokenUtil.setPayloadId(1L).setExpireSecond(60);
            for (String blank : new String[]{"", " ", "   \t\n"}) {
                ServiceException exception = assertThrows(ServiceException.class,
                        () -> accessTokenUtil.build(blank),
                        "secret 为空白串 [" + blank + "] 时应抛出 ServiceException");
                assertEquals(Json.UNAUTHORIZED_CODE, exception.getCode(), "错误码应为 401");
            }
        }

        @Test
        @DisplayName("没有任何负载时抛出 ServiceException：没有任何负载数据")
        void testEmptyPayloads() {
            accessTokenUtil.setExpireSecond(60);
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> accessTokenUtil.build(SECRET),
                    "没有负载数据时应抛出 ServiceException");
            assertEquals("没有任何负载数据", exception.getMessage(), "异常信息应为：没有任何负载数据");
        }

        @Test
        @DisplayName("移除全部负载后再生成应抛出：没有任何负载数据")
        void testRemoveAllPayloads() {
            accessTokenUtil.setPayloadId(1L).addPayload("k", "v")
                    .removePayload(Constant.ID)
                    .removePayload("k")
                    .setExpireSecond(60);
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> accessTokenUtil.build(SECRET),
                    "移除全部负载后应抛出 ServiceException");
            assertEquals("没有任何负载数据", exception.getMessage(), "异常信息应为：没有任何负载数据");
        }

        @Test
        @DisplayName("未设置过期时间时抛出 ServiceException：令牌必须设置过期时间")
        void testNoExpire() {
            accessTokenUtil.setPayloadId(1L);
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> accessTokenUtil.build(SECRET),
                    "未设置过期时间时应抛出 ServiceException");
            assertEquals("令牌必须设置过期时间", exception.getMessage(), "异常信息应为：令牌必须设置过期时间");
        }

        @Test
        @DisplayName("校验顺序：secret 优先于负载与过期时间")
        void testValidationOrder() {
            // 既没有负载也没有过期时间，应优先报 secret 的错误
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> accessTokenUtil.build(null),
                    "多个错误同时存在时应优先校验 secret");
            assertTrue(exception.getMessage().startsWith("身份令牌创建失败，"),
                    "应优先提示密钥配置问题，实际为：" + exception.getMessage());
        }
    }

    @Nested
    @DisplayName("4. 令牌生成与校验的正常往返")
    class BuildAndVerify {

        @Test
        @DisplayName("build 后 verify 的负载与 ID 完全一致")
        void testNormalRoundTrip() {
            String token = accessTokenUtil.setPayloadId(1001L)
                    .addPayload("name", "Hamm")
                    .addPayload("admin", Boolean.TRUE)
                    .setExpireSecond(60)
                    .build(SECRET);
            AccessTokenUtil.VerifiedToken verified = AccessTokenUtil.create().verify(token, SECRET);
            assertAll("校验后的负载应与写入时一致",
                    () -> assertEquals(1001, verified.getPayload(Constant.ID), "负载 ID 应一致"),
                    () -> assertEquals("Hamm", verified.getPayload("name"), "字符串负载应一致"),
                    () -> assertEquals(Boolean.TRUE, verified.getPayload("admin"), "布尔负载应一致"),
                    () -> assertEquals(1001L, verified.getPayloadId(), "getPayloadId 应返回正确的 ID"));
        }

        @Test
        @DisplayName("令牌为 3 段 Base64 结构，可被正确解析")
        void testTokenStructure() {
            String token = buildNormalToken();
            assertFalse(token.isEmpty(), "令牌不应为空");
            String source = new String(Base64.getUrlDecoder().decode(token.getBytes(UTF_8)), UTF_8);
            assertEquals(3, source.split("\\.").length, "令牌明文应由 3 段组成");
        }

        @Test
        @DisplayName("过期时间戳落在设定区间内")
        void testExpireTimestamps() {
            long before = System.currentTimeMillis();
            String token = buildNormalToken();
            long after = System.currentTimeMillis();
            long expire = AccessTokenUtil.create().verify(token, SECRET).getExpireTimestamps();
            assertAll("过期时间戳应等于生成时刻 + 60 秒",
                    () -> assertTrue(expire >= before + 60_000, "过期时间戳不应早于设定值"),
                    () -> assertTrue(expire <= after + 60_000, "过期时间戳不应晚于设定值"));
        }

        @Test
        @DisplayName("中文负载值可正常往返")
        void testChinesePayload() {
            String token = accessTokenUtil.setPayloadId(88L)
                    .addPayload("nickname", "汉墨")
                    .setExpireSecond(60)
                    .build(SECRET);
            AccessTokenUtil.VerifiedToken verified = AccessTokenUtil.create().verify(token, SECRET);
            assertEquals("汉墨", verified.getPayload("nickname"), "中文负载应能正确往返");
        }

        @Test
        @DisplayName("大整型 ID 可正常往返")
        void testBigLongId() {
            String token = accessTokenUtil.setPayloadId(9999999999L)
                    .addPayload("k", "v")
                    .setExpireSecond(60)
                    .build(SECRET);
            AccessTokenUtil.VerifiedToken verified = AccessTokenUtil.create().verify(token, SECRET);
            assertEquals(9999999999L, verified.getPayloadId(), "超出 int 范围的 ID 应能正确往返");
        }

        @Test
        @DisplayName("ID 为 0 时可正常往返，getPayloadId 返回 0")
        void testZeroId() {
            String token = accessTokenUtil.setPayloadId(0L)
                    .addPayload("k", "v")
                    .setExpireSecond(60)
                    .build(SECRET);
            AccessTokenUtil.VerifiedToken verified = AccessTokenUtil.create().verify(token, SECRET);
            assertEquals(0L, verified.getPayloadId(), "ID 为 0 时不应抛异常");
        }

        @Test
        @DisplayName("未设置 ID 时 getPayloadId 抛出 401 ServiceException")
        void testNoIdInToken() {
            String token = accessTokenUtil.addPayload("name", "无ID")
                    .setExpireSecond(60)
                    .build(SECRET);
            AccessTokenUtil.VerifiedToken verified = AccessTokenUtil.create().verify(token, SECRET);
            ServiceException exception = assertThrows(ServiceException.class, verified::getPayloadId,
                    "缺少 ID 负载时 getPayloadId 应抛出 ServiceException");
            assertAll("异常应为 401 且提示令牌无效",
                    () -> assertEquals(Json.UNAUTHORIZED_CODE, exception.getCode(), "错误码应为 401"),
                    () -> assertEquals(INVALID_MESSAGE, exception.getMessage(), "异常信息应为：" + INVALID_MESSAGE));
        }

        @Test
        @DisplayName("ID 为 null 时无法通过校验")
        void testNullId() {
            String token = accessTokenUtil.setPayloadId(null)
                    .addPayload("name", "空ID")
                    .setExpireSecond(60)
                    .build(SECRET);
            AccessTokenUtil.VerifiedToken verified = AccessTokenUtil.create().verify(token, SECRET);
            assertThrows(ServiceException.class, verified::getPayloadId,
                    "ID 为 null 时 getPayloadId 应抛出 ServiceException");
        }

        @Test
        @DisplayName("空值负载不影响其它负载，读取时返回 null")
        void testNullPayloadValue() {
            String token = accessTokenUtil.setPayloadId(5L)
                    .addPayload("empty", null)
                    .addPayload("name", "Hamm")
                    .setExpireSecond(60)
                    .build(SECRET);
            AccessTokenUtil.VerifiedToken verified = AccessTokenUtil.create().verify(token, SECRET);
            assertAll("空值负载不应影响其它负载",
                    () -> assertEquals("Hamm", verified.getPayload("name"), "其它负载应正常"),
                    () -> assertNull(verified.getPayload("empty"), "空值负载读取时应返回 null"),
                    () -> assertEquals(5L, verified.getPayloadId(), "ID 应可正常读取"));
        }
    }

    @Nested
    @DisplayName("5. verify 的异常分支")
    class VerifyException {

        @Test
        @DisplayName("secret 为 null / 空串 / 空白串时抛出 401 ServiceException")
        void testBlankSecret() {
            String token = buildNormalToken();
            for (String blank : new String[]{null, "", "   "}) {
                ServiceException exception = assertThrows(ServiceException.class,
                        () -> AccessTokenUtil.create().verify(token, blank),
                        "secret 为 [" + blank + "] 时应抛出 ServiceException");
                assertAll("异常应为 401 且提示配置环境变量",
                        () -> assertEquals(Json.UNAUTHORIZED_CODE, exception.getCode(), "错误码应为 401"),
                        () -> assertEquals(NO_SECRET_MESSAGE, exception.getMessage(),
                                "异常信息应为：" + NO_SECRET_MESSAGE));
            }
        }

        @Test
        @DisplayName("accessToken 为 null 时抛出 401 ServiceException")
        void testNullToken() {
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> AccessTokenUtil.create().verify(null, SECRET),
                    "令牌为 null 时应抛出 ServiceException");
            assertAll("异常应为 401 且提示令牌无效",
                    () -> assertEquals(Json.UNAUTHORIZED_CODE, exception.getCode(), "错误码应为 401"),
                    () -> assertEquals(INVALID_MESSAGE, exception.getMessage(), "异常信息应为：" + INVALID_MESSAGE));
        }

        @Test
        @DisplayName("accessToken 为空串或空白串时抛出 401 ServiceException")
        void testBlankToken() {
            for (String blank : new String[]{"", "   ", "\t\n "}) {
                ServiceException exception = assertThrows(ServiceException.class,
                        () -> AccessTokenUtil.create().verify(blank, SECRET),
                        "令牌为空白串时[" + blank + "]应抛出 ServiceException");
                assertAll("空白令牌应报 401 且提示令牌无效",
                        () -> assertEquals(Json.UNAUTHORIZED_CODE, exception.getCode(), "错误码应为 401"),
                        () -> assertEquals(INVALID_MESSAGE, exception.getMessage(),
                                "异常信息应为：" + INVALID_MESSAGE));
            }
        }

        @Test
        @DisplayName("accessToken 为 null / 空串 / 纯空白时统一按无效令牌处理")
        void testNullAndBlankTokenAreRejected() {
            // 源码新增的前置判断：空令牌在解码之前就应被拒绝，不再走到 Base64 解码分支
            for (String token : new String[]{null, "", "   ", "\t\n "}) {
                ServiceException exception = assertThrows(ServiceException.class,
                        () -> AccessTokenUtil.create().verify(token, SECRET),
                        "令牌为[" + token + "]时应在解码前直接抛出 ServiceException");
                assertAll("空令牌应报 401 且提示令牌无效",
                        () -> assertEquals(Json.UNAUTHORIZED_CODE, exception.getCode(), "错误码应为 401"),
                        () -> assertEquals(INVALID_MESSAGE, exception.getMessage(),
                                "异常信息应为：" + INVALID_MESSAGE));
            }
        }

        @Test
        @DisplayName("accessToken 不是合法 Base64 时抛出 401 ServiceException")
        void testIllegalBase64Token() {
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> AccessTokenUtil.create().verify("这不是Base64!!!", SECRET),
                    "非法 Base64 令牌应抛出 ServiceException");
            assertEquals(INVALID_MESSAGE, exception.getMessage(), "异常信息应为：" + INVALID_MESSAGE);
        }

        @Test
        @DisplayName("解码后不是 3 段时抛出 401 ServiceException")
        void testWrongPartCount() {
            String onePart = Base64.getUrlEncoder().encodeToString("只有一段".getBytes(UTF_8));
            String fourParts = Base64.getUrlEncoder().encodeToString("一.二.三.四".getBytes(UTF_8));
            for (String token : new String[]{onePart, fourParts}) {
                ServiceException exception = assertThrows(ServiceException.class,
                        () -> AccessTokenUtil.create().verify(token, SECRET),
                        "分段数不为 3 时应抛出 ServiceException");
                assertEquals(INVALID_MESSAGE, exception.getMessage(), "异常信息应为：" + INVALID_MESSAGE);
            }
        }

        @Test
        @DisplayName("签名段被篡改时抛出 401 ServiceException")
        void testTamperedSignature() {
            String source = new String(Base64.getUrlDecoder().decode(buildNormalToken().getBytes(UTF_8)), UTF_8);
            String[] list = source.split("\\.");
            String tampered = list[0] + "." + "0".repeat(64) + "." + list[2];
            String token = Base64.getUrlEncoder().encodeToString(tampered.getBytes(UTF_8));
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> AccessTokenUtil.create().verify(token, SECRET),
                    "签名被篡改时应抛出 ServiceException");
            assertEquals(INVALID_MESSAGE, exception.getMessage(), "异常信息应为：" + INVALID_MESSAGE);
        }

        @Test
        @DisplayName("负载段被篡改时抛出 401 ServiceException")
        void testTamperedPayload() {
            String source = new String(Base64.getUrlDecoder().decode(buildNormalToken().getBytes(UTF_8)), UTF_8);
            String[] list = source.split("\\.");
            String newPayload = Base64.getUrlEncoder().encodeToString("{\"id\":6666}".getBytes(UTF_8));
            String tampered = list[0] + "." + list[1] + "." + newPayload;
            String token = Base64.getUrlEncoder().encodeToString(tampered.getBytes(UTF_8));
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> AccessTokenUtil.create().verify(token, SECRET),
                    "负载被篡改时签名校验不通过，应抛出 ServiceException");
            assertEquals(INVALID_MESSAGE, exception.getMessage(), "异常信息应为：" + INVALID_MESSAGE);
        }

        @Test
        @DisplayName("使用错误的密钥校验时抛出 401 ServiceException")
        void testWrongSecret() {
            String token = buildNormalToken();
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> AccessTokenUtil.create().verify(token, SECRET + "-错误"),
                    "使用错误密钥时应抛出 ServiceException");
            assertEquals(INVALID_MESSAGE, exception.getMessage(), "异常信息应为：" + INVALID_MESSAGE);
        }

        @Test
        @DisplayName("令牌已过期时抛出 401 ServiceException")
        void testExpiredToken() throws InterruptedException {
            String token = accessTokenUtil.setPayloadId(7L)
                    .setExpireMillisecond(1)
                    .build(SECRET);
            Thread.sleep(50);
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> AccessTokenUtil.create().verify(token, SECRET),
                    "过期令牌应抛出 ServiceException");
            assertAll("异常应为 401 且提示令牌无效",
                    () -> assertEquals(Json.UNAUTHORIZED_CODE, exception.getCode(), "错误码应为 401"),
                    () -> assertEquals(INVALID_MESSAGE, exception.getMessage(), "异常信息应为：" + INVALID_MESSAGE));
        }

        @Test
        @DisplayName("校验顺序：secret 优先于令牌内容")
        void testValidationOrder() {
            assertEquals(NO_SECRET_MESSAGE,
                    assertThrows(ServiceException.class,
                            () -> AccessTokenUtil.create().verify("完全错误的令牌", ""),
                            "secret 与令牌同时非法时应优先报 secret 的错误").getMessage(),
                    "应优先提示密钥配置问题");
        }
    }

    @Nested
    @DisplayName("6. VerifiedToken 取值方法")
    class VerifiedToken {

        @Test
        @DisplayName("getPayloads 返回完整的负载集合")
        void testGetPayloads() {
            String token = accessTokenUtil.setPayloadId(1001L)
                    .addPayload("name", "Hamm")
                    .setExpireSecond(60)
                    .build(SECRET);
            Map<String, Object> payloads = AccessTokenUtil.create().verify(token, SECRET).getPayloads();
            assertAll("负载集合应包含全部写入项",
                    () -> assertNotNull(payloads, "getPayloads 不应返回 null"),
                    () -> assertEquals(2, payloads.size(), "负载数量应为 2"),
                    () -> assertEquals(1001, payloads.get(Constant.ID), "ID 负载应存在"),
                    () -> assertEquals("Hamm", payloads.get("name"), "name 负载应存在"));
        }

        @Test
        @DisplayName("getPayload 读取不存在的 Key 返回 null")
        void testGetPayloadUnknownKey() {
            AccessTokenUtil.VerifiedToken token = new AccessTokenUtil.VerifiedToken()
                    .setPayloads(Map.of(Constant.ID, 1))
                    .setExpireTimestamps(System.currentTimeMillis() + 1000);
            assertNull(token.getPayload("不存在的键"), "读取不存在的负载应返回 null");
        }

        @Test
        @DisplayName("getExpireTimestamps 返回设置的时间戳")
        void testGetExpireTimestamps() {
            long expect = System.currentTimeMillis() + 30_000;
            AccessTokenUtil.VerifiedToken token = new AccessTokenUtil.VerifiedToken().setExpireTimestamps(expect);
            assertEquals(expect, token.getExpireTimestamps(), "过期时间戳应原样返回");
        }

        @Test
        @DisplayName("getPayloadId 在 ID 不是数字时抛 401 ServiceException")
        void testNonNumericId() {
            AccessTokenUtil.VerifiedToken token = new AccessTokenUtil.VerifiedToken()
                    .setPayloads(Map.of(Constant.ID, "not-a-number"));
            // 负载被篡改时按无效令牌处理，否则上层按"未授权"统一拦截会漏掉这种畸形令牌
            ServiceException exception = assertThrows(ServiceException.class, token::getPayloadId,
                    "ID 非数字应按无效令牌处理，而不是泄漏 NumberFormatException");
            assertEquals(Json.UNAUTHORIZED_CODE, exception.getCode(), "应携带 401 未授权码");
        }

        @Test
        @DisplayName("默认构造的 VerifiedToken 负载为空、过期时间为 0")
        void testDefaults() {
            AccessTokenUtil.VerifiedToken token = new AccessTokenUtil.VerifiedToken();
            assertAll("默认值应当符合定义",
                    () -> assertTrue(token.getPayloads().isEmpty(), "默认负载集合应为空"),
                    () -> assertEquals(0L, token.getExpireTimestamps(), "默认过期时间戳应为 0"),
                    () -> assertNull(token.getPayload("id"), "默认负载读取应返回 null"));
        }
    }

    @Nested
    @DisplayName("7. 畸形令牌的处理（统一按无效令牌拒绝）")
    class KnownBehavior {

        @Test
        @DisplayName("过期时间戳为 0 的令牌按已过期拒绝（不再表示永不过期）")
        void testNeverExpireToken() {
            // 安全修复：源码已移除 `expire != 0` 的短路判断，0 不再代表永不过期
            String token = buildRawToken(SECRET, "0", "{\"id\":1,\"name\":\"永久令牌\"}");
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> AccessTokenUtil.create().verify(token, SECRET),
                    "过期时间戳为 0 的令牌应被判定为已过期，不应再被放行");
            assertAll("过期时间为 0 的令牌应报 401 且提示令牌无效",
                    () -> assertEquals(Json.UNAUTHORIZED_CODE, exception.getCode(), "错误码应为 401"),
                    () -> assertEquals(INVALID_MESSAGE, exception.getMessage(), "异常信息应为：" + INVALID_MESSAGE));
        }

        @Test
        @DisplayName("过期时间戳为非数字时抛出 401 ServiceException（不再泄漏 NumberFormatException）")
        void testNonNumericExpire() {
            String token = buildRawToken(SECRET, "not-a-number", "{\"id\":1}");
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> AccessTokenUtil.create().verify(token, SECRET),
                    "过期时间戳非数字时应统一包装为 ServiceException，而不是 NumberFormatException");
            assertAll("异常应为 401 且提示令牌无效",
                    () -> assertEquals(Json.UNAUTHORIZED_CODE, exception.getCode(), "错误码应为 401"),
                    () -> assertEquals(INVALID_MESSAGE, exception.getMessage(), "异常信息应为：" + INVALID_MESSAGE));
        }

        @Test
        @DisplayName("负载段不是合法 Base64 时抛出 401 ServiceException（不再泄漏 IllegalArgumentException）")
        void testIllegalPayloadBase64() {
            // 拼接一个签名正确但负载段非 Base64 的令牌
            String expire = String.valueOf(System.currentTimeMillis() + 60_000);
            String badPayload = "***不是Base64***";
            String raw = expire + "." + hmacSha256(SECRET, expire + "." + badPayload) + "." + badPayload;
            String token = Base64.getUrlEncoder().encodeToString(raw.getBytes(UTF_8));
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> AccessTokenUtil.create().verify(token, SECRET),
                    "负载段非 Base64 时应统一包装为 ServiceException，而不是 IllegalArgumentException");
            assertAll("异常应为 401 且提示令牌无效",
                    () -> assertEquals(Json.UNAUTHORIZED_CODE, exception.getCode(), "错误码应为 401"),
                    () -> assertEquals(INVALID_MESSAGE, exception.getMessage(), "异常信息应为：" + INVALID_MESSAGE));
        }

        @Test
        @DisplayName("负载段是合法 Base64 但不是 JSON 时抛出 401 ServiceException")
        void testPayloadNotJson() {
            String token = buildRawToken(SECRET, String.valueOf(System.currentTimeMillis() + 60_000), "我不是一个JSON");
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> AccessTokenUtil.create().verify(token, SECRET),
                    "负载不是 JSON 时应抛出 ServiceException");
            assertAll("异常应为 401 且提示令牌无效",
                    () -> assertEquals(Json.UNAUTHORIZED_CODE, exception.getCode(), "错误码应为 401"),
                    () -> assertEquals(INVALID_MESSAGE, exception.getMessage(),
                            "负载解析失败应统一提示令牌无效，而不是暴露 JSON 解析的原始信息，实际为："
                                    + exception.getMessage()));
        }
    }

    /**
     * <h2>溢出与畸形负载</h2>
     *
     * <p>回归 P1-20 / P1-21：{@code getPayloadId} 此前会泄漏 {@code NumberFormatException}；
     * {@code setExpireSecond} 的乘法溢出会报"毫秒数"错误，把排查方向带偏。</p>
     */
    @Nested
    @DisplayName("溢出与畸形负载")
    class OverflowAndMalformedTest {

        @Test
        @DisplayName("getPayloadId 遇到非数字负载抛 401 而不是 NumberFormatException")
        void getPayloadIdRejectsNonNumeric() {
            AccessTokenUtil.VerifiedToken token = new AccessTokenUtil.VerifiedToken()
                    .setPayloads(Map.of(Constant.ID, "abc"));
            ServiceException exception = assertThrows(ServiceException.class, token::getPayloadId,
                    "畸形负载应按无效令牌处理，不能泄漏 JDK 的 NumberFormatException");
            assertEquals(Json.UNAUTHORIZED_CODE, exception.getCode(), "应携带 401 未授权码");
        }

        @Test
        @DisplayName("getPayloadId 缺少 ID 负载时同样抛 401")
        void getPayloadIdRejectsMissing() {
            AccessTokenUtil.VerifiedToken token = new AccessTokenUtil.VerifiedToken()
                    .setPayloads(Map.of("other", "x"));
            assertThrows(ServiceException.class, token::getPayloadId,
                    "缺少 ID 负载应按无效令牌处理");
        }

        @Test
        @DisplayName("getPayloadId 对数字字符串正常返回")
        void getPayloadIdAcceptsNumericString() {
            AccessTokenUtil.VerifiedToken token = new AccessTokenUtil.VerifiedToken()
                    .setPayloads(Map.of(Constant.ID, "12345"));
            assertEquals(12345L, token.getPayloadId(), "数字字符串应被正常解析");
        }

        @Test
        @DisplayName("setExpireSecond 溢出时报的是秒而不是毫秒")
        void setExpireSecondRejectsOverflow() {
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> AccessTokenUtil.create().setExpireSecond(Long.MAX_VALUE),
                    "秒数过大导致乘法溢出，应明确报秒数");
            // 修复前 second * 1000 溢成负数，报的是"过期毫秒数必须大于0"，
            // 调用方传的是秒却被告知毫秒有问题
            assertTrue(exception.getMessage().contains("秒"),
                    "错误信息应提到秒数：" + exception.getMessage());
            assertFalse(exception.getMessage().contains("毫秒"),
                    "错误信息不应误导为毫秒数问题：" + exception.getMessage());
        }

        @Test
        @DisplayName("setExpireSecond 负数仍按秒报错")
        void setExpireSecondRejectsNonPositive() {
            assertEquals("过期秒数必须大于0",
                    assertThrows(ServiceException.class,
                            () -> AccessTokenUtil.create().setExpireSecond(0L),
                            "秒数为 0 应报错").getMessage(),
                    "非正秒数的提示应保持不变");
        }

        @Test
        @DisplayName("临界值秒数不会被误判为溢出")
        void setExpireSecondAcceptsLargeButValidValue() {
            // 约 292 年，在 long 范围内，不应触发溢出分支
            long validSecond = Long.MAX_VALUE / DateTimeUtil.MILLISECONDS_PER_SECOND;
            assertDoesNotThrow(() -> AccessTokenUtil.create().setExpireSecond(validSecond),
                    "范围内的秒数 " + validSecond + " 不应被误判为溢出");
        }
    }
}
