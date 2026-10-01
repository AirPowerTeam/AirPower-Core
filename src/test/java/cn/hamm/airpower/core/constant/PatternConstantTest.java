package cn.hamm.airpower.core.constant;

import cn.hamm.airpower.core.ValidateUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>PatternConstant 单元测试</h1>
 *
 * <p>直接覆盖每个预编译正则的边界行为。原先这些正则只能经 {@code ValidateUtil}
 * 间接触达，而 {@code ValidateUtilTest} 只测了其中五个方法，
 * {@code CHINESE} / {@code MOBILE_PHONE} / {@code TEL_PHONE} / {@code NORMAL_CODE}
 * 这几条零直接覆盖——其中 {@code CHINESE} 与 {@code NORMAL_CODE} 的汉字区间
 * 一度不一致，正是因为没有用例把两者放在一起比对。</p>
 *
 * @author Hamm.cn
 */
@DisplayName("预编译正则常量单元测试")
class PatternConstantTest {

    @Test
    @DisplayName("NUMBER 接受整数与小数，拒绝空串与字母")
    void number() {
        assertTrue(PatternConstant.NUMBER.matcher("123").matches());
        assertTrue(PatternConstant.NUMBER.matcher("-123").matches());
        assertTrue(PatternConstant.NUMBER.matcher("1.5").matches());
        assertFalse(PatternConstant.NUMBER.matcher("").matches(), "空串不应被接受");
        assertFalse(PatternConstant.NUMBER.matcher("1.2.3").matches());
        assertFalse(PatternConstant.NUMBER.matcher("12a").matches());
    }

    @Test
    @DisplayName("INTEGER 只接受整数，拒绝小数")
    void integer() {
        assertTrue(PatternConstant.INTEGER.matcher("123").matches());
        assertTrue(PatternConstant.INTEGER.matcher("-123").matches());
        assertFalse(PatternConstant.INTEGER.matcher("1.5").matches(), "INTEGER 不应接受小数");
    }

    @Test
    @DisplayName("NATURAL_INTEGER / NATURAL_NUMBER 不接受负号")
    void natural() {
        assertTrue(PatternConstant.NATURAL_INTEGER.matcher("0").matches());
        assertFalse(PatternConstant.NATURAL_INTEGER.matcher("-1").matches(), "自然数不应接受负号");
        assertTrue(PatternConstant.NATURAL_NUMBER.matcher("1.5").matches());
        assertFalse(PatternConstant.NATURAL_NUMBER.matcher("-1.5").matches());
    }

    @Test
    @DisplayName("LETTER / LETTER_OR_NUMBER / NUMBER_OR_LETTER 边界")
    void letters() {
        assertTrue(PatternConstant.LETTER.matcher("abcXYZ").matches());
        assertFalse(PatternConstant.LETTER.matcher("abc1").matches());
        assertTrue(PatternConstant.LETTER_OR_NUMBER.matcher("abc123").matches());
        assertFalse(PatternConstant.LETTER_OR_NUMBER.matcher("a-b").matches());
        assertTrue(PatternConstant.NUMBER_OR_LETTER.matcher("a1").matches());
        assertFalse(PatternConstant.NUMBER_OR_LETTER.matcher("1.5").matches(), "此模式不含小数点");
    }

    @Test
    @DisplayName("EMAIL 接受常见形态，拒绝缺 @ 与缺域名")
    void email() {
        for (String valid : new String[]{"a@b.com", "first.last@sub.domain.cn", "a+tag@b.io"}) {
            assertTrue(PatternConstant.EMAIL.matcher(valid).matches(), "应接受：" + valid);
        }
        for (String invalid : new String[]{"", "a@b", "a.com", "@b.com", "a b@c.com"}) {
            assertFalse(PatternConstant.EMAIL.matcher(invalid).matches(), "应拒绝：" + invalid);
        }
    }

    @Test
    @DisplayName("MOBILE_PHONE 接受 1 开头 11 位，可带国际区号")
    void mobilePhone() {
        assertTrue(PatternConstant.MOBILE_PHONE.matcher("13800138000").matches());
        assertTrue(PatternConstant.MOBILE_PHONE.matcher("+8613800138000").matches());
        assertTrue(PatternConstant.MOBILE_PHONE.matcher("+8613800138000").matches());
        assertFalse(PatternConstant.MOBILE_PHONE.matcher("12345678901").matches(), "第二位必须是 3-9");
        assertFalse(PatternConstant.MOBILE_PHONE.matcher("1380013800").matches(), "位数不足 11");
        assertFalse(PatternConstant.MOBILE_PHONE.matcher("138001380000").matches(), "位数超过 11");
    }

    @Test
    @DisplayName("TEL_PHONE 覆盖 5 层嵌套分组里的各分支")
    void telPhone() {
        for (String valid : new String[]{
                "010-1234567", "0755-12345678", "1234567", "12345678",
                "4001234567", "8001234567", "010-1234567-1234", "12345678-123"}) {
            assertTrue(PatternConstant.TEL_PHONE.matcher(valid).matches(), "应接受：" + valid);
        }
        for (String invalid : new String[]{"", "123", "abc-defg", "400123", "12345678#123"}) {
            assertFalse(PatternConstant.TEL_PHONE.matcher(invalid).matches(), "应拒绝：" + invalid);
        }
    }

    @Test
    @DisplayName("CHINESE 覆盖 Unicode 区间边界")
    void chinese() {
        assertTrue(PatternConstant.CHINESE.matcher("中文").matches());
        assertTrue(PatternConstant.CHINESE.matcher("鿿").matches(), "上界 \\u9fff 应被接受");
        assertFalse(PatternConstant.CHINESE.matcher("一A").matches(), "混入字母应被拒绝");
        assertFalse(PatternConstant.CHINESE.matcher("").matches());
    }

    @Test
    @DisplayName("NORMAL_CODE 允许的字符集与 CHINESE 的汉字区间必须一致")
    void normalCodeRangeMatchesChinese() {
        // 这两条正则的汉字上界曾经不同（CHINESE 是 \\u9fff，NORMAL_CODE 是 \\u9fa5），
        // 导致 \\u9fa6~\\u9fff 被 NORMAL_CODE 接受却被 CHINESE 拒绝
        for (String cjk : new String[]{"一", "鿻", "鿿"}) {
            boolean byNormalCode = PatternConstant.NORMAL_CODE.matcher(cjk).matches();
            boolean byChinese = PatternConstant.CHINESE.matcher(cjk).matches();
            assertEquals(byChinese, byNormalCode,
                    "汉字 U+" + Integer.toHexString(cjk.charAt(0))
                            + " 在 CHINESE 与 NORMAL_CODE 中的判定必须一致");
        }
    }

    @Test
    @DisplayName("NORMAL_CODE 允许 javadoc 声明的全部字符")
    void normalCodeAllowedChars() {
        for (String valid : new String[]{"abcXYZ", "0123", "中文", "WH-001", "a/b", "a\\b", "@#%_+-"}) {
            assertTrue(PatternConstant.NORMAL_CODE.matcher(valid).matches(), "应允许：" + valid);
        }
        for (String invalid : new String[]{"", "a b", "a=b", "a,b", "a*b"}) {
            assertFalse(PatternConstant.NORMAL_CODE.matcher(invalid).matches(), "应拒绝：" + invalid);
        }
    }

    @Test
    @DisplayName("每个常量与 ValidateUtil 的对应方法行为一致")
    void consistentWithValidateUtil() {
        String[] samples = {"", "123", "abc", "中文", "13800138000", "a-b", "-1", "1.5"};
        assertThrowsConsistent(ValidateUtil::isChinese, PatternConstant.CHINESE, samples);
        assertThrowsConsistent(ValidateUtil::isNormalCode, PatternConstant.NORMAL_CODE, samples);
        assertThrowsConsistent(ValidateUtil::isMobilePhone, PatternConstant.MOBILE_PHONE, samples);
        assertThrowsConsistent(ValidateUtil::isTelPhone, PatternConstant.TEL_PHONE, samples);
        assertThrowsConsistent(ValidateUtil::isNumber, PatternConstant.NUMBER, samples);
        assertThrowsConsistent(ValidateUtil::isInteger, PatternConstant.INTEGER, samples);
    }

    /**
     * 断言校验方法与正则常量对同一批样本给出相同结论
     *
     * @param method  校验方法
     * @param pattern 正则常量
     * @param samples 样本
     */
    private static void assertThrowsConsistent(java.util.function.Predicate<String> method,
                                              java.util.regex.Pattern pattern, String[] samples) {
        for (String sample : samples) {
            assertEquals(pattern.matcher(sample).matches(), method.test(sample),
                    "ValidateUtil 与 " + pattern.pattern() + " 对样本「" + sample + "」的结论应一致");
        }
    }
}
