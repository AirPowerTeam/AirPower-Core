package cn.hamm.airpower.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>StringUtil 单元测试</h1>
 *
 * <p>覆盖 {@code StringUtil} 的 9 个 public 方法（含同名重载），
 * 重点验证 {@code null} / 空串 / 纯空白 / 非 String 类型 {@link CharSequence} /
 * 首字符无大小写变化时返回原引用 等边界行为。</p>
 *
 * @author Hamm.cn
 */
@DisplayName("StringUtil 字符串工具类测试")
public class StringUtilTest {

    /**
     * 不间断空格 NBSP（U+00A0）：{@link Character#isWhitespace} 判定为 <b>非</b>空白
     */
    private static final String NBSP = "\u00A0";

    /**
     * 全角空格（U+3000）：判定为空白
     */
    private static final String FULL_WIDTH_SPACE = "\u3000";

    /**
     * 标题字母 Dž（U+01C6）：其大写形式为 DŽ（U+01C4），两者不相等
     */
    private static final String TITLECASE_DZ = "\u01C6";

    /**
     * 大写字母 DŽ（U+01C4）：其小写形式为 Dž（U+01C6）
     */
    private static final String UPPERCASE_DZ = "\u01C4";

    /**
     * 公共的样本集合：覆盖 null、空串、纯空白、含空白、无空白、非 ASCII
     */
    private static String[] samples() {
        return new String[]{null, "", " ", "   ", "\t\n", "a", " a ", "a b", NBSP, FULL_WIDTH_SPACE, "中文"};
    }

    @Nested
    @DisplayName("isEmpty 是否为空")
    class IsEmpty {

        @Test
        @DisplayName("CharSequence 重载：null 与空串为 true，非空串为 false")
        void charSequenceOverload() {
            assertTrue(StringUtil.isEmpty((CharSequence) null), "CharSequence 重载遇到 null 应返回 true");
            assertTrue(StringUtil.isEmpty((CharSequence) ""), "空字符串应判定为 empty");
            assertTrue(StringUtil.isEmpty(new StringBuilder("")), "空的 StringBuilder 应判定为 empty");
            assertFalse(StringUtil.isEmpty((CharSequence) " "), "仅含空格的字符串长度不为 0，不应判定为 empty");
            assertFalse(StringUtil.isEmpty((CharSequence) "a"), "非空字符串不应判定为 empty");
        }

        @Test
        @DisplayName("String 重载：null 与空串为 true，非空串为 false")
        void stringOverload() {
            assertTrue(StringUtil.isEmpty((String) null), "String 重载遇到 null 应返回 true");
            assertTrue(StringUtil.isEmpty(""), "空字符串应判定为 empty");
            assertFalse(StringUtil.isEmpty(" "), "仅含空格的字符串不应判定为 empty");
            assertFalse(StringUtil.isEmpty(NBSP), "不间断空格的 length 为 1，不应判定为 empty");
            assertFalse(StringUtil.isEmpty("中文"), "中文字符串不应判定为 empty");
        }

        @Test
        @DisplayName("两个重载对同一输入结果一致")
        void bothOverloadsSameResult() {
            String[] data = samples();
            for (int i = 0; i < data.length; i++) {
                assertEquals(StringUtil.isEmpty((CharSequence) data[i]), StringUtil.isEmpty(data[i]),
                        "样本 [" + i + "] 在两个 isEmpty 重载下结果应一致");
            }
        }
    }

    @Nested
    @DisplayName("hasText 是否包含有效字符")
    class HasText {

        @Test
        @DisplayName("CharSequence 重载：null / 空串 / 纯空白为 false，其余为 true")
        void charSequenceOverload() {
            assertFalse(StringUtil.hasText((CharSequence) null), "null 不包含有效字符");
            assertFalse(StringUtil.hasText((CharSequence) ""), "空字符串不包含有效字符");
            assertFalse(StringUtil.hasText((CharSequence) "   "), "全为空格的字符串不包含有效字符");
            assertFalse(StringUtil.hasText((CharSequence) "\t\n\r "), "全为空白字符的字符串不包含有效字符");
            assertFalse(StringUtil.hasText(new StringBuilder(FULL_WIDTH_SPACE + "\t")),
                    "全角空格与制表符均视为空白，不包含有效字符");
            assertTrue(StringUtil.hasText((CharSequence) " a "), "两端有空格但中间有字符，应包含有效字符");
            assertTrue(StringUtil.hasText((CharSequence) "中文"), "中文应视为有效字符");
            assertTrue(StringUtil.hasText((CharSequence) NBSP),
                    "不间断空格不被 Character.isWhitespace 视为空白，应算作有效字符");
        }

        @Test
        @DisplayName("String 重载：null / 空串 / 纯空白为 false，其余为 true")
        void stringOverload() {
            assertFalse(StringUtil.hasText((String) null), "null 不包含有效字符");
            assertFalse(StringUtil.hasText(""), "空字符串不包含有效字符");
            assertFalse(StringUtil.hasText("   "), "全为空格的字符串不包含有效字符");
            assertFalse(StringUtil.hasText("\t\n\r "), "全为空白字符的字符串不包含有效字符");
            assertFalse(StringUtil.hasText(FULL_WIDTH_SPACE), "仅含全角空格不应包含有效字符");
            assertTrue(StringUtil.hasText(" a "), "两端有空格但中间有字符，应包含有效字符");
            assertTrue(StringUtil.hasText("中文"), "中文应视为有效字符");
            assertTrue(StringUtil.hasText(NBSP), "不间断空格不被 String.isBlank 视为空白，应算作有效字符");
        }

        @Test
        @DisplayName("两个重载对同一输入结果一致")
        void bothOverloadsSameResult() {
            String[] data = samples();
            for (int i = 0; i < data.length; i++) {
                assertEquals(StringUtil.hasText((CharSequence) data[i]), StringUtil.hasText(data[i]),
                        "样本 [" + i + "] 在两个 hasText 重载下结果应一致");
            }
        }

        @Test
        @DisplayName("hasText 与 isEmpty 相互独立：纯空白串非空但无有效字符")
        void relationWithIsEmpty() {
            String blank = "   ";
            assertFalse(StringUtil.isEmpty(blank), "纯空白字符串不是空串");
            assertFalse(StringUtil.hasText(blank), "纯空白字符串不含有效字符");
            assertTrue(StringUtil.containsWhitespace(blank), "纯空白字符串一定包含空白字符");
        }
    }

    @Nested
    @DisplayName("containsWhitespace 是否包含空白")
    class ContainsWhitespace {

        @Test
        @DisplayName("CharSequence 重载：null / 空串 / 无空白为 false，含空白为 true")
        void charSequenceOverload() {
            assertFalse(StringUtil.containsWhitespace((CharSequence) null), "null 不包含空白字符");
            assertFalse(StringUtil.containsWhitespace((CharSequence) ""), "空字符串不包含空白字符");
            assertFalse(StringUtil.containsWhitespace((CharSequence) "abc"), "不含空白的字符串应返回 false");
            assertFalse(StringUtil.containsWhitespace((CharSequence) "中文123"),
                    "中文与数字都不是空白字符，应返回 false");
            assertFalse(StringUtil.containsWhitespace((CharSequence) NBSP),
                    "不间断空格不是空白字符，应返回 false");
            assertTrue(StringUtil.containsWhitespace((CharSequence) "a b"), "中间含空格应返回 true");
            assertTrue(StringUtil.containsWhitespace((CharSequence) "abc\t"),
                    "结尾的制表符也应被识别为空白");
            assertTrue(StringUtil.containsWhitespace((CharSequence) FULL_WIDTH_SPACE),
                    "全角空格应被识别为空白");
        }

        @Test
        @DisplayName("String 重载：内部委托给 CharSequence 重载，结果一致")
        void stringOverload() {
            assertFalse(StringUtil.containsWhitespace((String) null), "null 不包含空白字符");
            assertFalse(StringUtil.containsWhitespace(""), "空字符串不包含空白字符");
            assertFalse(StringUtil.containsWhitespace("abc"), "不含空白的字符串应返回 false");
            assertTrue(StringUtil.containsWhitespace(" "), "仅含一个空格应返回 true");
            assertTrue(StringUtil.containsWhitespace("a b\tc\n"), "多种空白字符应被识别");
        }

        @Test
        @DisplayName("两个重载对同一输入结果一致")
        void bothOverloadsSameResult() {
            String[] data = samples();
            for (int i = 0; i < data.length; i++) {
                assertEquals(StringUtil.containsWhitespace((CharSequence) data[i]),
                        StringUtil.containsWhitespace(data[i]),
                        "样本 [" + i + "] 在两个 containsWhitespace 重载下结果应一致");
            }
        }
    }

    @Nested
    @DisplayName("trimAllWhitespace 去除所有空白")
    class TrimAllWhitespace {

        @Test
        @DisplayName("null 原样返回 null")
        void nullInput() {
            assertNull(StringUtil.trimAllWhitespace(null), "null 输入应返回 null");
        }

        @Test
        @DisplayName("空串原样返回，且为同一引用")
        void emptyInput() {
            String empty = "";
            CharSequence result = StringUtil.trimAllWhitespace(empty);
            assertEquals("", result.toString(), "空串应原样返回");
            assertSame(empty, result, "空串分支直接返回入参引用，不会新建对象");
        }

        @Test
        @DisplayName("剔除全部空白字符后返回 StringBuilder")
        void trimsAllWhitespace() {
            CharSequence result = StringUtil.trimAllWhitespace(" a b\tc\nd ");
            assertEquals("abcd", result.toString(), "所有空白字符都应被剔除");
            assertInstanceOf(StringBuilder.class, result, "非空分支返回的是 StringBuilder，而非 String");
        }

        @Test
        @DisplayName("无空白时也会新建 StringBuilder，内容不变")
        void noWhitespaceStillRebuilds() {
            String source = "abc";
            CharSequence result = StringUtil.trimAllWhitespace(source);
            assertEquals("abc", result.toString(), "无空白时内容应保持不变");
            assertNotSame(source, result, "无空白时同样会新建 StringBuilder，不返回原引用");
        }

        @Test
        @DisplayName("全空白字符串被剔除为长度 0")
        void allWhitespace() {
            CharSequence result = StringUtil.trimAllWhitespace(" \t\n\u3000");
            assertEquals(0, result.length(), "全空白字符串剔除空白后应为空串");
        }

        @Test
        @DisplayName("不间断空格不是空白字符，应被保留")
        void keepsNbsp() {
            CharSequence result = StringUtil.trimAllWhitespace("a" + NBSP + "b");
            assertEquals("a" + NBSP + "b", result.toString(), "不间断空格不应被剔除");
        }

        @Test
        @DisplayName("支持非 String 的 CharSequence 入参")
        void supportsAnyCharSequence() {
            CharSequence result = StringUtil.trimAllWhitespace(new StringBuilder(" 你 好 "));
            assertEquals("你好", result.toString(), "StringBuilder 入参也应正确剔除空白");
        }
    }

    @Nested
    @DisplayName("capitalize 首字母大写")
    class Capitalize {

        @Test
        @DisplayName("null 与空串原样返回")
        void nullAndEmpty() {
            assertNull(StringUtil.capitalize(null), "null 输入应返回 null");
            String empty = "";
            assertSame(empty, StringUtil.capitalize(empty), "空串应原样返回同一引用");
        }

        @Test
        @DisplayName("小写首字母被转为大写")
        void lowerToUpper() {
            assertEquals("Abc", StringUtil.capitalize("abc"), "小写首字母应转大写");
            assertEquals("ABC", StringUtil.capitalize("aBC"), "仅首字母被处理，其余保持不变");
            assertEquals("A", StringUtil.capitalize("a"), "单字符也应被转大写");
        }

        @Test
        @DisplayName("首字符无变化时返回原引用")
        void returnsSameReferenceWhenUnchanged() {
            String already = "Abc";
            assertSame(already, StringUtil.capitalize(already), "首字母已是大写，应返回同一引用");
            String digit = "1abc";
            assertSame(digit, StringUtil.capitalize(digit), "数字无大小写之分，应返回同一引用");
            String chinese = "中文";
            assertSame(chinese, StringUtil.capitalize(chinese), "中文字符无大小写之分，应返回同一引用");
            String symbol = "@abc";
            assertSame(symbol, StringUtil.capitalize(symbol), "符号无大小写之分，应返回同一引用");
            String space = " abc";
            assertSame(space, StringUtil.capitalize(space), "首字符为空格时无变化，应返回同一引用");
        }

        @Test
        @DisplayName("标题字母会被转成对应大写字母")
        void titlecaseToUppercase() {
            assertEquals(UPPERCASE_DZ + "eta", StringUtil.capitalize(TITLECASE_DZ + "eta"),
                    "U+01C6 的大写形式是 U+01C4，两者不同故应新建字符串");
        }

        @Test
        @DisplayName("不会影响末尾字符")
        void onlyFirstCharChanged() {
            assertEquals("Abc DEF", StringUtil.capitalize("abc DEF"), "仅首字符被转大写");
        }
    }

    @Nested
    @DisplayName("uncapitalize 首字母小写")
    class Uncapitalize {

        @Test
        @DisplayName("null 与空串原样返回")
        void nullAndEmpty() {
            assertNull(StringUtil.uncapitalize(null), "null 输入应返回 null");
            String empty = "";
            assertSame(empty, StringUtil.uncapitalize(empty), "空串应原样返回同一引用");
        }

        @Test
        @DisplayName("大写首字母被转为小写")
        void upperToLower() {
            assertEquals("abc", StringUtil.uncapitalize("Abc"), "大写首字母应转小写");
            assertEquals("aBC", StringUtil.uncapitalize("ABC"), "仅首字母被处理，其余保持不变");
            assertEquals("a", StringUtil.uncapitalize("A"), "单字符也应被转小写");
        }

        @Test
        @DisplayName("首字符无变化时返回原引用")
        void returnsSameReferenceWhenUnchanged() {
            String already = "abc";
            assertSame(already, StringUtil.uncapitalize(already), "首字母已是小写，应返回同一引用");
            String digit = "1ABC";
            assertSame(digit, StringUtil.uncapitalize(digit), "数字无大小写之分，应返回同一引用");
            String chinese = "中文";
            assertSame(chinese, StringUtil.uncapitalize(chinese), "中文字符无大小写之分，应返回同一引用");
            String symbol = "@ABC";
            assertSame(symbol, StringUtil.uncapitalize(symbol), "符号无大小写之分，应返回同一引用");
        }

        @Test
        @DisplayName("大写特殊字母会被转成对应小写字母")
        void specialCharToLowercase() {
            assertEquals(TITLECASE_DZ + "eta", StringUtil.uncapitalize(UPPERCASE_DZ + "eta"),
                    "U+01C4 的小写形式是 U+01C6，两者不同故应新建字符串");
        }

        @Test
        @DisplayName("capitalize 与 uncapitalize 互为逆操作（可逆字符）")
        void roundTrip() {
            assertEquals("abc", StringUtil.uncapitalize(StringUtil.capitalize("abc")),
                    "小写先大写再小写应回到原值");
            assertEquals("Abc", StringUtil.capitalize(StringUtil.uncapitalize("Abc")),
                    "大写先小写再大写应回到原值");
        }
    }
}
