package cn.hamm.airpower.core;

import cn.hamm.airpower.core.exception.ServiceException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * <h1>随机生成工具类</h1>
 *
 * @author Hamm.cn
 */
public class RandomUtil {
    /**
     * 默认长度
     */
    private static final int DEFAULT_LENGTH = 32;

    /**
     * 小写字母
     */
    private static final String BASE_CHAR = "abcdefghijklmnopqrstuvwxyz";

    /**
     * 数字
     */
    private static final String BASE_NUMBER = "0123456789";

    /**
     * 小写字母和数字
     */
    private static final String BASE_CHAR_NUMBER_LOWER = BASE_CHAR + BASE_NUMBER;

    /**
     * 大写和小写字母
     *
     * @apiNote 固定 {@link Locale#ROOT}：土耳其语环境下 "i".toUpperCase() 得到
     * 带点的 "İ"，产出的随机串会混入非 ASCII 字符
     */
    private static final String BASE_CHAR_NUMBER = BASE_CHAR.toUpperCase(Locale.ROOT) + BASE_CHAR_NUMBER_LOWER;

    /**
     * 禁止外部实例化
     */
    @Contract(pure = true)
    private RandomUtil() {

    }

    /**
     * 获取随机字节数组
     *
     * @param length 长度（字节）
     * @return 随机字节数组
     */
    public static byte @NotNull [] randomBytes(int length) {
        if (length < 0) {
            throw new ServiceException("随机字节数组长度不能小于0");
        }
        byte[] bytes = new byte[length];
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < length; i++) {
            bytes[i] = (byte) random.nextInt(256);
        }
        return bytes;
    }

    /**
     * 获取 {@code 32} 字节的随机字节数组
     *
     * @return 随机字节数组
     */
    public static byte @NotNull [] randomBytes() {
        return randomBytes(DEFAULT_LENGTH);
    }

    /**
     * 获取 {@code 32} 个字符的随机字符串（大小写字母 + 数字）
     *
     * @return 随机字符串
     */
    public static @NotNull String randomString() {
        return randomString(DEFAULT_LENGTH);
    }

    /**
     * 获取指定位数的随机字符串
     *
     * @param length 字符串的长度
     * @return 随机字符串
     */
    public static @NotNull String randomString(final int length) {
        return randomString(BASE_CHAR_NUMBER, length);
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
     * 获取一个随机整数
     *
     * @return 随机数
     * @return 随机数
     * @see Random#nextInt()
     * @apiNote 无上界版本，<b>返回值可能为负数</b>，需要非负请用带下界的重载
     */
    public static int randomInt() {
        return getRandom().nextInt();
    }

    /**
     * 获得 {@code [0, upperBound)} 范围内的随机数
     *
     * @param upperBound 上界（不包含）
     * @return 随机数
     */
    public static int randomInt(final int upperBound) {
        return getRandom().nextInt(upperBound);
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
     * @apiNote 最终委托给 {@code nextInt(min, max)}（上界不含），因此两个开关都是
     * {@code false} 时区间为空，会抛 {@code IllegalArgumentException}
     * @apiNote 端点语义：{@code includeMin=false} 时下界从 {@code min+1} 起算，
     * {@code includeMax=false} 时上界为 {@code max-1}（含）。两个开关都不含时
     * 结果区间是 {@code [min+1, max-1]}，两个都含时是 {@code [min, max]}，
     * 只开一个时请按上面两条各自换算
     */
    public static int randomInt(int min, int max, final boolean includeMin, final boolean includeMax) {
        if (!includeMin) {
            min++;
        }
        if (includeMax) {
            max++;
        }
        return getRandom().nextInt(min, max);
    }

    /**
     * 获取当前线程的随机数生成器
     *
     * @return 随机数生成器
     * @apiNote 必须是 {@link ThreadLocalRandom}，用共享的 {@link Random} 会让高并发下
     * 争抢同一个 CAS 原子变量
     */
    private static ThreadLocalRandom getRandom() {
        return ThreadLocalRandom.current();
    }
}
