package cn.hamm.airpower.core;

import cn.hamm.airpower.core.exception.ServiceException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>NumberUtil 单元测试</h1>
 *
 * @author Hamm.cn
 */
class NumberUtilTest {

    private static final double DELTA = 0.00000001;

    // ==================== add 方法测试 ====================

    @Test
    void testAddWithTwoDoubles() {
        double result = NumberUtil.add(1.5, 2.5);
        assertEquals(4.0, result, DELTA);
    }

    @Test
    void testAddWithMultipleDoubles() {
        double result = NumberUtil.add(1.0, 2.0, 3.0, 4.0);
        assertEquals(10.0, result, DELTA);
    }

    @Test
    void testAddWithNegativeNumbers() {
        double result = NumberUtil.add(-1.0, 2.0);
        assertEquals(1.0, result, DELTA);
    }

    @Test
    void testAddWithTwoLongs() {
        long result = NumberUtil.add(10L, 20L);
        assertEquals(30L, result);
    }

    @Test
    void testAddWithMultipleLongs() {
        long result = NumberUtil.add(1L, 2L, 3L, 4L);
        assertEquals(10L, result);
    }

    @Test
    void testAddWithZero() {
        double result = NumberUtil.add(0.0, 5.0);
        assertEquals(5.0, result, DELTA);
    }

    // ==================== subtract 方法测试 ====================

    @Test
    void testSubtractWithTwoDoubles() {
        double result = NumberUtil.subtract(5.0, 3.0);
        assertEquals(2.0, result, DELTA);
    }

    @Test
    void testSubtractWithMultipleDoubles() {
        double result = NumberUtil.subtract(10.0, 2.0, 3.0);
        assertEquals(5.0, result, DELTA);
    }

    @Test
    void testSubtractWithNegativeResult() {
        double result = NumberUtil.subtract(3.0, 5.0);
        assertEquals(-2.0, result, DELTA);
    }

    @Test
    void testSubtractWithTwoLongs() {
        long result = NumberUtil.subtract(10L, 3L);
        assertEquals(7L, result);
    }

    @Test
    void testSubtractWithMultipleLongs() {
        long result = NumberUtil.subtract(100L, 10L, 20L);
        assertEquals(70L, result);
    }

    // ==================== multiply 方法测试 ====================

    @Test
    void testMultiplyWithTwoDoubles() {
        double result = NumberUtil.multiply(2.0, 3.0);
        assertEquals(6.0, result, DELTA);
    }

    @Test
    void testMultiplyWithMultipleDoubles() {
        double result = NumberUtil.multiply(2.0, 3.0, 4.0);
        assertEquals(24.0, result, DELTA);
    }

    @Test
    void testMultiplyWithNegativeNumbers() {
        double result = NumberUtil.multiply(-2.0, 3.0);
        assertEquals(-6.0, result, DELTA);
    }

    @Test
    void testMultiplyWithTwoLongs() {
        long result = NumberUtil.multiply(4L, 5L);
        assertEquals(20L, result);
    }

    @Test
    void testMultiplyWithMultipleLongs() {
        long result = NumberUtil.multiply(2L, 3L, 4L);
        assertEquals(24L, result);
    }

    @Test
    void testMultiplyWithZero() {
        double result = NumberUtil.multiply(5.0, 0.0);
        assertEquals(0.0, result, DELTA);
    }

    // ==================== divide 方法测试 ====================

    @Test
    void testDivideWithTwoDoubles() {
        double result = NumberUtil.divide(6.0, 3.0);
        assertEquals(2.0, result, DELTA);
    }

    @Test
    void testDivideWithScale() {
        double result = NumberUtil.divide(10.0, 3.0, 2);
        assertEquals(3.33, result, DELTA);
    }

    @Test
    void testDivideWithRoundingMode() {
        double result = NumberUtil.divide(10.0, 3.0, 2, RoundingMode.UP);
        assertEquals(3.34, result, DELTA);
    }

    @Test
    void testDivideWithTwoLongs() {
        double result = NumberUtil.divide(10L, 3L);
        assertEquals(3.33333333, result, DELTA);
    }

    @Test
    void testDivideWithLongScale() {
        double result = NumberUtil.divide(10L, 3L, 2);
        assertEquals(3.33, result, DELTA);
    }

    @Test
    void testDivideWithLongRoundingMode() {
        double result = NumberUtil.divide(10L, 3L, 2, RoundingMode.UP);
        assertEquals(3.34, result, DELTA);
    }

    @Test
    void testDivideByZero() {
        assertThrows(ServiceException.class, () ->
            NumberUtil.divide(10.0, 0.0)
        );
    }

    @Test
    void testDivideLongByZero() {
        assertThrows(ServiceException.class, () ->
            NumberUtil.divide(10L, 0L)
        );
    }

    @Test
    void testDivideWithNegativeNumbers() {
        double result = NumberUtil.divide(-10.0, 2.0);
        assertEquals(-5.0, result, DELTA);
    }

    // ==================== floor 方法测试 ====================

    @Test
    void testFloorWithPositiveNumber() {
        BigDecimal result = NumberUtil.floor(3.7, 0);
        assertEquals(BigDecimal.valueOf(3), result);
    }

    @Test
    void testFloorWithNegativeNumber() {
        // floor(-3.7) 使用 RoundingMode.DOWN，对于负数是向零取整，结果是 -3
        BigDecimal result = NumberUtil.floor(-3.7, 0);
        assertEquals(BigDecimal.valueOf(-3), result);
    }

    @Test
    void testFloorWithScale() {
        BigDecimal result = NumberUtil.floor(3.789, 2);
        assertEquals(BigDecimal.valueOf(3.78), result);
    }

    // ==================== ceil 方法测试 ====================

    @Test
    void testCeilWithPositiveNumber() {
        BigDecimal result = NumberUtil.ceil(3.2, 0);
        assertEquals(BigDecimal.valueOf(4), result);
    }

    @Test
    void testCeilWithNegativeNumber() {
        // ceil(-3.2) 使用 RoundingMode.UP，对于负数是远离零取整，结果是 -4
        BigDecimal result = NumberUtil.ceil(-3.2, 0);
        assertEquals(BigDecimal.valueOf(-4), result);
    }

    @Test
    void testCeilWithScale() {
        BigDecimal result = NumberUtil.ceil(3.211, 2);
        assertEquals(BigDecimal.valueOf(3.22), result);
    }

    // ==================== round 方法测试 ====================

    @Test
    void testRoundWithHalfUp() {
        BigDecimal result = NumberUtil.round(3.5, 0, RoundingMode.HALF_UP);
        assertEquals(BigDecimal.valueOf(4), result);
    }

    @Test
    void testRoundWithHalfDown() {
        BigDecimal result = NumberUtil.round(3.5, 0, RoundingMode.HALF_DOWN);
        assertEquals(BigDecimal.valueOf(3), result);
    }

    @Test
    void testRoundWithNegativeScale() {
        // 负数scale应该被修正为0
        BigDecimal result = NumberUtil.round(3.7, -1, RoundingMode.HALF_UP);
        assertEquals(BigDecimal.valueOf(4), result);
    }

    @Test
    void testRoundWithDecimalPlaces() {
        BigDecimal result = NumberUtil.round(3.14159, 2, RoundingMode.HALF_UP);
        assertEquals(BigDecimal.valueOf(3.14), result);
    }

    // ==================== 边界条件测试 ====================

    @Test
    void testAddWithMaxDouble() {
        // 测试极大值相加
        double result = NumberUtil.add(Double.MAX_VALUE, 0.0);
        assertEquals(Double.MAX_VALUE, result, DELTA);
    }

    @Test
    void testMultiplyWithMaxDouble() {
        // 测试极大值相乘（会溢出）
        double result = NumberUtil.multiply(Double.MAX_VALUE, 2.0);
        assertTrue(Double.isInfinite(result));
    }

    @Test
    void testDivideWithVerySmallNumber() {
        // 测试极小值相除
        double result = NumberUtil.divide(1.0, Double.MIN_VALUE);
        assertTrue(Double.isInfinite(result));
    }

    @Test
    void testSubtractWithSameNumber() {
        double result = NumberUtil.subtract(5.0, 5.0);
        assertEquals(0.0, result, DELTA);
    }

    @Test
    void testMultiplyWithOne() {
        double result = NumberUtil.multiply(5.0, 1.0);
        assertEquals(5.0, result, DELTA);
    }

    @Test
    void testDivideWithOne() {
        double result = NumberUtil.divide(5.0, 1.0);
        assertEquals(5.0, result, DELTA);
    }

    @Test
    void testFloorWithInteger() {
        BigDecimal result = NumberUtil.floor(5.0, 0);
        assertEquals(BigDecimal.valueOf(5), result);
    }

    @Test
    void testCeilWithInteger() {
        BigDecimal result = NumberUtil.ceil(5.0, 0);
        assertEquals(BigDecimal.valueOf(5), result);
    }
}
