package cn.hamm.airpower.core;

import cn.hamm.airpower.core.exception.ServiceException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.security.SecureRandom;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * <h2>随机生成工具类</h2>
 *
 * @author Hamm.cn
 */
public class RandomUtil {
    /**
     * 默认长度
     */
    private static final int DEFAULT_LENGTH = 32;

    /**
     * 数字
     */
    private static final String BASE_NUMBER = "0123456789";

    /**
     * 密码学安全随机源
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 安全字符表表
     */
    private static final String SAFE_CHARS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    /**
     * 禁止外部实例化
     */
    @Contract(pure = true)
    private RandomUtil() {

    }

    /**
     * 获取密码学安全的随机字节数组
     *
     * @param length 长度（字节）
     * @return 随机字节数组
     */
    public static byte @NotNull [] randomBytes(int length) {
        if (length < 0) {
            throw new ServiceException("随机字节数组长度不能小于0");
        }
        byte[] bytes = new byte[length];
        SECURE_RANDOM.nextBytes(bytes);
        return bytes;
    }

    /**
     * 获取 {@code 32} 字节的密码学安全随机字节数组
     *
     * @return 随机字节数组
     */
    public static byte @NotNull [] randomBytes() {
        return randomBytes(DEFAULT_LENGTH);
    }

    /**
     * 获取密码学安全的随机字符串
     *
     * @param length 长度（字符）
     * @return 随机字符串，字符集为 URL/文件名安全的 base64url
     */
    public static @NotNull String randomString(int length) {
        if (length <= 0) {
            throw new ServiceException("随机字符串长度不能为负数");
        }
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(SAFE_CHARS.charAt(SECURE_RANDOM.nextInt(SAFE_CHARS.length())));
        }
        return builder.toString();
    }

    /**
     * 获取 {@code 32} 位的密码学安全随机字符串
     *
     * @return 随机字符串
     */
    public static @NotNull String randomString() {
        return randomString(DEFAULT_LENGTH);
    }

    /**
     * 获取密码学安全的随机 {@code int}
     *
     * @return 随机 int
     */
    public static int randomInt() {
        return SECURE_RANDOM.nextInt();
    }

    /**
     * 获取随机数字的字符串
     *
     * @param length 字符串的长度
     * @return 随机字符串
     */
    public static @NotNull String randomNumbers(final int length) {
        return randomString(BASE_NUMBER, length);
    }

    /**
     * 获取指定样本的随机字符串
     *
     * @param baseString 随机字符选取的样本
     * @param length     字符串的长度
     * @return 随机字符串
     */
    public static @NotNull String randomString(final String baseString, int length) {
        if (Objects.isNull(baseString) || baseString.isEmpty()) {
            throw new ServiceException("随机字符样本不能为空");
        }
        if (length <= 0) {
            // 不做静默纠正：把负数当成 1 会让调用方的传参错误被彻底吞掉
            throw new ServiceException("随机字符串长度必须大于0，当前为 " + length);
        }
        final int baseLength = baseString.length();
        return IntStream.range(0, length)
                .map(i -> randomInt(baseLength))
                .mapToObj(number -> String.valueOf(baseString.charAt(number)))
                .collect(Collectors.joining());
    }

    /**
     * 获得 {@code [0, upperBound)} 范围内的随机数
     *
     * @param upperBound 上界（不包含）
     * @return 随机数
     */
    public static int randomInt(final int upperBound) {
        if (upperBound <= 0) {
            throw new ServiceException("随机数上界必须大于0，当前为 " + upperBound);
        }
        return randomInt(0, upperBound, true, false);
    }

    /**
     * 获得指定范围内的随机数
     *
     * @param minInclude 最小数（包含）
     * @param maxExclude 最大数（不包含）
     * @return 随机数
     */
    public static int randomInt(final int minInclude, final int maxExclude) {
        return randomInt(minInclude, maxExclude, true, false);
    }

    /**
     * 获得指定范围内的随机数
     *
     * @param min        最小值
     * @param max        最大值
     * @param includeMin 是否包含最小值
     * @param includeMax 是否包含最大值
     * @return 随机数
     */
    public static int randomInt(int min, int max, final boolean includeMin, final boolean includeMax) {
        long origin = includeMin ? min : (long) min + 1;
        long bound = includeMax ? (long) max + 1 : max;
        if (bound <= origin) {
            throw new ServiceException("随机数区间非法，min=" + min + "，max=" + max
                    + "，includeMin=" + includeMin + "，includeMax=" + includeMax);
        }
        if (bound > Integer.MAX_VALUE) {
            throw new ServiceException("随机数区间的开区间上界超出 int 可表示范围，min=" + min + "，max=" + max
                    + "，includeMin=" + includeMin + "，includeMax=" + includeMax);
        }
        return SECURE_RANDOM.nextInt((int) origin, (int) bound);
    }
}
