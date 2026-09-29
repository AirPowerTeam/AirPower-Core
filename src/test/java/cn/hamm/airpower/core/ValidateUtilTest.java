package cn.hamm.airpower.core;

import cn.hamm.airpower.core.constant.PatternConstant;
import cn.hamm.airpower.core.exception.ServiceException;
import cn.hamm.airpower.core.fixture.ValidDemoModel;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>验证器工具类测试</h1>
 *
 * <p>所有断言均与 {@link ValidateUtil} / {@code PatternConstant} 源码实际行为对齐。</p>
 *
 * @author Hamm.cn
 */
@DisplayName("验证器工具类测试")
class ValidateUtilTest {

    /**
     * 构造一个完全合法的模型
     *
     * @return 合法模型
     */
    private static ValidDemoModel validModel() {
        return new ValidDemoModel()
                .setName("张三")
                .setCount(10)
                .setMinValue(1)
                .setMaxValue(100)
                .setOnlyMobile("13800138000")
                .setOnlyTel("010-12345678")
                .setMobileOrTel("13800138000")
                .setGender(1)
                .setCreateName("创建名称");
    }

    /**
     * 执行模型校验并返回异常消息
     *
     * @param model   模型
     * @param actions 校验分组
     * @return 异常消息
     */
    private static String violationMessage(ValidDemoModel model, Class<?>... actions) {
        ValidationException e = assertThrows(ValidationException.class, () -> ValidateUtil.valid(model, actions),
                "校验失败时应抛 jakarta.validation.ValidationException");
        return e.getMessage();
    }

    /**
     * 一个完全没有任何约束注解的模型，用于验证「无注解模型不抛异常」
     */
    static class EmptyModel extends RootModel<EmptyModel> {
    }

    @Nested
    @DisplayName("正则类方法 - 正常路径与反例")
    class RegexMethods {

        @Test
        @DisplayName("是否数字: 正反例")
        void isNumber() {
            // NUMBER = ^-?\d+(\.\d+)?$ ：整数或小数，负号可选
            assertTrue(ValidateUtil.isNumber("123"), "纯整数应识别为数字");
            assertTrue(ValidateUtil.isNumber("123.45"), "小数应识别为数字");
            assertTrue(ValidateUtil.isNumber("-123"), "负整数应识别为数字");
            assertTrue(ValidateUtil.isNumber("0"), "0 应识别为数字");
            assertTrue(ValidateUtil.isNumber("-0"), "-0 应识别为数字");
            assertFalse(ValidateUtil.isNumber(""), "空串不是数字");
            assertFalse(ValidateUtil.isNumber("1.2.3"), "多个小数点不是数字");
            assertFalse(ValidateUtil.isNumber("12a"), "含字母不是数字");
            assertFalse(ValidateUtil.isNumber("+1"), "正号不被允许");
            assertFalse(ValidateUtil.isNumber(" 1"), "前导空格不被允许");
            assertFalse(ValidateUtil.isNumber("1 "), "尾部空格不被允许");
            assertFalse(ValidateUtil.isNumber(".1"), "必须以数字开头");
        }

        @Test
        @DisplayName("是否整数: 正反例")
        void isInteger() {
            // INTEGER = ^-?\d+$ ：只允许整数，不允许小数点
            assertTrue(ValidateUtil.isInteger("-0"), "-0 应识别为整数");
            assertTrue(ValidateUtil.isInteger("0"), "0 应识别为整数");
            assertTrue(ValidateUtil.isInteger("-123"), "负整数应识别为整数");
            assertFalse(ValidateUtil.isInteger("1.0"), "含小数点不是整数");
            assertFalse(ValidateUtil.isInteger(""), "空串不是整数");
            assertFalse(ValidateUtil.isInteger("1e3"), "科学计数法不是整数");
            assertFalse(ValidateUtil.isInteger("+5"), "正号不被允许");
        }

        @Test
        @DisplayName("是否邮箱: 正反例")
        void isEmail() {
            // EMAIL = ^(?!.*\.\.)[a-zA-Z0-9]+([._%+-][a-zA-Z0-9]+)*@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$
            assertTrue(ValidateUtil.isEmail("user@example.com"), "标准邮箱应识别为合法邮箱");
            assertTrue(ValidateUtil.isEmail("A@B.COM"), "大写邮箱应识别为合法邮箱");
            assertTrue(ValidateUtil.isEmail("a.b@c.cn"), "含点的用户名应识别为合法邮箱");
            assertTrue(ValidateUtil.isEmail("a+b_c@x-y.com"), "含特殊字符的本地部分应识别为合法邮箱");
            assertFalse(ValidateUtil.isEmail("user..a@example.com"), "连续两个点被负向预查排除");
            assertFalse(ValidateUtil.isEmail("user@example"), "顶级域前必须有点和至少两位顶级域");
            assertFalse(ValidateUtil.isEmail("@example.com"), "@ 前必须有本地部分");
            assertFalse(ValidateUtil.isEmail("a@b.c1"), "顶级域必须是纯字母");
            assertFalse(ValidateUtil.isEmail("a b@c.com"), "不允许包含空格");
        }

        @Test
        @DisplayName("是否字母: 正反例")
        void isLetter() {
            // LETTER = ^[A-Za-z]+$ ：至少一个字母
            assertTrue(ValidateUtil.isLetter("abc"), "纯小写字母应识别为字母");
            assertTrue(ValidateUtil.isLetter("ABC"), "纯大写字母应识别为字母");
            assertFalse(ValidateUtil.isLetter("abc1"), "含数字不是纯字母");
            assertFalse(ValidateUtil.isLetter(""), "空串不是字母（+ 至少一个）");
            assertFalse(ValidateUtil.isLetter("a b"), "含空格不是纯字母");
        }

        @Test
        @DisplayName("是否字母或数字: 正反例")
        void isLetterOrNumber() {
            // LETTER_OR_NUMBER = ^[A-Za-z0-9]+$
            assertTrue(ValidateUtil.isLetterOrNumber("a1"), "字母加数字应识别为字母或数字");
            assertTrue(ValidateUtil.isLetterOrNumber("A1"), "大写字母加数字应识别为字母或数字");
            assertFalse(ValidateUtil.isLetterOrNumber("a-1"), "连字符不被允许");
            assertFalse(ValidateUtil.isLetterOrNumber(""), "空串不是字母或数字");
            assertFalse(ValidateUtil.isLetterOrNumber("中文"), "中文不被允许");
        }

        @Test
        @DisplayName("是否中文: 正反例（空串不再匹配）")
        void isChinese() {
            // CHINESE = ^[\u4e00-\u9fff]+$ ：量词是 +，空串不再被判定为中文
            assertTrue(ValidateUtil.isChinese("中文"), "中文汉字应识别为中文");
            assertFalse(ValidateUtil.isChinese(""), "空串不应被识别为中文");
            assertFalse(ValidateUtil.isChinese("abc"), "字母不是中文");
            assertFalse(ValidateUtil.isChinese("中a"), "中英混排不是纯中文");
            assertFalse(ValidateUtil.isChinese("a中"), "中英混排不是纯中文");
            assertFalse(ValidateUtil.isChinese("。"), "中文标点不在 \u4e00-\u9fff 区间");
        }

        @Test
        @DisplayName("是否手机号: 正反例")
        void isMobilePhone() {
            // MOBILE_PHONE = ^(\+(\d{1,4}))?1[3-9](\d{9})$ ：可带 +86 前缀
            assertTrue(ValidateUtil.isMobilePhone("13800138000"), "标准 11 位手机号应识别为手机号");
            assertTrue(ValidateUtil.isMobilePhone("+8613800138000"), "带 +86 国家码应识别为手机号");
            assertTrue(ValidateUtil.isMobilePhone("19912345678"), "19 段号段应识别为手机号");
            assertFalse(ValidateUtil.isMobilePhone("12800138000"), "号段必须是 3-9");
            assertFalse(ValidateUtil.isMobilePhone("1380013800"), "位数不足 11 位");
            assertFalse(ValidateUtil.isMobilePhone("138001380000"), "位数超过 11 位");
            assertFalse(ValidateUtil.isMobilePhone("+86138001380001"), "带前缀后总位数过多");
            assertFalse(ValidateUtil.isMobilePhone("+1234513800138000"), "国家码超过 4 位");
            assertFalse(ValidateUtil.isMobilePhone("1a800138000"), "不允许含非数字字符");
            assertFalse(ValidateUtil.isMobilePhone(""), "空串不是手机号");
        }

        @Test
        @DisplayName("是否座机电话: 正反例（注意区号必须带连字符）")
        void isTelPhone() {
            // TEL_PHONE = ^(((0\d{2,3})-)?((\d{7,8})|(400\d{7})|(800\d{7}))(-(\d{1,4}))?)$
            // 区号分支中的「-」是必填的，没有 ? 量词，因此 "01012345678" 并不匹配
            assertTrue(ValidateUtil.isTelPhone("12345678"), "8 位纯号码应识别为座机");
            assertTrue(ValidateUtil.isTelPhone("1234567"), "7 位纯号码应识别为座机");
            assertTrue(ValidateUtil.isTelPhone("010-12345678"), "带连字符的区号应识别为座机");
            assertTrue(ValidateUtil.isTelPhone("0755-12345678-123"), "带分机号应识别为座机");
            assertTrue(ValidateUtil.isTelPhone("4001234567"), "400 客服号应识别为座机");
            assertTrue(ValidateUtil.isTelPhone("8001234567"), "800 客服号应识别为座机");
            assertFalse(ValidateUtil.isTelPhone("01012345678"), "区号后缺连字符不匹配（源码正则缺陷）");
            assertFalse(ValidateUtil.isTelPhone("12345"), "位数不足 7 位");
            assertFalse(ValidateUtil.isTelPhone("0101234567"), "区号后缺连字符且整体位数不足");
            assertFalse(ValidateUtil.isTelPhone("40012345678"), "400 号码固定 10 位，多一位不匹配");
            assertFalse(ValidateUtil.isTelPhone("010-123456789"), "分机号超过 4 位");
            assertFalse(ValidateUtil.isTelPhone("+8613800138000"), "手机号不是座机");
        }

        @Test
        @DisplayName("是否普通字符: 正反例（注意只允许单个字符）")
        void isNormalCode() {
            // NORMAL_CODE =^[@#%a-zA-Z0-9\u4e00-\u9fa5_\-\/+]$ ：单字符集合
            assertTrue(ValidateUtil.isNormalCode("a"), "单个小写字母是普通字符");
            assertTrue(ValidateUtil.isNormalCode("中"), "单个汉字是普通字符");
            assertTrue(ValidateUtil.isNormalCode("@"), "@ 是普通字符");
            assertTrue(ValidateUtil.isNormalCode("#"), "# 是普通字符");
            assertTrue(ValidateUtil.isNormalCode("%"), "% 是普通字符");
            assertTrue(ValidateUtil.isNormalCode("_"), "下划线是普通字符");
            assertTrue(ValidateUtil.isNormalCode("-"), "连字符是普通字符");
            assertTrue(ValidateUtil.isNormalCode("/"), "斜杠是普通字符");
            assertTrue(ValidateUtil.isNormalCode("+"), "加号是普通字符");
            assertTrue(ValidateUtil.isNormalCode("ab"), "多字符普通字符串由 + 量词匹配");
            assertTrue(ValidateUtil.isNormalCode("A1"), "字母数字混排由 + 量词匹配");
            assertTrue(ValidateUtil.isNormalCode("user_name-01"), "下划线连字符数字组合应匹配");
            assertFalse(ValidateUtil.isNormalCode("a b"), "含空格不应匹配");
            assertFalse(ValidateUtil.isNormalCode(" "), "空格不在允许集合中");
            assertFalse(ValidateUtil.isNormalCode(""), "空串不匹配");
        }

        @Test
        @DisplayName("是否纯数字加字母: 正反例")
        void isOnlyNumberAndLetter() {
            // NUMBER_OR_LETTER = ^[0-9a-zA-Z]+$
            assertTrue(ValidateUtil.isOnlyNumberAndLetter("a1"), "字母加数字应识别为纯数字加字母");
            assertTrue(ValidateUtil.isOnlyNumberAndLetter("A1"), "大写字母加数字应识别为纯数字加字母");
            assertFalse(ValidateUtil.isOnlyNumberAndLetter("a-1"), "连字符不被允许");
            assertFalse(ValidateUtil.isOnlyNumberAndLetter(""), "空串不是纯数字加字母");
            assertFalse(ValidateUtil.isOnlyNumberAndLetter("中"), "中文不被允许");
        }

        @Test
        @DisplayName("是否自然数: 正反例")
        void isNaturalNumber() {
            // NATURAL_NUMBER = ^[0-9]+(\.[0-9]+)?$ ：不允许负号
            assertTrue(ValidateUtil.isNaturalNumber("1.5"), "正小数应识别为自然数");
            assertTrue(ValidateUtil.isNaturalNumber("1"), "正整数应识别为自然数");
            assertTrue(ValidateUtil.isNaturalNumber("0"), "0 应识别为自然数");
            assertTrue(ValidateUtil.isNaturalNumber("01"), "允许前导零");
            assertFalse(ValidateUtil.isNaturalNumber("-1"), "负数不是自然数");
            assertFalse(ValidateUtil.isNaturalNumber("+1"), "正号不被允许");
            assertFalse(ValidateUtil.isNaturalNumber(""), "空串不是自然数");
            assertFalse(ValidateUtil.isNaturalNumber("1."), "小数点后必须有数字");
            assertFalse(ValidateUtil.isNaturalNumber("1.5.5"), "多个小数点不是自然数");
        }

        @Test
        @DisplayName("是否自然整数: 正反例")
        void isNaturalInteger() {
            // NATURAL_INTEGER = ^[0-9]+$ ：正整数与 0，不允许小数点和负号
            assertTrue(ValidateUtil.isNaturalInteger("0"), "0 应识别为自然整数");
            assertTrue(ValidateUtil.isNaturalInteger("1"), "1 应识别为自然整数");
            assertTrue(ValidateUtil.isNaturalInteger("007"), "允许前导零");
            assertFalse(ValidateUtil.isNaturalInteger("-1"), "负数不是自然整数");
            assertFalse(ValidateUtil.isNaturalInteger("1.5"), "含小数点不是自然整数");
            assertFalse(ValidateUtil.isNaturalInteger(""), "空串不是自然整数");
            assertFalse(ValidateUtil.isNaturalInteger(" 1"), "前导空格不被允许");
        }
    }

    @Nested
    @DisplayName("正则类方法 - 传入 null 时返回 false")
    class NullInputs {

        @Test
        @DisplayName("是否数字传 null 返回 false")
        void isNumberNull() {
            assertFalse(ValidateUtil.isNumber(null), "传 null 应判定为不匹配");
        }

        @Test
        @DisplayName("是否整数传 null 返回 false")
        void isIntegerNull() {
            assertFalse(ValidateUtil.isInteger(null), "传 null 应判定为不匹配");
        }

        @Test
        @DisplayName("是否邮箱传 null 返回 false")
        void isEmailNull() {
            assertFalse(ValidateUtil.isEmail(null), "传 null 应判定为不匹配");
        }

        @Test
        @DisplayName("是否字母传 null 返回 false")
        void isLetterNull() {
            assertFalse(ValidateUtil.isLetter(null), "传 null 应判定为不匹配");
        }

        @Test
        @DisplayName("是否字母或数字传 null 返回 false")
        void isLetterOrNumberNull() {
            assertFalse(ValidateUtil.isLetterOrNumber(null), "传 null 应判定为不匹配");
        }

        @Test
        @DisplayName("是否中文传 null 返回 false")
        void isChineseNull() {
            assertFalse(ValidateUtil.isChinese(null), "传 null 应判定为不匹配");
        }

        @Test
        @DisplayName("是否手机号传 null 返回 false")
        void isMobilePhoneNull() {
            assertFalse(ValidateUtil.isMobilePhone(null), "传 null 应判定为不匹配");
        }

        @Test
        @DisplayName("是否座机传 null 返回 false")
        void isTelPhoneNull() {
            assertFalse(ValidateUtil.isTelPhone(null), "传 null 应判定为不匹配");
        }

        @Test
        @DisplayName("是否普通字符传 null 返回 false")
        void isNormalCodeNull() {
            assertFalse(ValidateUtil.isNormalCode(null), "传 null 应判定为不匹配");
        }

        @Test
        @DisplayName("是否纯数字加字母传 null 返回 false")
        void isOnlyNumberAndLetterNull() {
            assertFalse(ValidateUtil.isOnlyNumberAndLetter(null), "传 null 应判定为不匹配");
        }

        @Test
        @DisplayName("是否自然数传 null 返回 false")
        void isNaturalNumberNull() {
            assertFalse(ValidateUtil.isNaturalNumber(null), "传 null 应判定为不匹配");
        }

        @Test
        @DisplayName("是否自然整数传 null 返回 false")
        void isNaturalIntegerNull() {
            assertFalse(ValidateUtil.isNaturalInteger(null), "传 null 应判定为不匹配");
        }
    }

    @Nested
    @DisplayName("正则校验 validRegex")
    class ValidRegex {

        @Test
        @DisplayName("自定义正则匹配成功与失败")
        void customPattern() {
            Pattern pattern = Pattern.compile("^[a-z]{3}-\\d{4}$");
            assertTrue(ValidateUtil.validRegex("abc-1234", pattern), "符合自定义正则应返回 true");
            assertFalse(ValidateUtil.validRegex("ABC-1234", pattern), "大写不符合自定义正则");
            assertFalse(ValidateUtil.validRegex("abc-123", pattern), "位数不足不符合自定义正则");
        }

        @Test
        @DisplayName("空串与允许空串的正则")
        void emptyValue() {
            assertFalse(ValidateUtil.validRegex("", Pattern.compile("^[a-z]+$")), "空串不匹配 + 量词正则");
            assertTrue(ValidateUtil.validRegex("", Pattern.compile("^[a-z]*$")), "空串匹配 * 量词正则");
        }

        @Test
        @DisplayName("值为 null 时返回 false")
        void nullValue() {
            assertFalse(ValidateUtil.validRegex(null, PatternConstant.NUMBER), "值为 null 应判定为不匹配");
        }

        @Test
        @DisplayName("正则为 null 时返回 false")
        void nullPattern() {
            assertFalse(ValidateUtil.validRegex("123", null), "正则为 null 应判定为不匹配");
        }
    }

    @Nested
    @DisplayName("二代身份证校验 isChina2Identity")
    class China2Identity {

        @Test
        @DisplayName("null 返回 false")
        void nullValue() {
            assertFalse(ValidateUtil.isChina2Identity(null), "null 应直接返回 false");
        }

        @Test
        @DisplayName("长度既不是 15 也不是 18 返回 false")
        void invalidLength() {
            assertFalse(ValidateUtil.isChina2Identity(""), "空串长度非法应返回 false");
            assertFalse(ValidateUtil.isChina2Identity("11010519491231"), "14 位长度非法应返回 false");
            assertFalse(ValidateUtil.isChina2Identity("11010519491231002"), "17 位长度非法应返回 false");
            assertFalse(ValidateUtil.isChina2Identity("11010519491231002XY"), "19 位长度非法应返回 false");
        }

        @Test
        @DisplayName("18 位且校验位正确返回 true")
        void validIdCard() {
            // factor = {7,9,10,5,8,4,2,1,6,3,7,9,10,5,8,4,2}
            // 前 17 位加权求和 = 167，167 % 11 = 2，flags[2] = 'X'，与末位一致
            assertTrue(ValidateUtil.isChina2Identity("11010519491231002X"), "校验位正确的 18 位身份证应通过");
        }

        @Test
        @DisplayName("18 位校验位错误返回 false")
        void wrongCheckCode() {
            // 加权和仍为 167，期望校验位 'X'
            assertFalse(ValidateUtil.isChina2Identity("110105194912310021"), "末位写成 1 应校验失败");
            assertFalse(ValidateUtil.isChina2Identity("110105194912310020"), "末位写成 0 应校验失败");
            assertFalse(ValidateUtil.isChina2Identity("110105194912310023"), "末位写成 3 应校验失败");
            assertFalse(ValidateUtil.isChina2Identity("11010519491231002_"), "末位为下划线应校验失败");
        }

        @Test
        @DisplayName("18 位末位为小写 x 返回 true（大小写均可）")
        void lowercaseX() {
            // 加权和 167，167 % 11 = 2，flags[2] = 'X'，源码把末位小写 x 归一化为 X 后比对
            assertTrue(ValidateUtil.isChina2Identity("11010519491231002x"), "校验位允许小写 x");
        }

        @Test
        @DisplayName("15 位一代身份证抛 ServiceException")
        void firstGeneration() {
            ServiceException e = assertThrows(ServiceException.class, () -> ValidateUtil.isChina2Identity("110105194912310"),
                    "15 位一代身份证应抛 ServiceException");
            assertEquals("暂不支持一代身份证校验", e.getMessage(), "异常消息应与源码一致");
        }

        @Test
        @DisplayName("18 位前 17 位含非数字字符返回 false")
        void notDigitInBody() {
            assertFalse(ValidateUtil.isChina2Identity("1101051949123100X2"), "前 17 位含非数字字符应判定为不匹配");
        }
    }

    @Nested
    @DisplayName("模型校验 valid")
    class Valid {

        @Test
        @DisplayName("模型为 null 时直接返回，不抛异常")
        void nullModel() {
            ValidDemoModel model = null;
            assertDoesNotThrow(() -> ValidateUtil.valid(model), "模型为 null 时应直接返回");
        }

        @Test
        @DisplayName("无任何约束注解的模型不抛异常")
        void modelWithoutConstraint() {
            assertDoesNotThrow(() -> ValidateUtil.valid(new EmptyModel()), "无注解模型不应抛异常");
        }

        @Test
        @DisplayName("全部合法的模型不抛异常")
        void validModelShouldPass() {
            assertDoesNotThrow(() -> ValidateUtil.valid(validModel()), "合法模型不应抛异常");
        }

        @Test
        @DisplayName("空模型（所有字段为 null）仅触发 @NotNull 与 @NotBlank")
        void emptyModel() {
            // 未加约束的字段（@Min / @Max / @Phone / @Dictionary 对 null 均视为通过）
            String message = violationMessage(new ValidDemoModel());
            assertTrue(Set.of("名称不能为空", "数量不能为空").contains(message),
                    "空模型应抛出名称或数量的非空错误，实际为：" + message);
        }

        @Test
        @DisplayName("@NotBlank: null、空串、纯空格均不通过")
        void notBlank() {
            ValidDemoModel nullName = validModel().setName(null);
            assertEquals("名称不能为空", violationMessage(nullName), "名称为 null 时异常消息应与注解一致");

            ValidDemoModel emptyName = validModel().setName("");
            assertEquals("名称不能为空", violationMessage(emptyName), "名称为空串时异常消息应与注解一致");

            ValidDemoModel blankName = validModel().setName("   ");
            assertEquals("名称不能为空", violationMessage(blankName), "名称为纯空格时异常消息应与注解一致");

            ValidDemoModel goodName = validModel().setName(" 李四 ");
            assertDoesNotThrow(() -> ValidateUtil.valid(goodName), "名称首尾带空格不是空，应通过 @NotBlank");
        }

        @Test
        @DisplayName("@NotNull: 为 null 时不通过")
        void notNull() {
            assertEquals("数量不能为空", violationMessage(validModel().setCount(null)),
                    "数量为 null 时异常消息应与注解一致");
        }

        @Test
        @DisplayName("@Min 边界: 1 通过，0 与负数不通过")
        void minBoundary() {
            assertDoesNotThrow(() -> ValidateUtil.valid(validModel().setMinValue(1)), "最小值取下边界 1 应通过");
            assertDoesNotThrow(() -> ValidateUtil.valid(validModel().setMinValue(null)), "@Min 对 null 不做校验");
            assertEquals("最小值不能小于1", violationMessage(validModel().setMinValue(0)),
                    "最小值取 0 时异常消息应与注解一致");
            assertEquals("最小值不能小于1", violationMessage(validModel().setMinValue(-1)),
                    "最小值为负数时异常消息应与注解一致");
        }

        @Test
        @DisplayName("@Max 边界: 100 通过，101 不通过")
        void maxBoundary() {
            assertDoesNotThrow(() -> ValidateUtil.valid(validModel().setMaxValue(100)), "最大值取上边界 100 应通过");
            assertDoesNotThrow(() -> ValidateUtil.valid(validModel().setMaxValue(null)), "@Max 对 null 不做校验");
            assertEquals("最大值不能大于100", violationMessage(validModel().setMaxValue(101)),
                    "最大值取 101 时异常消息应与注解一致");
        }

        @Test
        @DisplayName("非法校验时抛 jakarta.validation.ValidationException 而非 ServiceException")
        void exceptionType() {
            ValidationException e = assertThrows(ValidationException.class,
                    () -> ValidateUtil.valid(validModel().setCount(null)),
                    "校验失败应抛 jakarta.validation.ValidationException");
            assertEquals("数量不能为空", e.getMessage(), "异常消息应取第一个违规项的 message");
            assertNotEquals(ServiceException.class, e.getClass(), "校验异常不应是 ServiceException");
        }

        @Test
        @DisplayName("分组校验: 不传分组时只校验 Default 组")
        void withoutGroup() {
            // createName 的 @NotBlank 属于 Create 组，默认不参与校验
            ValidDemoModel model = validModel().setCreateName(null);
            assertDoesNotThrow(() -> ValidateUtil.valid(model), "默认分组不校验 Create 组的约束");
        }

        @Test
        @DisplayName("分组校验: 指定 Create 组时只校验该组约束")
        void withGroup() {
            assertDoesNotThrow(() -> ValidateUtil.valid(validModel().setCreateName("名称"), ValidDemoModel.Create.class),
                    "Create 组字段合法时校验通过");
            assertEquals("创建场景名称不能为空",
                    violationMessage(validModel().setCreateName(null), ValidDemoModel.Create.class),
                    "Create 组字段为空时异常消息应与注解一致");
        }

        @Test
        @DisplayName("分组校验: 指定 Create 组时不校验 Default 组约束")
        void groupIsolation() {
            ValidDemoModel model = validModel().setName(null).setCount(null).setGender(99)
                    .setCreateName("名称");
            assertDoesNotThrow(() -> ValidateUtil.valid(model, ValidDemoModel.Create.class),
                    "指定 Create 组时不应校验 Default 组的约束");
        }

        @Test
        @DisplayName("多字段同时违规时抛 ValidationException（消息取任一违规项）")
        void multipleViolations() {
            ValidDemoModel model = validModel().setName(null).setGender(99);
            String message = violationMessage(model);
            assertTrue(Set.of("名称不能为空", "不允许的枚举字典值").contains(message),
                    "多违规项时消息应来自其中一项，实际为：" + message);
        }

        @Test
        @DisplayName("分组参数为 null 时按空分组处理")
        void nullActions() {
            assertDoesNotThrow(() -> ValidateUtil.valid(validModel(), (Class<?>[]) null), "分组参数为 null 时按空分组处理");
        }
    }

    /**
     * <h2>多违规时的错误消息稳定性</h2>
     *
     * <p>回归 P1-9：{@code valid} 曾直接取 {@code violations.iterator().next()}，
     * 而 {@link Set} 的迭代顺序不保证稳定，同一个对象多次校验会报出不同字段，
     * 上层无法据此做字段级回显。现改为按属性路径排序后取第一条。</p>
     */
    @Nested
    @DisplayName("多违规时的错误消息稳定性")
    class MultiViolationStabilityTest {

        /**
         * 重复校验次数
         */
        private static final int REPEAT = 300;

        @Test
        @DisplayName("同一对象连续多次校验，抛出的消息始终一致")
        void messageIsStableAcrossRepeatedCalls() {
            // name 为空、count 为空、maxValue 超界 —— 必然触发三条违规
            Set<String> messages = new LinkedHashSet<>();
            for (int i = 0; i < REPEAT; i++) {
                ValidationException e = assertThrows(ValidationException.class,
                        () -> ValidateUtil.valid(new ValidDemoModel().setMaxValue(999)),
                        "第 " + i + " 次校验都应触发违规");
                messages.add(e.getMessage());
            }
            assertEquals(1, messages.size(),
                    "同一模型连续校验 " + REPEAT + " 次应始终报同一个字段，实际出现：" + messages);
        }

        @Test
        @DisplayName("消息稳定的是字段路径，与 Set 迭代顺序无关")
        void messageDependsOnPropertyPathNotIterationOrder() {
            // count / maxValue / name 三条违规中，属性路径 "count" 字典序最小
            Set<String> messages = new LinkedHashSet<>();
            for (int i = 0; i < REPEAT; i++) {
                ValidationException e = assertThrows(ValidationException.class,
                        () -> ValidateUtil.valid(new ValidDemoModel().setMaxValue(999)),
                        "模型必然存在多条违规");
                messages.add(e.getMessage());
            }
            assertEquals("数量不能为空", messages.iterator().next(),
                    "应取属性路径字典序最小的那条，实际为：" + messages);
        }

        @Test
        @DisplayName("不同违规组合仍能各自稳定报出消息")
        void differentViolationsRemainDeterministic() {
            Set<String> messages = new LinkedHashSet<>();
            for (int i = 0; i < REPEAT; i++) {
                ValidationException e = assertThrows(ValidationException.class,
                        () -> ValidateUtil.valid(new ValidDemoModel().setCount(1).setMaxValue(999)),
                        "maxValue 超界必然触发违规");
                messages.add(e.getMessage());
            }
            assertEquals(1, messages.size(), "单一违规的消息应稳定：" + messages);
            assertEquals("最大值不能大于100", messages.iterator().next(), "应报最大值相关的提示");
        }
    }

    /**
     * <h2>验证器资源释放</h2>
     *
     * <p>回归 P1-10：{@code ValidatorFactory} 此前被静态持有却没有任何关闭入口，
     * 在热部署 / 容器反复重载场景下无法回收。新增 {@link ValidateUtil#close()}。</p>
     */
    @Nested
    @DisplayName("验证器生命周期")
    class LifecycleTest {

        @Test
        @DisplayName("close() 后再次 valid() 可自动重新初始化")
        void validWorksAfterClose() {
            // 先确保验证器已初始化
            assertDoesNotThrow(() -> ValidateUtil.valid(validModel()), "关闭前校验应正常");

            ValidateUtil.close();

            // 关闭后 validator 被置空，下次 valid 会重新构建工厂
            assertDoesNotThrow(() -> ValidateUtil.valid(validModel()),
                    "close() 之后 valid() 应自动重建验证器，而不是抛空指针");
            ValidationException e = assertThrows(ValidationException.class,
                    () -> ValidateUtil.valid(new ValidDemoModel().setMaxValue(999)),
                    "重建后的验证器应照常报告违规");
            assertNotNull(e.getMessage(), "重建后抛出的异常应带消息");
        }

        @Test
        @DisplayName("重复调用 close() 是安全的")
        void closeIsIdempotent() {
            ValidateUtil.close();
            assertDoesNotThrow(ValidateUtil::close, "重复关闭不应抛异常");
            assertDoesNotThrow(ValidateUtil::close, "多次关闭仍不应抛异常");
        }

        @Test
        @DisplayName("close() 释放资源不影响后续校验结果")
        void closeDoesNotChangeResult() {
            ValidationException before = assertThrows(ValidationException.class,
                    () -> ValidateUtil.valid(new ValidDemoModel().setMaxValue(999)),
                    "关闭前应报出违规");

            ValidateUtil.close();

            ValidationException after = assertThrows(ValidationException.class,
                    () -> ValidateUtil.valid(new ValidDemoModel().setMaxValue(999)),
                    "关闭后仍应报出违规");
            assertEquals(before.getMessage(), after.getMessage(),
                    "重建验证器后的报错应与关闭前完全一致");
        }
    }
}
