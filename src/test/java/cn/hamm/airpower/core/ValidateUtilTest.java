package cn.hamm.airpower.core;

import cn.hamm.airpower.core.exception.ServiceException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>ValidateUtil 单元测试</h1>
 *
 * @author Hamm.cn
 */
class ValidateUtilTest {

    // ==================== isNumber 方法测试 ====================

    @Test
    void testIsNumberWithValidInteger() {
        assertTrue(ValidateUtil.isNumber("123"));
    }

    @Test
    void testIsNumberWithValidDecimal() {
        assertTrue(ValidateUtil.isNumber("123.45"));
    }

    @Test
    void testIsNumberWithNegativeNumber() {
        assertTrue(ValidateUtil.isNumber("-123.45"));
    }

    @Test
    void testIsNumberWithInvalidString() {
        assertFalse(ValidateUtil.isNumber("abc"));
    }

    @Test
    void testIsNumberWithEmptyString() {
        assertFalse(ValidateUtil.isNumber(""));
    }

    @Test
    void testIsNumberWithNull() {
        assertThrows(NullPointerException.class, () -> ValidateUtil.isNumber(null));
    }

    @Test
    void testIsNumberWithMultipleDots() {
        assertFalse(ValidateUtil.isNumber("123.45.67"));
    }

    @Test
    void testIsNumberWithLeadingDot() {
        assertFalse(ValidateUtil.isNumber(".123"));
    }

    // ==================== isInteger 方法测试 ====================

    @Test
    void testIsIntegerWithValidInteger() {
        assertTrue(ValidateUtil.isInteger("123"));
    }

    @Test
    void testIsIntegerWithNegativeInteger() {
        assertTrue(ValidateUtil.isInteger("-123"));
    }

    @Test
    void testIsIntegerWithDecimal() {
        assertFalse(ValidateUtil.isInteger("123.45"));
    }

    @Test
    void testIsIntegerWithInvalidString() {
        assertFalse(ValidateUtil.isInteger("abc"));
    }

    @Test
    void testIsIntegerWithNull() {
        assertThrows(NullPointerException.class, () -> ValidateUtil.isInteger(null));
    }

    // ==================== isEmail 方法测试 ====================

    @Test
    void testIsEmailWithValidEmail() {
        assertTrue(ValidateUtil.isEmail("test@example.com"));
    }

    @Test
    void testIsEmailWithValidEmailWithDot() {
        assertTrue(ValidateUtil.isEmail("user.name@example.com"));
    }

    @Test
    void testIsEmailWithValidEmailWithPlus() {
        assertTrue(ValidateUtil.isEmail("user+tag@example.com"));
    }

    @Test
    void testIsEmailWithoutAtSymbol() {
        assertFalse(ValidateUtil.isEmail("testexample.com"));
    }

    @Test
    void testIsEmailWithoutDomain() {
        assertFalse(ValidateUtil.isEmail("test@"));
    }

    @Test
    void testIsEmailWithInvalidDomain() {
        assertFalse(ValidateUtil.isEmail("test@.com"));
    }

    @Test
    void testIsEmailWithNull() {
        assertThrows(NullPointerException.class, () -> ValidateUtil.isEmail(null));
    }

    @Test
    void testIsEmailWithDoubleDots() {
        assertFalse(ValidateUtil.isEmail("test..name@example.com"));
    }

    // ==================== isLetter 方法测试 ====================

    @Test
    void testIsLetterWithLowercase() {
        assertTrue(ValidateUtil.isLetter("abc"));
    }

    @Test
    void testIsLetterWithUppercase() {
        assertTrue(ValidateUtil.isLetter("ABC"));
    }

    @Test
    void testIsLetterWithMixedCase() {
        assertTrue(ValidateUtil.isLetter("AbC"));
    }

    @Test
    void testIsLetterWithNumber() {
        assertFalse(ValidateUtil.isLetter("abc123"));
    }

    @Test
    void testIsLetterWithChinese() {
        assertFalse(ValidateUtil.isLetter("abc中文"));
    }

    @Test
    void testIsLetterWithNull() {
        assertThrows(NullPointerException.class, () -> ValidateUtil.isLetter(null));
    }

    // ==================== isLetterOrNumber 方法测试 ====================

    @Test
    void testIsLetterOrNumberWithLetters() {
        assertTrue(ValidateUtil.isLetterOrNumber("abc"));
    }

    @Test
    void testIsLetterOrNumberWithNumbers() {
        assertTrue(ValidateUtil.isLetterOrNumber("123"));
    }

    @Test
    void testIsLetterOrNumberWithMixed() {
        assertTrue(ValidateUtil.isLetterOrNumber("abc123"));
    }

    @Test
    void testIsLetterOrNumberWithSpecialChars() {
        assertFalse(ValidateUtil.isLetterOrNumber("abc_123"));
    }

    @Test
    void testIsLetterOrNumberWithChinese() {
        assertFalse(ValidateUtil.isLetterOrNumber("abc中文"));
    }

    @Test
    void testIsLetterOrNumberWithNull() {
        assertThrows(NullPointerException.class, () -> ValidateUtil.isLetterOrNumber(null));
    }

    // ==================== isChinese 方法测试 ====================

    @Test
    void testIsChineseWithChineseChars() {
        assertTrue(ValidateUtil.isChinese("中文汉字"));
    }

    @Test
    void testIsChineseWithEmptyString() {
        assertTrue(ValidateUtil.isChinese(""));
    }

    @Test
    void testIsChineseWithEnglish() {
        assertFalse(ValidateUtil.isChinese("hello"));
    }

    @Test
    void testIsChineseWithMixed() {
        assertFalse(ValidateUtil.isChinese("中文abc"));
    }

    @Test
    void testIsChineseWithNull() {
        assertThrows(NullPointerException.class, () -> ValidateUtil.isChinese(null));
    }

    // ==================== isMobilePhone 方法测试 ====================

    @Test
    void testIsMobilePhoneWithValidNumber() {
        assertTrue(ValidateUtil.isMobilePhone("13800138000"));
    }

    @Test
    void testIsMobilePhoneWithValidNumberStartingWith13() {
        assertTrue(ValidateUtil.isMobilePhone("13912345678"));
    }

    @Test
    void testIsMobilePhoneWithValidNumberStartingWith18() {
        assertTrue(ValidateUtil.isMobilePhone("18812345678"));
    }

    @Test
    void testIsMobilePhoneWithInternationalPrefix() {
        assertTrue(ValidateUtil.isMobilePhone("+8613800138000"));
    }

    @Test
    void testIsMobilePhoneWithInvalidPrefix() {
        assertFalse(ValidateUtil.isMobilePhone("12800138000"));
    }

    @Test
    void testIsMobilePhoneWithTooShort() {
        assertFalse(ValidateUtil.isMobilePhone("138001"));
    }

    @Test
    void testIsMobilePhoneWithTooLong() {
        assertFalse(ValidateUtil.isMobilePhone("138001380001"));
    }

    @Test
    void testIsMobilePhoneWithNull() {
        assertThrows(NullPointerException.class, () -> ValidateUtil.isMobilePhone(null));
    }

    // ==================== isTelPhone 方法测试 ====================

    @Test
    void testIsTelPhoneWithValidNumber() {
        assertTrue(ValidateUtil.isTelPhone("010-12345678"));
    }

    @Test
    void testIsTelPhoneWithValidNumberNoAreaCode() {
        assertTrue(ValidateUtil.isTelPhone("12345678"));
    }

    @Test
    void testIsTelPhoneWith400Number() {
        assertTrue(ValidateUtil.isTelPhone("4001234567"));
    }

    @Test
    void testIsTelPhoneWith800Number() {
        assertTrue(ValidateUtil.isTelPhone("8001234567"));
    }

    @Test
    void testIsTelPhoneWithExtension() {
        assertTrue(ValidateUtil.isTelPhone("010-12345678-123"));
    }

    @Test
    void testIsTelPhoneWithInvalidNumber() {
        assertFalse(ValidateUtil.isTelPhone("123"));
    }

    @Test
    void testIsTelPhoneWithNull() {
        assertThrows(NullPointerException.class, () -> ValidateUtil.isTelPhone(null));
    }

    // ==================== isNormalCode 方法测试 ====================

    @Test
    void testIsNormalCodeWithValidChars() {
        assertTrue(ValidateUtil.isNormalCode("a"));
    }

    @Test
    void testIsNormalCodeWithAtSymbol() {
        assertTrue(ValidateUtil.isNormalCode("@"));
    }

    @Test
    void testIsNormalCodeWithHash() {
        assertTrue(ValidateUtil.isNormalCode("#"));
    }

    @Test
    void testIsNormalCodeWithPercent() {
        assertTrue(ValidateUtil.isNormalCode("%"));
    }

    @Test
    void testIsNormalCodeWithUnderscore() {
        assertTrue(ValidateUtil.isNormalCode("_"));
    }

    @Test
    void testIsNormalCodeWithChinese() {
        assertTrue(ValidateUtil.isNormalCode("中"));
    }

    @Test
    void testIsNormalCodeWithInvalidChar() {
        assertFalse(ValidateUtil.isNormalCode("!"));
    }

    @Test
    void testIsNormalCodeWithMultipleChars() {
        assertFalse(ValidateUtil.isNormalCode("abc"));
    }

    @Test
    void testIsNormalCodeWithNull() {
        assertThrows(NullPointerException.class, () -> ValidateUtil.isNormalCode(null));
    }

    // ==================== isOnlyNumberAndLetter 方法测试 ====================

    @Test
    void testIsOnlyNumberAndLetterWithLetters() {
        assertTrue(ValidateUtil.isOnlyNumberAndLetter("abc"));
    }

    @Test
    void testIsOnlyNumberAndLetterWithNumbers() {
        assertTrue(ValidateUtil.isOnlyNumberAndLetter("123"));
    }

    @Test
    void testIsOnlyNumberAndLetterWithMixed() {
        assertTrue(ValidateUtil.isOnlyNumberAndLetter("abc123"));
    }

    @Test
    void testIsOnlyNumberAndLetterWithSpecialChars() {
        assertFalse(ValidateUtil.isOnlyNumberAndLetter("abc_123"));
    }

    @Test
    void testIsOnlyNumberAndLetterWithNull() {
        assertThrows(NullPointerException.class, () -> ValidateUtil.isOnlyNumberAndLetter(null));
    }

    // ==================== isNaturalNumber 方法测试 ====================

    @Test
    void testIsNaturalNumberWithValidInteger() {
        assertTrue(ValidateUtil.isNaturalNumber("123"));
    }

    @Test
    void testIsNaturalNumberWithValidDecimal() {
        assertTrue(ValidateUtil.isNaturalNumber("123.45"));
    }

    @Test
    void testIsNaturalNumberWithZero() {
        assertTrue(ValidateUtil.isNaturalNumber("0"));
    }

    @Test
    void testIsNaturalNumberWithNegative() {
        assertFalse(ValidateUtil.isNaturalNumber("-123"));
    }

    @Test
    void testIsNaturalNumberWithNull() {
        assertThrows(NullPointerException.class, () -> ValidateUtil.isNaturalNumber(null));
    }

    // ==================== isNaturalInteger 方法测试 ====================

    @Test
    void testIsNaturalIntegerWithValidInteger() {
        assertTrue(ValidateUtil.isNaturalInteger("123"));
    }

    @Test
    void testIsNaturalIntegerWithZero() {
        assertTrue(ValidateUtil.isNaturalInteger("0"));
    }

    @Test
    void testIsNaturalIntegerWithDecimal() {
        assertFalse(ValidateUtil.isNaturalInteger("123.45"));
    }

    @Test
    void testIsNaturalIntegerWithNegative() {
        assertFalse(ValidateUtil.isNaturalInteger("-123"));
    }

    @Test
    void testIsNaturalIntegerWithNull() {
        assertThrows(NullPointerException.class, () -> ValidateUtil.isNaturalInteger(null));
    }

    // ==================== isChina2Identity 方法测试 ====================

    @Test
    void testIsChina2IdentityWithValidId18() {
        // 使用一个符合校验规则的18位身份证
        // 前17位: 11010119900101123
        // 校验位计算结果: 7
        assertTrue(ValidateUtil.isChina2Identity("110101199001011237"));
    }

    @Test
    void testIsChina2IdentityWithValidId15() {
        // 15位身份证
        ServiceException exception = assertThrows(ServiceException.class,
                () -> ValidateUtil.isChina2Identity("110101900101123"));
        assertTrue(exception.getMessage().contains("暂不支持一代身份证校验"));
    }

    @Test
    void testIsChina2IdentityWithNull() {
        assertFalse(ValidateUtil.isChina2Identity(null));
    }

    @Test
    void testIsChina2IdentityWithInvalidLength() {
        assertFalse(ValidateUtil.isChina2Identity("123"));
    }

    @Test
    void testIsChina2IdentityWithInvalidCheckDigit() {
        // 18位但校验位错误
        assertFalse(ValidateUtil.isChina2Identity("110101199001011235"));
    }

    // ==================== validRegex 方法测试 ====================

    @Test
    void testValidRegexWithValidPattern() {
        assertTrue(ValidateUtil.validRegex("123", java.util.regex.Pattern.compile("^\\d+$")));
    }

    @Test
    void testValidRegexWithInvalidPattern() {
        assertFalse(ValidateUtil.validRegex("abc", java.util.regex.Pattern.compile("^\\d+$")));
    }

    @Test
    void testValidRegexWithEmptyString() {
        assertFalse(ValidateUtil.validRegex("", java.util.regex.Pattern.compile("^\\d+$")));
    }

    // ==================== valid 方法测试（Jakarta Validation） ====================

    @Test
    void testValidWithNullModel() {
        // null 模型不应该抛出异常
        assertDoesNotThrow(() -> ValidateUtil.valid(null));
    }

    /**
     * 测试用的模型类
     */
    static class TestModel extends RootModel<TestModel> {
        private String name;

        String getName() {
            return name;
        }

        void setName(String name) {
            this.name = name;
        }
    }
}
