package cn.hamm.airpower.core;

import cn.hamm.airpower.core.exception.ServiceException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.BiFunction;

import static java.math.RoundingMode.*;

/**
 * <h1>数字工具类</h1>
 *
 * @author Hamm.cn
 */
public class NumberUtil {
    /**
     * 计算的最大精度保留
     */
    private static final int DEFAULT_SCALE = 8;

    /**
     * 默认除法的保留方式
     */
    private static final RoundingMode DEFAULT_ROUNDING_MODE = HALF_UP;

    /**
     * 空的 {@code double} 数组（可变参数为 {@code null} 时的兜底）
     */
    private static final double[] EMPTY_DOUBLE = {};

    /**
     * 空的 {@code long} 数组（可变参数为 {@code null} 时的兜底）
     */
    private static final long[] EMPTY_LONG = {};

    /**
     * 禁止外部实例化
     */
    @Contract(pure = true)
    private NumberUtil() {
    }

    /**
     * 多个数求和
     *
     * @param first  加数
     * @param second 被加数
     * @param values 更多被加数
     * @return 和
     */
    public static double add(double first, double second, double... values) {
        return calculate(BigDecimal::add, BigDecimal.valueOf(first), BigDecimal.valueOf(second),
                Arrays.stream(Objects.requireNonNullElse(values, EMPTY_DOUBLE))
                        .mapToObj(BigDecimal::valueOf).toArray(BigDecimal[]::new)
        ).doubleValue();
    }

    /**
     * 多个数求和
     *
     * @param first  加数
     * @param second 被加数
     * @param values 更多被加数
     * @return 和
     */
    public static long add(long first, long second, long... values) {
        return toLongExact(calculate(BigInteger::add, BigInteger.valueOf(first), BigInteger.valueOf(second),
                Arrays.stream(Objects.requireNonNullElse(values, EMPTY_LONG))
                        .mapToObj(BigInteger::valueOf).toArray(BigInteger[]::new)
        ), "加法");
    }

    /**
     * 多个数相减
     *
     * @param first  被减数
     * @param second 减数
     * @param values 更多减数
     * @return 差
     */
    public static double subtract(double first, double second, double... values) {
        return calculate(BigDecimal::subtract, BigDecimal.valueOf(first), BigDecimal.valueOf(second),
                Arrays.stream(Objects.requireNonNullElse(values, EMPTY_DOUBLE))
                        .mapToObj(BigDecimal::valueOf).toArray(BigDecimal[]::new)
        ).doubleValue();
    }

    /**
     * 多个数相减
     *
     * @param first  被减数
     * @param second 减数
     * @param values 更多减数
     * @return 差
     */
    public static long subtract(long first, long second, long... values) {
        return toLongExact(calculate(BigInteger::subtract, BigInteger.valueOf(first), BigInteger.valueOf(second),
                Arrays.stream(Objects.requireNonNullElse(values, EMPTY_LONG))
                        .mapToObj(BigInteger::valueOf).toArray(BigInteger[]::new)
        ), "减法");
    }

    /**
     * 多个数相乘
     *
     * @param first  乘数
     * @param second 被乘数
     * @param values 更多被乘数
     * @return 乘积
     */
    public static double multiply(double first, double second, double... values) {
        return calculate(BigDecimal::multiply, BigDecimal.valueOf(first), BigDecimal.valueOf(second),
                Arrays.stream(Objects.requireNonNullElse(values, EMPTY_DOUBLE))
                        .mapToObj(BigDecimal::valueOf).toArray(BigDecimal[]::new)
        ).doubleValue();
    }

    /**
     * 多个数相乘
     *
     * @param first  乘数
     * @param second 被乘数
     * @param values 更多被乘数
     * @return 乘积
     */
    public static long multiply(long first, long second, long... values) {
        return toLongExact(calculate(BigInteger::multiply, BigInteger.valueOf(first), BigInteger.valueOf(second),
                Arrays.stream(Objects.requireNonNullElse(values, EMPTY_LONG))
                        .mapToObj(BigInteger::valueOf).toArray(BigInteger[]::new)
        ), "乘法");
    }

    /**
     * 多个数相除
     *
     * @param first  被除数
     * @param second 除数
     * @return 商
     */
    public static double divide(double first, double second) {
        return divide(first, second, DEFAULT_SCALE);
    }

    /**
     * 多个数相除
     *
     * @param first  被除数
     * @param second 除数
     * @param scale  保留位数
     * @return 商
     */
    public static double divide(double first, double second, int scale) {
        return divide(BigDecimal.valueOf(first), BigDecimal.valueOf(second), scale, DEFAULT_ROUNDING_MODE)
                .doubleValue();
    }

    /**
     * 多个数相除
     *
     * @param first        被除数
     * @param second       除数
     * @param scale        保留位数
     * @param roundingMode 舍弃方式
     * @return 商
     */
    public static double divide(double first, double second, int scale, RoundingMode roundingMode) {
        return divide(BigDecimal.valueOf(first), BigDecimal.valueOf(second), scale, roundingMode)
                .doubleValue();
    }

    /**
     * 多个数相除
     *
     * @param first  被除数
     * @param second 除数
     * @return 商
     */
    public static double divide(long first, long second) {
        return divide(first, second, DEFAULT_SCALE);
    }

    /**
     * 多个数相除
     *
     * @param first  被除数
     * @param second 除数
     * @param scale  保留位数
     * @return 商
     */
    public static double divide(long first, long second, int scale) {
        return divide(BigDecimal.valueOf(first), BigDecimal.valueOf(second), scale, DEFAULT_ROUNDING_MODE)
                .doubleValue();
    }

    /**
     * 多个数相除
     *
     * @param first        被除数
     * @param second       除数
     * @param scale        保留位数
     * @param roundingMode 舍弃方式
     * @return 商
     */
    public static double divide(long first, long second, int scale, RoundingMode roundingMode) {
        return divide(BigDecimal.valueOf(first), BigDecimal.valueOf(second), scale, roundingMode)
                .doubleValue();
    }

    /**
     * 计算的业务逻辑
     *
     * @param function 计算执行方法
     * @param first    第一个数据
     * @param second   第二个数据
     * @param values   更多的数据
     * @param <T>      数据类型
     * @return 计算的结果
     */
    private static <T extends Number> T calculate(@NotNull BiFunction<T, T, T> function, T first, T second, T[] values) {
        T result = function.apply(first, second);
        // 入参已由调用方用 requireNonNullElse 兜底，此处恒不为 null
        for (T value : values) {
            result = function.apply(result, value);
        }
        return result;
    }

    /**
     * 将 {@link BigInteger} 收窄为 {@code long}，越界时报错
     *
     * @param value  计算结果
     * @param action 操作名称，用于错误提示
     * @return 收窄后的值
     * @apiNote 直接调用 {@code longValue()} 会静默截断（{@code multiply(MAX, 4)} 会得到
     * {@code -4}），金额等场景必须显式拦截溢出
     */
    private static long toLongExact(@NotNull BigInteger value, @NotNull String action) {
        if (value.bitLength() > Long.SIZE - 1) {
            throw new ServiceException(action + "结果超出 long 范围，" + value);
        }
        return value.longValue();
    }

    /**
     * 多个数相除
     *
     * @param first        被除数
     * @param second       除数
     * @param scale        保留位数
     * @param roundingMode 舍弃方式
     * @return 商
     */
    private static @NotNull BigDecimal divide(BigDecimal first, BigDecimal second, int scale, RoundingMode roundingMode) {
        if (Objects.isNull(second) || second.compareTo(BigDecimal.ZERO) == 0) {
            throw new ServiceException("除数不能为0");
        }
        return first.divide(second, scale, roundingMode);
    }

    /**
     * 向下取整（向负无穷方向）
     *
     * @param value 数字
     * @param scale 位数
     * @return 取整后的数字
     * @apiNote 使用 {@link RoundingMode#FLOOR}。原实现用 {@code DOWN}（向零截断），
     * 负数结果全错：{@code floor(-1.5, 0)} 会得到 {@code -1}，正确值是 {@code -2}
     */
    public static @NotNull BigDecimal floor(double value, int scale) {
        return round(value, scale, FLOOR);
    }

    /**
     * 向上取整（向正无穷方向）
     *
     * @param value 数字
     * @param scale 位数
     * @return 取整后的数字
     * @apiNote 使用 {@link RoundingMode#CEILING}。原实现用 {@code UP}（远离零），
     * 负数结果全错：{@code ceil(-1.5, 0)} 会得到 {@code -2}，正确值是 {@code -1}
     */
    public static @NotNull BigDecimal ceil(double value, int scale) {
        return round(value, scale, CEILING);
    }

    /**
     * 保留固定位数小数
     *
     * @param number       数字值
     * @param scale        保留小数位数
     * @param roundingMode 保留小数的模式 {@link RoundingMode}
     * @return 新值
     */
    public static @NotNull BigDecimal round(double number, int scale, @NotNull RoundingMode roundingMode) {
        if (scale < 0) {
            // 负 scale 会被静默改成 0，调用方难以及时发现传参错误
            throw new ServiceException("保留位数不能小于0，" + scale);
        }
        if (Objects.isNull(roundingMode)) {
            throw new ServiceException("舍弃方式不能为null");
        }
        return BigDecimal.valueOf(number).setScale(scale, roundingMode);
    }
}
