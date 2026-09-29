package cn.hamm.airpower.core;

import cn.hamm.airpower.core.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>NumberUtil 单元测试</h1>
 *
 * <p>覆盖 {@code NumberUtil} 的 14 个 public 方法：
 * add / subtract / multiply 各 2 个重载（含可变参数的三种调用形态）、
 * divide 的 6 个重载、round / floor / ceil。</p>
 *
 * <p>注意：{@code double} 走 {@link BigDecimal}，因此可精确表示十进制小数；
 * {@code long} 走 {@link java.math.BigInteger}，超出 64 位时会截断（见各用例注释）。</p>
 *
 * @author Hamm.cn
 */
@DisplayName("NumberUtil 数字工具类测试")
public class NumberUtilTest {

    /**
     * 浮点比较容差
     */
    private static final double DELTA = 1e-9;

    @Nested
    @DisplayName("add 求和")
    class Add {

        @Test
        @DisplayName("double 重载：两个数相加")
        void doubleTwoArgs() {
            assertEquals(4.0, NumberUtil.add(1.5, 2.5), DELTA, "1.5 + 2.5 应等于 4.0");
            assertEquals(0.0, NumberUtil.add(0.0, 0.0), DELTA, "0 + 0 应等于 0.0");
            assertEquals(-1.0, NumberUtil.add(2.0, -3.0), DELTA, "混合正负数求和应正确");
        }

        @Test
        @DisplayName("double 重载：经 BigDecimal 运算，0.1 + 0.2 精确等于 0.3")
        void doublePrecision() {
            // 源码用 BigDecimal.valueOf(double) 构造，0.1 + 0.2 的结果精确为 0.3，无浮点误差
            assertEquals(0.3, NumberUtil.add(0.1, 0.2), 0.0, "BigDecimal 运算下 0.1 + 0.2 应精确等于 0.3");
        }

        @Test
        @DisplayName("double 重载：可变参数追加第三个数")
        void doubleThreeArgs() {
            assertEquals(6.0, NumberUtil.add(1.0, 2.0, 3.0), DELTA, "1 + 2 + 3 应等于 6.0");
            assertEquals(0.5, NumberUtil.add(1.0, 2.0, -2.5), DELTA, "可变参数可为负数");
        }

        @Test
        @DisplayName("double 重载：可变参数传入多个数（求和）")
        void doubleMoreArgs() {
            assertEquals(10.0, NumberUtil.add(1.0, 2.0, 3.0, 4.0), DELTA, "1+2+3+4 应等于 10.0");
        }

        @Test
        @DisplayName("double 重载：可变参数为空数组时只做二元加法")
        void doubleEmptyVarargs() {
            assertEquals(4.0, NumberUtil.add(1.5, 2.5, new double[0]), DELTA, "空数组可变参数应被忽略");
            assertEquals(4.0, NumberUtil.add(1.5, 2.5), DELTA, "不传可变参数等价于空数组");
        }

        @Test
        @DisplayName("double 重载：可变参数为 null 时按空数组处理")
        void doubleNullVarargs() {
            // 源码在遍历可变参数前用 Objects.requireNonNullElse(values, 空数组) 兜底，
            // 显式传 null 数组等价于「没有更多参数」，不会抛 NullPointerException
            double result = assertDoesNotThrow(() -> NumberUtil.add(1.0, 2.0, (double[]) null),
                    "可变参数显式传 null 时应按空数组兜底，不应抛出 NullPointerException");
            assertEquals(3.0, result, DELTA, "add(1.0, 2.0, null) 应等价于 add(1.0, 2.0)，结果为 3.0");
            assertEquals(NumberUtil.add(1.0, 2.0, new double[0]), result, DELTA,
                    "传 null 与传空数组的结果应完全一致");
        }

        @Test
        @DisplayName("long 重载：两个数相加")
        void longTwoArgs() {
            assertEquals(3L, NumberUtil.add(1L, 2L), "1 + 2 应等于 3");
            assertEquals(0L, NumberUtil.add(0L, 0L), "0 + 0 应等于 0");
            assertEquals(-1L, NumberUtil.add(2L, -3L), "混合正负数求和应正确");
        }

        @Test
        @DisplayName("long 重载：可变参数追加第三个数")
        void longThreeArgs() {
            assertEquals(6L, NumberUtil.add(1L, 2L, 3L), "1 + 2 + 3 应等于 6");
            assertEquals(-1L, NumberUtil.add(1L, 2L, -4L), "可变参数可为负数");
        }

        @Test
        @DisplayName("long 重载：可变参数为空数组")
        void longEmptyVarargs() {
            assertEquals(3L, NumberUtil.add(1L, 2L, new long[0]), "空数组可变参数应被忽略");
        }

        @Test
        @DisplayName("long 重载：溢出时按 64 位截断")
        void longOverflowTruncated() {
            // BigInteger 精确算出 2^63，再 longValue() 只取低 64 位，得到 Long.MIN_VALUE
            assertEquals(Long.MIN_VALUE, NumberUtil.add(Long.MAX_VALUE, 1L),
                    "超出 long 范围时 longValue() 会回绕为 Long.MIN_VALUE（源码无溢出保护）");
        }

        @Test
        @DisplayName("long 重载：可变参数为 null 时按空数组处理")
        void longNullVarargs() {
            long result = assertDoesNotThrow(() -> NumberUtil.add(1L, 2L, (long[]) null),
                    "可变参数显式传 null 时应按空数组兜底，不应抛出 NullPointerException");
            assertEquals(3L, result, "add(1L, 2L, null) 应等价于 add(1L, 2L)，结果为 3");
            assertEquals(NumberUtil.add(1L, 2L, new long[0]), result,
                    "传 null 与传空数组的结果应完全一致");
        }
    }

    @Nested
    @DisplayName("subtract 相减")
    class Subtract {

        @Test
        @DisplayName("double 重载：两个数相减")
        void doubleTwoArgs() {
            assertEquals(7.0, NumberUtil.subtract(10.0, 3.0), DELTA, "10 - 3 应等于 7.0");
            assertEquals(0.0, NumberUtil.subtract(0.0, 0.0), DELTA, "0 - 0 应等于 0.0");
            assertEquals(0.0, NumberUtil.subtract(5.0, 5.0), DELTA, "相同数相减应为 0.0");
            assertEquals(13.0, NumberUtil.subtract(10.0, -3.0), DELTA, "减负数等价于加正数");
        }

        @Test
        @DisplayName("double 重载：连续相减多个数")
        void doubleMoreArgs() {
            assertEquals(4.0, NumberUtil.subtract(10.0, 3.0, 3.0), DELTA, "10-3-3 应等于 4.0");
            assertEquals(1.0, NumberUtil.subtract(10.0, 3.0, 3.0, 3.0), DELTA, "10-3-3-3 应等于 1.0");
        }

        @Test
        @DisplayName("double 重载：可变参数为空数组")
        void doubleEmptyVarargs() {
            assertEquals(7.0, NumberUtil.subtract(10.0, 3.0, new double[0]), DELTA, "空数组可变参数应被忽略");
        }

        @Test
        @DisplayName("double 重载：可变参数为 null 时按空数组处理")
        void doubleNullVarargs() {
            double result = assertDoesNotThrow(() -> NumberUtil.subtract(10.0, 3.0, (double[]) null),
                    "可变参数显式传 null 时应按空数组兜底，不应抛出 NullPointerException");
            assertEquals(7.0, result, DELTA, "subtract(10.0, 3.0, null) 应等价于 subtract(10.0, 3.0)，结果为 7.0");
            assertEquals(NumberUtil.subtract(10.0, 3.0, new double[0]), result, DELTA,
                    "传 null 与传空数组的结果应完全一致");
        }

        @Test
        @DisplayName("long 重载：两个数相减")
        void longTwoArgs() {
            assertEquals(7L, NumberUtil.subtract(10L, 3L), "10 - 3 应等于 7");
            assertEquals(0L, NumberUtil.subtract(5L, 5L), "相同数相减应为 0");
            assertEquals(13L, NumberUtil.subtract(10L, -3L), "减负数等价于加正数");
        }

        @Test
        @DisplayName("long 重载：连续相减多个数")
        void longMoreArgs() {
            assertEquals(4L, NumberUtil.subtract(10L, 3L, 3L), "10-3-3 应等于 4");
            assertEquals(1L, NumberUtil.subtract(10L, 3L, 3L, 3L), "10-3-3-3 应等于 1");
        }

        @Test
        @DisplayName("long 重载：可变参数为空数组")
        void longEmptyVarargs() {
            assertEquals(7L, NumberUtil.subtract(10L, 3L, new long[0]), "空数组可变参数应被忽略");
        }

        @Test
        @DisplayName("long 重载：下溢时按 64 位截断")
        void longUnderflowTruncated() {
            assertEquals(Long.MAX_VALUE, NumberUtil.subtract(Long.MIN_VALUE, 1L),
                    "低于 long 范围时 longValue() 会回绕为 Long.MAX_VALUE（源码无溢出保护）");
        }
    }

    @Nested
    @DisplayName("multiply 相乘")
    class Multiply {

        @Test
        @DisplayName("double 重载：两个数相乘")
        void doubleTwoArgs() {
            assertEquals(6.0, NumberUtil.multiply(2.0, 3.0), DELTA, "2 * 3 应等于 6.0");
            assertEquals(0.0, NumberUtil.multiply(0.0, 5.0), DELTA, "0 乘任意数应为 0.0");
            assertEquals(0.0, NumberUtil.multiply(0.0, 0.0, 9.0), DELTA, "含 0 的连乘应为 0.0");
            assertEquals(-6.0, NumberUtil.multiply(2.0, -3.0), DELTA, "结果符号应正确");
        }

        @Test
        @DisplayName("double 重载：连乘多个数")
        void doubleMoreArgs() {
            assertEquals(24.0, NumberUtil.multiply(2.0, 3.0, 4.0), DELTA, "2*3*4 应等于 24.0");
            assertEquals(0.02, NumberUtil.multiply(0.1, 0.2), 0.0,
                    "BigDecimal 运算下 0.1 * 0.2 应精确等于 0.02");
        }

        @Test
        @DisplayName("double 重载：可变参数为空数组")
        void doubleEmptyVarargs() {
            assertEquals(6.0, NumberUtil.multiply(2.0, 3.0, new double[0]), DELTA, "空数组可变参数应被忽略");
        }

        @Test
        @DisplayName("double 重载：可变参数为 null 时按空数组处理")
        void doubleNullVarargs() {
            double result = assertDoesNotThrow(() -> NumberUtil.multiply(2.0, 3.0, (double[]) null),
                    "可变参数显式传 null 时应按空数组兜底，不应抛出 NullPointerException");
            assertEquals(6.0, result, DELTA, "multiply(2.0, 3.0, null) 应等价于 multiply(2.0, 3.0)，结果为 6.0");
            assertEquals(NumberUtil.multiply(2.0, 3.0, new double[0]), result, DELTA,
                    "传 null 与传空数组的结果应完全一致");
        }

        @Test
        @DisplayName("double 重载：NaN / 无穷大会抛 NumberFormatException")
        void doubleSpecialValue() {
            assertThrows(NumberFormatException.class, () -> NumberUtil.add(Double.NaN, 1.0),
                    "BigDecimal.valueOf 无法处理 NaN，应抛 NumberFormatException");
            assertThrows(NumberFormatException.class, () -> NumberUtil.multiply(Double.POSITIVE_INFINITY, 2.0),
                    "BigDecimal.valueOf 无法处理正无穷，应抛 NumberFormatException");
        }

        @Test
        @DisplayName("long 重载：两个数相乘")
        void longTwoArgs() {
            assertEquals(6L, NumberUtil.multiply(2L, 3L), "2 * 3 应等于 6");
            assertEquals(0L, NumberUtil.multiply(0L, 5L), "0 乘任意数应为 0");
            assertEquals(-6L, NumberUtil.multiply(2L, -3L), "结果符号应正确");
        }

        @Test
        @DisplayName("long 重载：连乘多个数")
        void longMoreArgs() {
            assertEquals(24L, NumberUtil.multiply(2L, 3L, 4L), "2*3*4 应等于 24");
            assertEquals(120L, NumberUtil.multiply(2L, 3L, 4L, 5L), "2*3*4*5 应等于 120");
        }

        @Test
        @DisplayName("long 重载：可变参数为空数组")
        void longEmptyVarargs() {
            assertEquals(6L, NumberUtil.multiply(2L, 3L, new long[0]), "空数组可变参数应被忽略");
        }

        @Test
        @DisplayName("long 重载：可变参数为 null 时按空数组处理")
        void longNullVarargs() {
            long result = assertDoesNotThrow(() -> NumberUtil.multiply(2L, 3L, (long[]) null),
                    "可变参数显式传 null 时应按空数组兜底，不应抛出 NullPointerException");
            assertEquals(6L, result, "multiply(2L, 3L, null) 应等价于 multiply(2L, 3L)，结果为 6");
            assertEquals(NumberUtil.multiply(2L, 3L, new long[0]), result,
                    "传 null 与传空数组的结果应完全一致");
        }

        @Test
        @DisplayName("long 重载：溢出时按 64 位截断")
        void longOverflowTruncated() {
            // BigInteger.valueOf(Long.MAX_VALUE) * 4 = 2^65 - 4，取低 64 位后按有符号解释为 -4
            assertEquals(-4L, NumberUtil.multiply(Long.MAX_VALUE, 4L),
                    "乘积超出 long 范围时 longValue() 只保留低 64 位，结果为 -4（源码无溢出保护）");
        }
    }

    @Nested
    @DisplayName("divide 相除")
    class Divide {

        @Test
        @DisplayName("(double, double)：默认保留 8 位，HALF_UP")
        void doubleDefault() {
            assertEquals(0.33333333, NumberUtil.divide(1.0, 3.0), DELTA,
                    "默认 scale 应为 8，且第三位 3 被 HALF_UP 舍掉");
            assertEquals(2.5, NumberUtil.divide(5.0, 2.0), DELTA, "整除结果应精确");
            assertEquals(0.0, NumberUtil.divide(0.0, 3.0), DELTA, "0 除以非 0 应为 0.0");
        }

        @Test
        @DisplayName("(double, double, int)：自定义保留位数")
        void doubleWithScale() {
            assertEquals(0.33, NumberUtil.divide(1.0, 3.0, 2), DELTA, "保留 2 位应为 0.33");
            assertEquals(0.3333, NumberUtil.divide(1.0, 3.0, 4), DELTA, "保留 4 位应为 0.3333");
            assertEquals(2.50, NumberUtil.divide(10.0, 4.0, 2), DELTA, "scale 为 2 时按该精度取值");
        }

        @Test
        @DisplayName("(double, double, int, RoundingMode)：自定义舍弃方式")
        void doubleWithScaleAndMode() {
            assertEquals(3.0, NumberUtil.divide(7.0, 2.0, 0, RoundingMode.FLOOR), DELTA,
                    "3.5 向下取整（向 0 截断）应为 3.0");
            assertEquals(4.0, NumberUtil.divide(7.0, 2.0, 0, RoundingMode.HALF_UP), DELTA,
                    "3.5 四舍五入应为 4.0");
            assertEquals(4.0, NumberUtil.divide(7.0, 2.0, 0, RoundingMode.CEILING), DELTA,
                    "3.5 向上取整（背离 0 截断）应为 4.0");
            assertEquals(-4.0, NumberUtil.divide(-7.0, 2.0, 0, RoundingMode.HALF_UP), DELTA,
                    "-3.5 按 HALF_UP 为 -4.0（逢 5 进位，背离 0）");
        }

        @Test
        @DisplayName("(long, long)：默认保留 8 位，HALF_UP")
        void longDefault() {
            assertEquals(0.33333333, NumberUtil.divide(1L, 3L), DELTA, "long 版默认 scale 同为 8");
            assertEquals(2.5, NumberUtil.divide(5L, 2L), DELTA, "long 版整除应精确");
        }

        @Test
        @DisplayName("(long, long, int)：自定义保留位数")
        void longWithScale() {
            assertEquals(0.33, NumberUtil.divide(1L, 3L, 2), DELTA, "保留 2 位应为 0.33");
            assertEquals(4.0, NumberUtil.divide(7L, 2L, 0), DELTA, "3.5 按默认 HALF_UP 取整应为 4.0");
        }

        @Test
        @DisplayName("(long, long, int, RoundingMode)：自定义舍弃方式")
        void longWithScaleAndMode() {
            assertEquals(3.0, NumberUtil.divide(7L, 2L, 0, RoundingMode.FLOOR), DELTA,
                    "3.5 向 0 截断应为 3.0");
            assertEquals(4.0, NumberUtil.divide(7L, 2L, 0, RoundingMode.HALF_UP), DELTA,
                    "3.5 四舍五入应为 4.0");
        }

        @Test
        @DisplayName("除数为 0 抛 ServiceException")
        void zeroDivisor() {
            ServiceException e1 = assertThrows(ServiceException.class, () -> NumberUtil.divide(1.0, 0.0),
                    "double 除数为 0.0 应抛出 ServiceException");
            assertEquals("除数不能为0", e1.getMessage(), "除零异常消息应与源码一致");
            assertEquals(Json.SERVICE_ERROR, e1.getCode(), "除零异常应携带默认业务错误码 500");

            ServiceException e2 = assertThrows(ServiceException.class, () -> NumberUtil.divide(1.0, 0.0, 2),
                    "带 scale 的重载除数为 0 同样应抛 ServiceException");
            assertEquals("除数不能为0", e2.getMessage(), "异常消息应一致");

            ServiceException e3 = assertThrows(ServiceException.class,
                    () -> NumberUtil.divide(1.0, 0.0, 2, RoundingMode.FLOOR),
                    "带舍入模式的重载除数为 0 同样应抛 ServiceException");
            assertEquals("除数不能为0", e3.getMessage(), "异常消息应一致");

            ServiceException e4 = assertThrows(ServiceException.class, () -> NumberUtil.divide(1L, 0L),
                    "long 重载除数为 0 同样应抛 ServiceException");
            assertEquals("除数不能为0", e4.getMessage(), "异常消息应一致");

            ServiceException e5 = assertThrows(ServiceException.class, () -> NumberUtil.divide(1L, 0L, 2),
                    "long + scale 重载除数为 0 同样应抛 ServiceException");
            assertEquals("除数不能为0", e5.getMessage(), "异常消息应一致");

            ServiceException e6 = assertThrows(ServiceException.class,
                    () -> NumberUtil.divide(1L, 0L, 2, RoundingMode.FLOOR),
                    "long + scale + 舍入模式重载除数为 0 同样应抛 ServiceException");
            assertEquals("除数不能为0", e6.getMessage(), "异常消息应一致");
        }

        @Test
        @DisplayName("除数为 0.0（分子也是 0）与 -0.0 也按除零处理")
        void zeroDivisorVariants() {
            assertThrows(ServiceException.class, () -> NumberUtil.divide(0.0, 0.0),
                    "0.0 / 0.0 应按除零处理");
            assertThrows(ServiceException.class, () -> NumberUtil.divide(1.0, -0.0),
                    "-0.0 与 0.0 在 compareTo 下相等，应按除零处理");
        }

        @Test
        @DisplayName("除数为负数可正常计算")
        void negativeDivisor() {
            assertEquals(-0.5, NumberUtil.divide(1.0, -2.0), DELTA, "1 / -2 应为 -0.5");
            assertEquals(-0.5, NumberUtil.divide(1.0, -2.0, 2), DELTA, "负除数下结果同样为负");
            assertEquals(-0.5, NumberUtil.divide(1L, -2L), DELTA, "long 版负除数同样可算");
            assertEquals(0.5, NumberUtil.divide(-1.0, -2.0, 2), DELTA, "负数除以负数结果为正");
        }

        @Test
        @DisplayName("极小但非零的除数不会被误判为 0")
        void tinyNonZeroDivisor() {
            assertEquals(1.0E9, NumberUtil.divide(1.0, 0.000000001), DELTA,
                    "1e-9 的 scale 不同于 0，compareTo 不为 0，不应按除零处理");
        }
    }

    @Nested
    @DisplayName("round 保留固定位数")
    class Round {

        @Test
        @DisplayName("HALF_UP 四舍五入")
        void halfUp() {
            BigDecimal value = NumberUtil.round(1.005, 2, RoundingMode.HALF_UP);
            assertEquals(0, value.compareTo(new BigDecimal("1.01")), "1.005 按 HALF_UP 保留 2 位应为 1.01");
            assertEquals(2, value.scale(), "结果的 scale 应等于入参 scale");
            assertEquals(0, NumberUtil.round(1.0049, 2, RoundingMode.HALF_UP)
                    .compareTo(new BigDecimal("1.00")), "1.0049 保留 2 位应为 1.00");
            assertEquals(0, NumberUtil.round(-1.235, 2, RoundingMode.HALF_UP)
                    .compareTo(new BigDecimal("-1.24")), "-1.235 保留 2 位应为 -1.24");
        }

        @Test
        @DisplayName("scale 为 0 时四舍五入为整数")
        void zeroScale() {
            assertEquals(0, NumberUtil.round(2.5, 0, RoundingMode.HALF_UP).compareTo(BigDecimal.valueOf(3)),
                    "2.5 按 HALF_UP 取整应为 3");
            assertEquals(0, NumberUtil.round(2.4, 0, RoundingMode.HALF_UP).compareTo(BigDecimal.valueOf(2)),
                    "2.4 按 HALF_UP 取整应为 2");
        }

        @Test
        @DisplayName("负 scale 按 0 处理")
        void negativeScaleTreatedAsZero() {
            BigDecimal value = NumberUtil.round(1.5, -1, RoundingMode.HALF_UP);
            assertEquals(0, value.compareTo(BigDecimal.valueOf(2)), "scale 为 -1 时被拉回为 0，1.5 → 2");
            assertEquals(0, value.scale(), "负 scale 被按 0 处理，结果 scale 应为 0");
            assertEquals(0, NumberUtil.round(1.4, -5, RoundingMode.HALF_UP).compareTo(BigDecimal.valueOf(1)),
                    "scale 为 -5 时同样按 0 处理，1.4 → 1");
        }

        @Test
        @DisplayName("0 值会补齐到指定 scale")
        void zeroKeepsScale() {
            BigDecimal value = NumberUtil.round(0.0, 2, RoundingMode.HALF_UP);
            assertNotNull(value, "返回值不应为 null");
            assertEquals(0, NumberUtil.round(0.0, 2, RoundingMode.HALF_UP).compareTo(BigDecimal.ZERO),
                    "数值应等于 0");
            assertEquals(2, NumberUtil.round(0.0, 2, RoundingMode.HALF_UP).scale(), "0.00 的 scale 应为 2");
        }

        @Test
        @DisplayName("NaN 与无穷大抛 NumberFormatException")
        void specialValue() {
            assertThrows(NumberFormatException.class, () -> NumberUtil.round(Double.NaN, 2, RoundingMode.HALF_UP),
                    "BigDecimal.valueOf 无法处理 NaN，应抛 NumberFormatException");
            assertThrows(NumberFormatException.class,
                    () -> NumberUtil.round(Double.POSITIVE_INFINITY, 2, RoundingMode.HALF_UP),
                    "BigDecimal.valueOf 无法处理正无穷，应抛 NumberFormatException");
        }
    }

    @Nested
    @DisplayName("floor / ceil 向下 / 向上省略")
    class FloorAndCeil {

        @Test
        @DisplayName("floor 等价于 RoundingMode.DOWN（向 0 截断）")
        void floor() {
            assertEquals(0, NumberUtil.floor(1.9, 0).compareTo(BigDecimal.valueOf(1)), "floor(1.9) 应为 1");
            assertEquals(0, NumberUtil.floor(1.0, 0).compareTo(BigDecimal.valueOf(1)), "floor(1.0) 应为 1");
            assertEquals(0, NumberUtil.floor(1.234, 2).compareTo(new BigDecimal("1.23")), "floor 保留 2 位应为 1.23");
            // 注意：RoundingMode.DOWN 是向 0 截断而非向 -∞ 取整，负数结果为 -1 而不是 -2
            assertEquals(0, NumberUtil.floor(-1.1, 0).compareTo(BigDecimal.valueOf(-1)),
                    "floor(-1.1) 按 DOWN 为 -1（向 0 截断，非向下取整）");
        }

        @Test
        @DisplayName("ceil 等价于 RoundingMode.UP（背离 0 截断）")
        void ceil() {
            assertEquals(0, NumberUtil.ceil(1.1, 0).compareTo(BigDecimal.valueOf(2)), "ceil(1.1) 应为 2");
            assertEquals(0, NumberUtil.ceil(1.0, 0).compareTo(BigDecimal.valueOf(1)), "ceil(1.0) 应为 1");
            assertEquals(0, NumberUtil.ceil(1.236, 2).compareTo(new BigDecimal("1.24")), "ceil 保留 2 位应为 1.24");
            assertEquals(0, NumberUtil.ceil(-1.9, 0).compareTo(BigDecimal.valueOf(-2)),
                    "ceil(-1.9) 按 UP 为 -2（背离 0 截断）");
        }

        @Test
        @DisplayName("floor / ceil 的负 scale 同样按 0 处理")
        void negativeScale() {
            assertEquals(0, NumberUtil.floor(1.9, -1).scale(), "负 scale 被拉回为 0");
            assertEquals(0, NumberUtil.ceil(1.1, -1).scale(), "负 scale 被拉回为 0");
        }
    }
}
