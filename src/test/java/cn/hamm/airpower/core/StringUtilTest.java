package cn.hamm.airpower.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>StringUtil 单元测试</h1>
 *
 * @author Hamm.cn
 */
class StringUtilTest {

    // ==================== isEmpty 方法测试 ====================

    @Test
    void testIsEmptyWithNull() {
        assertTrue(StringUtil.isEmpty(null));
    }

    @Test
    void testIsEmptyWithEmptyString() {
        assertTrue(StringUtil.isEmpty(""));
    }

    @Test
    void testIsEmptyWithWhitespace() {
        assertFalse(StringUtil.isEmpty("   "));
    }

    @Test
    void testIsEmptyWithText() {
        assertFalse(StringUtil.isEmpty("hello"));
    }

    @Test
    void testIsEmptyWithCharSequenceNull() {
        assertTrue(StringUtil.isEmpty((CharSequence) null));
    }

    @Test
    void testIsEmptyWithCharSequenceEmpty() {
        assertTrue(StringUtil.isEmpty((CharSequence) ""));
    }

    @Test
    void testIsEmptyWithCharSequenceText() {
        assertFalse(StringUtil.isEmpty((CharSequence) "hello"));
    }

    // ==================== hasText 方法测试 ====================

    @Test
    void testHasTextWithNull() {
        assertFalse(StringUtil.hasText((String) null));
    }

    @Test
    void testHasTextWithEmptyString() {
        assertFalse(StringUtil.hasText(""));
    }

    @Test
    void testHasTextWithWhitespace() {
        assertFalse(StringUtil.hasText("   "));
    }

    @Test
    void testHasTextWithText() {
        assertTrue(StringUtil.hasText("hello"));
    }

    @Test
    void testHasTextWithMixedWhitespaceAndText() {
        assertTrue(StringUtil.hasText("  hello  "));
    }

    @Test
    void testHasTextWithCharSequenceNull() {
        assertFalse(StringUtil.hasText((CharSequence) null));
    }

    @Test
    void testHasTextWithCharSequenceEmpty() {
        assertFalse(StringUtil.hasText((CharSequence) ""));
    }

    @Test
    void testHasTextWithCharSequenceWhitespace() {
        assertFalse(StringUtil.hasText((CharSequence) "   "));
    }

    @Test
    void testHasTextWithCharSequenceText() {
        assertTrue(StringUtil.hasText((CharSequence) "hello"));
    }

    @Test
    void testHasTextWithTabAndNewline() {
        assertFalse(StringUtil.hasText("\t\n\r"));
    }

    // ==================== containsWhitespace 方法测试 ====================

    @Test
    void testContainsWhitespaceWithNull() {
        assertFalse(StringUtil.containsWhitespace((String) null));
    }

    @Test
    void testContainsWhitespaceWithEmptyString() {
        assertFalse(StringUtil.containsWhitespace(""));
    }

    @Test
    void testContainsWhitespaceWithNoWhitespace() {
        assertFalse(StringUtil.containsWhitespace("hello"));
    }

    @Test
    void testContainsWhitespaceWithSpace() {
        assertTrue(StringUtil.containsWhitespace("hello world"));
    }

    @Test
    void testContainsWhitespaceWithTab() {
        assertTrue(StringUtil.containsWhitespace("hello\tworld"));
    }

    @Test
    void testContainsWhitespaceWithNewline() {
        assertTrue(StringUtil.containsWhitespace("hello\nworld"));
    }

    @Test
    void testContainsWhitespaceWithOnlyWhitespace() {
        assertTrue(StringUtil.containsWhitespace("   "));
    }

    @Test
    void testContainsWhitespaceWithCharSequence() {
        assertTrue(StringUtil.containsWhitespace((CharSequence) "hello world"));
    }

    // ==================== trimAllWhitespace 方法测试 ====================

    @Test
    void testTrimAllWhitespaceWithNull() {
        assertNull(StringUtil.trimAllWhitespace(null));
    }

    @Test
    void testTrimAllWhitespaceWithEmptyString() {
        assertEquals("", StringUtil.trimAllWhitespace("").toString());
    }

    @Test
    void testTrimAllWhitespaceWithNoWhitespace() {
        assertEquals("hello", StringUtil.trimAllWhitespace("hello").toString());
    }

    @Test
    void testTrimAllWhitespaceWithSpaces() {
        assertEquals("helloworld", StringUtil.trimAllWhitespace("hello world").toString());
    }

    @Test
    void testTrimAllWhitespaceWithTabs() {
        assertEquals("helloworld", StringUtil.trimAllWhitespace("hello\tworld").toString());
    }

    @Test
    void testTrimAllWhitespaceWithNewlines() {
        assertEquals("helloworld", StringUtil.trimAllWhitespace("hello\nworld").toString());
    }

    @Test
    void testTrimAllWhitespaceWithMixedWhitespace() {
        assertEquals("helloworld", StringUtil.trimAllWhitespace(" hello \t\n world ").toString());
    }

    @Test
    void testTrimAllWhitespaceWithOnlyWhitespace() {
        assertEquals("", StringUtil.trimAllWhitespace("   ").toString());
    }

    // ==================== capitalize 方法测试 ====================

    @Test
    void testCapitalizeWithNull() {
        assertNull(StringUtil.capitalize(null));
    }

    @Test
    void testCapitalizeWithEmptyString() {
        assertEquals("", StringUtil.capitalize(""));
    }

    @Test
    void testCapitalizeWithLowercase() {
        assertEquals("Hello", StringUtil.capitalize("hello"));
    }

    @Test
    void testCapitalizeWithUppercase() {
        assertEquals("Hello", StringUtil.capitalize("Hello"));
    }

    @Test
    void testCapitalizeWithSingleChar() {
        assertEquals("H", StringUtil.capitalize("h"));
    }

    @Test
    void testCapitalizeWithNonLetter() {
        assertEquals("123abc", StringUtil.capitalize("123abc"));
    }

    @Test
    void testCapitalizeWithChinese() {
        assertEquals("中文", StringUtil.capitalize("中文"));
    }

    // ==================== uncapitalize 方法测试 ====================

    @Test
    void testUncapitalizeWithNull() {
        assertNull(StringUtil.uncapitalize(null));
    }

    @Test
    void testUncapitalizeWithEmptyString() {
        assertEquals("", StringUtil.uncapitalize(""));
    }

    @Test
    void testUncapitalizeWithUppercase() {
        assertEquals("hello", StringUtil.uncapitalize("Hello"));
    }

    @Test
    void testUncapitalizeWithLowercase() {
        assertEquals("hello", StringUtil.uncapitalize("hello"));
    }

    @Test
    void testUncapitalizeWithSingleChar() {
        assertEquals("h", StringUtil.uncapitalize("H"));
    }

    @Test
    void testUncapitalizeWithNonLetter() {
        assertEquals("123abc", StringUtil.uncapitalize("123abc"));
    }

    @Test
    void testUncapitalizeWithChinese() {
        assertEquals("中文", StringUtil.uncapitalize("中文"));
    }

    // ==================== capitalize 和 uncapitalize 互逆测试 ====================

    @Test
    void testCapitalizeAndUncapitalize() {
        String original = "helloWorld";
        assertEquals(original, StringUtil.uncapitalize(StringUtil.capitalize(original)));
    }

    @Test
    void testUncapitalizeAndCapitalize() {
        String original = "HelloWorld";
        assertEquals(original, StringUtil.capitalize(StringUtil.uncapitalize(original)));
    }
}
