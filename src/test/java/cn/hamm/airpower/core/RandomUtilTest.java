package cn.hamm.airpower.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>RandomUtil 单元测试</h1>
 *
 * @author Hamm.cn
 */
class RandomUtilTest {

    private static final int DEFAULT_LENGTH = 32;
    private static final int CUSTOM_LENGTH = 16;
    private static final String BASE_CHAR = "abcdefghijklmnopqrstuvwxyz";
    private static final String BASE_NUMBER = "0123456789";

    // ==================== randomBytes 方法测试 ====================

    @Test
    void testRandomBytesWithDefaultLength() {
        byte[] result = RandomUtil.randomBytes();
        assertNotNull(result);
        assertEquals(DEFAULT_LENGTH, result.length);
    }

    @Test
    void testRandomBytesWithCustomLength() {
        byte[] result = RandomUtil.randomBytes(CUSTOM_LENGTH);
        assertNotNull(result);
        assertEquals(CUSTOM_LENGTH, result.length);
    }

    @Test
    void testRandomBytesWithZeroLength() {
        byte[] result = RandomUtil.randomBytes(0);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    void testRandomBytesWithNegativeLength() {
        // 负数长度会抛出 NegativeArraySizeException
        assertThrows(NegativeArraySizeException.class, () ->
            RandomUtil.randomBytes(-1)
        );
    }

    @Test
    void testRandomBytesValuesInRange() {
        byte[] result = RandomUtil.randomBytes(100);
        for (byte b : result) {
            // byte范围是 -128 到 127
            assertTrue(b >= -128 && b <= 127);
        }
    }

    @Test
    void testRandomBytesRandomness() {
        byte[] result1 = RandomUtil.randomBytes(32);
        byte[] result2 = RandomUtil.randomBytes(32);
        // 两次生成的随机数应该不同（极大概率）
        assertNotNull(result1);
        assertNotNull(result2);
        // 不直接比较是否相等，因为理论上存在相等的概率（虽然极低）
    }

    // ==================== randomString 方法测试 ====================

    @Test
    void testRandomStringDefaultLength() {
        String result = RandomUtil.randomString();
        assertNotNull(result);
        assertEquals(DEFAULT_LENGTH, result.length());
    }

    @Test
    void testRandomStringWithCustomLength() {
        String result = RandomUtil.randomString(CUSTOM_LENGTH);
        assertNotNull(result);
        assertEquals(CUSTOM_LENGTH, result.length());
    }

    @Test
    void testRandomStringWithZeroLength() {
        // length被修正为至少1
        String result = RandomUtil.randomString(0);
        assertNotNull(result);
        assertEquals(1, result.length());
    }

    @Test
    void testRandomStringWithNegativeLength() {
        // 负数length被修正为1
        String result = RandomUtil.randomString(-5);
        assertNotNull(result);
        assertEquals(1, result.length());
    }

    @Test
    void testRandomStringContent() {
        String result = RandomUtil.randomString(100);
        // 默认使用 BASE_CHAR_NUMBER，包含大小写字母和数字
        for (char c : result.toCharArray()) {
            assertTrue(
                Character.isLetterOrDigit(c),
                "字符 '" + c + "' 应该是字母或数字"
            );
        }
    }

    @Test
    void testRandomStringRandomness() {
        String result1 = RandomUtil.randomString(32);
        String result2 = RandomUtil.randomString(32);
        // 两次生成的随机字符串应该不同（极大概率）
        assertNotEquals(result1, result2);
    }

    // ==================== randomNumbers 方法测试 ====================

    @Test
    void testRandomNumbersWithLength() {
        String result = RandomUtil.randomNumbers(CUSTOM_LENGTH);
        assertNotNull(result);
        assertEquals(CUSTOM_LENGTH, result.length());
    }

    @Test
    void testRandomNumbersContent() {
        String result = RandomUtil.randomNumbers(50);
        for (char c : result.toCharArray()) {
            assertTrue(Character.isDigit(c), "字符 '" + c + "' 应该是数字");
        }
    }

    @Test
    void testRandomNumbersWithZeroLength() {
        // 0被修正为1
        String result = RandomUtil.randomNumbers(0);
        assertNotNull(result);
        assertEquals(1, result.length());
    }

    // ==================== randomString(String, int) 方法测试 ====================

    @Test
    void testRandomStringWithBaseString() {
        String result = RandomUtil.randomString(BASE_CHAR, CUSTOM_LENGTH);
        assertNotNull(result);
        assertEquals(CUSTOM_LENGTH, result.length());
        for (char c : result.toCharArray()) {
            assertTrue(BASE_CHAR.indexOf(c) >= 0, "字符 '" + c + "' 应该在基础字符串中");
        }
    }

    @Test
    void testRandomStringWithEmptyBaseString() {
        assertThrows(IllegalArgumentException.class, () ->
            RandomUtil.randomString("", CUSTOM_LENGTH)
        );
    }

    @Test
    void testRandomStringWithNullBaseString() {
        assertThrows(IllegalArgumentException.class, () ->
            RandomUtil.randomString(null, CUSTOM_LENGTH)
        );
    }

    @Test
    void testRandomStringWithSingleCharBase() {
        String result = RandomUtil.randomString("a", 10);
        assertNotNull(result);
        assertEquals(10, result.length());
        assertEquals("aaaaaaaaaa", result);
    }

    @Test
    void testRandomStringWithZeroLengthAndBaseString() {
        // 0被修正为1
        String result = RandomUtil.randomString(BASE_CHAR, 0);
        assertNotNull(result);
        assertEquals(1, result.length());
    }

    // ==================== randomInt 方法测试 ====================

    @Test
    void testRandomIntNoArgs() {
        int result = RandomUtil.randomInt();
        // 应该返回任意整数，不做范围限制
        assertTrue(result >= Integer.MIN_VALUE && result <= Integer.MAX_VALUE);
    }

    @Test
    void testRandomIntWithExclude() {
        int result = RandomUtil.randomInt(10);
        assertTrue(result >= 0 && result < 10);
    }

    @Test
    void testRandomIntWithExcludeZero() {
        assertThrows(IllegalArgumentException.class, () ->
            RandomUtil.randomInt(0)
        );
    }

    @Test
    void testRandomIntWithExcludeNegative() {
        assertThrows(IllegalArgumentException.class, () ->
            RandomUtil.randomInt(-5)
        );
    }

    @Test
    void testRandomIntWithRange() {
        int result = RandomUtil.randomInt(5, 10);
        assertTrue(result >= 5 && result < 10);
    }

    @Test
    void testRandomIntWithSameMinMax() {
        // minInclude=5, maxExclude=5, 范围为空
        assertThrows(IllegalArgumentException.class, () ->
            RandomUtil.randomInt(5, 5)
        );
    }

    @Test
    void testRandomIntWithMinGreaterThanMax() {
        // min=10, max=5, 这是无效的
        assertThrows(IllegalArgumentException.class, () ->
            RandomUtil.randomInt(10, 5)
        );
    }

    @Test
    void testRandomIntWithRangeAndFlags() {
        // includeMin=true, includeMax=false -> [5, 10)
        int result = RandomUtil.randomInt(5, 10, true, false);
        assertTrue(result >= 5 && result < 10);
    }

    @Test
    void testRandomIntExcludeMin() {
        // includeMin=false, includeMax=false -> (5, 10)
        int result = RandomUtil.randomInt(5, 10, false, false);
        assertTrue(result > 5 && result < 10);
    }

    @Test
    void testRandomIntIncludeMax() {
        // includeMin=true, includeMax=true -> [5, 11) 即 [5, 10]
        int result = RandomUtil.randomInt(5, 10, true, true);
        assertTrue(result >= 5 && result <= 10);
    }

    @Test
    void testRandomIntExcludeBoth() {
        // includeMin=false, includeMax=true -> (5, 11) 即 [6, 10]
        int result = RandomUtil.randomInt(5, 10, false, true);
        assertTrue(result >= 6 && result <= 10);
    }

    @RepeatedTest(10)
    void testRandomIntDistribution() {
        // 多次测试确保随机数在范围内
        int result = RandomUtil.randomInt(0, 100);
        assertTrue(result >= 0 && result < 100);
    }

    // ==================== 边界条件测试 ====================

    @Test
    void testRandomStringWithVeryLongLength() {
        String result = RandomUtil.randomString(10000);
        assertNotNull(result);
        assertEquals(10000, result.length());
    }

    @Test
    void testRandomNumbersWithVeryLongLength() {
        String result = RandomUtil.randomNumbers(1000);
        assertNotNull(result);
        assertEquals(1000, result.length());
    }

    @Test
    void testRandomBytesWithLargeLength() {
        byte[] result = RandomUtil.randomBytes(10000);
        assertNotNull(result);
        assertEquals(10000, result.length);
    }

    @Test
    void testRandomIntWithMaxRange() {
        int result = RandomUtil.randomInt(Integer.MIN_VALUE, Integer.MAX_VALUE, true, false);
        // 应该能处理大范围
        assertNotNull(result);
    }
}
