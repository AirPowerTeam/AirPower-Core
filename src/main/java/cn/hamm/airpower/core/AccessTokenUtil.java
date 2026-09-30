package cn.hamm.airpower.core;

import cn.hamm.airpower.core.constant.Constant;
import cn.hamm.airpower.core.exception.ServiceException;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * <h1>AccessToken 工具类</h1>
 *
 * @author Hamm.cn
 */
@Slf4j
public class AccessTokenUtil {
    /**
     * 无效的令牌
     */
    private static final String ACCESS_TOKEN_INVALID = "身份令牌无效，请重新获取身份令牌";

    /**
     * 请先设置密钥环境变量
     */
    private static final String SET_ENV_TOKEN_SECRET_FIRST = "请在环境变量配置 airpower.accessTokenSecret";

    /**
     * 算法
     */
    private static final String HMAC_SHA_256 = "HmacSHA256";

    /**
     * Token 分隔符
     */
    private static final String TOKEN_DELIMITER = ".";

    /**
     * {@code HMAC-SHA-256}错误
     */
    private static final String HMAC_SHA_256_ERROR = "HMAC-SHA-256发生错误";

    /**
     * {@code Token} 由 {@code 3} 部分组成
     */
    private static final int TOKEN_PART_COUNT = 3;

    /**
     * 验证后的 Token
     */
    private VerifiedToken verifiedToken;

    /**
     * 禁止外部实例化
     */
    @Contract(pure = true)
    private AccessTokenUtil() {
    }

    /**
     * 创建实例
     *
     * @return AccessTokenUtil 实例
     */
    public static @NotNull AccessTokenUtil create() {
        AccessTokenUtil accessTokenUtil = new AccessTokenUtil();
        accessTokenUtil.verifiedToken = new VerifiedToken();
        return accessTokenUtil;
    }

    /**
     * 抛出异常
     *
     * @param message 错误信息
     */
    @Contract("_ -> fail")
    private static void throwException(String message) {
        throw new ServiceException(Json.UNAUTHORIZED_CODE, message);
    }

    /**
     * 恒等比较
     *
     * @param a 第一个字符串
     * @param b 第二个字符串
     * @return 是否相等
     */
    private static boolean constantTimeEquals(@NotNull String a, @NotNull String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 解析令牌中的过期时间
     *
     * @param value 令牌中的过期时间字符串
     * @return 过期时间（毫秒）
     * @apiNote 非法内容按无效令牌处理；时间为 0 同样视为已过期
     */
    private static long parseExpireTimestamps(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throwException(ACCESS_TOKEN_INVALID);
            return 0L;
        }
    }

    /**
     * 解析令牌中的负载数据
     *
     * @param value 令牌中的负载字符串
     * @return 负载数据
     * @apiNote 非法内容按无效令牌处理
     */
    private static @NotNull Map<String, Object> parsePayloads(String value) {
        try {
            return Json.parse2Map(new String(Base64.getUrlDecoder().decode(value.getBytes(UTF_8))));
        } catch (Exception e) {
            throwException(ACCESS_TOKEN_INVALID);
            return new HashMap<>();
        }
    }

    /**
     * 字节数组转小写十六进制字符串
     *
     * @param bytes 字节数组
     * @return 十六进制字符串，每个字节占两位
     */
    private static @NotNull String toHex(byte @NotNull [] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        char[] digits = "0123456789abcdef".toCharArray();
        for (int i = 0; i < bytes.length; i++) {
            int value = bytes[i] & 0xff;
            hexChars[i * 2] = digits[value >>> 4];
            hexChars[i * 2 + 1] = digits[value & 0x0f];
        }
        return new String(hexChars);
    }

    /**
     * 创建一个 AccessToken
     *
     * @param id TokenID
     * @return AccessTokenUtil 实例
     * @apiNote 不设置令牌过期时间
     */
    public AccessTokenUtil setPayloadId(Long id) {
        return addPayload(Constant.ID, id);
    }

    /**
     * 生成 {@code Token}
     *
     * @param secret 密钥
     * @return AccessToken
     */
    public final String build(String secret) {
        if (!StringUtil.hasText(secret)) {
            throwException("身份令牌创建失败，" + SET_ENV_TOKEN_SECRET_FIRST);
        }
        if (verifiedToken.getPayloads().isEmpty()) {
            throw new ServiceException("没有任何负载数据");
        }
        String payloadBase = Base64.getUrlEncoder().encodeToString(
                Json.toString(verifiedToken.getPayloads()).getBytes(UTF_8)
        );
        long expireTimestamps = verifiedToken.getExpireTimestamps();
        if (expireTimestamps <= 0) {
            throw new ServiceException("令牌必须设置过期时间");
        }
        String content = expireTimestamps +
                TOKEN_DELIMITER +
                hmacSha256(secret, expireTimestamps + TOKEN_DELIMITER + payloadBase) +
                TOKEN_DELIMITER +
                payloadBase;
        return Base64.getUrlEncoder().encodeToString(content.getBytes(UTF_8));
    }

    /**
     * 添加负载
     *
     * @param key   负载的 Key
     * @param value 负载的 Value
     * @return AccessTokenUtil 实例
     */
    @Contract("_, _ -> this")
    public final AccessTokenUtil addPayload(String key, Object value) {
        verifiedToken.getPayloads().put(key, value);
        return this;
    }

    /**
     * 移除负载
     *
     * @param key 负载 Key
     * @return AccessTokenUtil 实例
     */
    @Contract("_ -> this")
    public final AccessTokenUtil removePayload(String key) {
        verifiedToken.getPayloads().remove(key);
        return this;
    }

    /**
     * 设置过期时间 {@code 毫秒}
     *
     * @param millisecond 过期毫秒
     * @return AccessTokenUtil 实例
     */
    @Contract("_ -> this")
    public final AccessTokenUtil setExpireMillisecond(long millisecond) {
        if (millisecond <= 0) {
            throw new ServiceException("过期毫秒数必须大于0");
        }
        verifiedToken.setExpireTimestamps(System.currentTimeMillis() + millisecond);
        return this;
    }

    /**
     * 设置过期时间 {@code 秒}
     *
     * @param second 秒数
     * @return AccessTokenUtil 实例
     */
    @Contract("_ -> this")
    public final AccessTokenUtil setExpireSecond(long second) {
        if (second <= 0) {
            throw new ServiceException("过期秒数必须大于0");
        }
        // 直接相乘溢出会变成负数，再传给毫秒重载会报"过期毫秒数必须大于0"，
        // 调用方传的是秒却被告知毫秒有问题，排查方向会被带偏
        final long millisecond;
        try {
            millisecond = Math.multiplyExact(second, DateTimeUtil.MILLISECONDS_PER_SECOND);
        } catch (ArithmeticException e) {
            throw new ServiceException("过期秒数过大，超出可表示范围：" + second);
        }
        return setExpireMillisecond(millisecond);
    }

    /**
     * 验证 AccessToken 并返回 VerifiedToken
     *
     * @param accessToken AccessToken
     * @param secret      密钥
     * @return VerifiedToken
     */
    public final VerifiedToken verify(String accessToken, String secret) {
        if (!StringUtil.hasText(secret)) {
            throwException(SET_ENV_TOKEN_SECRET_FIRST);
        }
        if (secret.length() < 32) {
            throwException("身份令牌创建失败，令牌最短限制为32位字符");
        }
        if (!StringUtil.hasText(accessToken)) {
            throwException(ACCESS_TOKEN_INVALID);
        }
        String source = "";
        try {
            source = new String(Base64.getUrlDecoder().decode(accessToken.getBytes(UTF_8)));
        } catch (Exception exception) {
            throwException(ACCESS_TOKEN_INVALID);
        }
        if (!StringUtil.hasText(source)) {
            throwException(ACCESS_TOKEN_INVALID);
        }
        String[] list = source.split("\\" + TOKEN_DELIMITER);
        if (list.length != TOKEN_PART_COUNT) {
            throwException(ACCESS_TOKEN_INVALID);
        }
        //noinspection AlibabaUndefineMagicConstant
        if (!constantTimeEquals(hmacSha256(secret, list[0] + TOKEN_DELIMITER + list[2]), list[1])) {
            throwException(ACCESS_TOKEN_INVALID);
        }
        long expireTimestamps = parseExpireTimestamps(list[0]);
        if (expireTimestamps < System.currentTimeMillis()) {
            throwException(ACCESS_TOKEN_INVALID);
        }
        return new VerifiedToken().setExpireTimestamps(expireTimestamps).setPayloads(parsePayloads(list[2]));
    }

    /**
     * HMacSha256 签名
     *
     * @param secret  密钥
     * @param content 数据
     * @return 签名
     */
    private @NotNull String hmacSha256(@NotNull String secret, @NotNull String content) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA_256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(UTF_8), HMAC_SHA_256);
            mac.init(secretKeySpec);
            // 手工转十六进制：String.format 每次调用都要解析格式串，
            // 在令牌签发/校验这类热路径上开销明显
            return toHex(mac.doFinal(content.getBytes(UTF_8)));
        } catch (Exception e) {
            throw new ServiceException(HMAC_SHA_256_ERROR);
        }
    }

    /**
     * 已验证的身份令牌
     *
     * @author Hamm.cn
     */
    @Data
    @Accessors(chain = true)
    public static class VerifiedToken {
        /**
         * 负载数据
         */
        private Map<String, Object> payloads = new HashMap<>();

        /**
         * 过期时间 {@code 毫秒}
         */
        private long expireTimestamps = 0;

        /**
         * 获取负载
         *
         * @param key 负载的 Key
         * @return 负载的 Value
         */
        public final @Nullable Object getPayload(String key) {
            return payloads.get(key);
        }

        /**
         * 获取负载的 {@code ID}
         *
         * @return {@code ID}
         */
        public final long getPayloadId() {
            Object userId = getPayload(Constant.ID);
            if (Objects.isNull(userId)) {
                throwException(ACCESS_TOKEN_INVALID);
            }
            try {
                return Long.parseLong(userId.toString());
            } catch (NumberFormatException e) {
                // 负载被篡改或格式错误时按无效令牌处理，
                // 否则上层按"未授权"统一拦截时会漏掉这种畸形令牌
                throwException(ACCESS_TOKEN_INVALID);
                return 0L;
            }
        }
    }
}
