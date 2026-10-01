package cn.hamm.airpower.core;

import jakarta.validation.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static cn.hamm.airpower.core.constant.PatternConstant.*;

/**
 * <h1>验证器工具类</h1>
 *
 * @author Hamm.cn
 */
public class ValidateUtil {
    /**
     * 验证器实例
     */
    private static volatile Validator validator;
    /**
     * ValidatorFactory 实例（延迟初始化，应用关闭时统一关闭）
     */
    private static ValidatorFactory validatorFactory;

    /**
     * 禁止外部实例化
     */
    @Contract(pure = true)
    private ValidateUtil() {
    }

    /**
     * 初始化验证器
     */
    private static synchronized void initValidator() {
        if (validator != null) {
            return;
        }
        if (validatorFactory == null) {
            validatorFactory = Validation.buildDefaultValidatorFactory();
        }
        validator = validatorFactory.getValidator();
    }

    /**
     * 是否是数字
     *
     * @param value 参数
     * @return 验证结果
     */
    public static boolean isNumber(String value) {
        return validRegex(value, NUMBER);
    }

    /**
     * 是否是整数
     *
     * @param value 参数
     * @return 验证结果
     */
    public static boolean isInteger(String value) {
        return validRegex(value, INTEGER);
    }

    /**
     * 是否是邮箱
     *
     * @param value 参数
     * @return 验证结果
     */
    public static boolean isEmail(String value) {
        return validRegex(value, EMAIL);
    }

    /**
     * 是否是字母
     *
     * @param value 参数
     * @return 验证结果
     */
    public static boolean isLetter(String value) {
        return validRegex(value, LETTER);
    }

    /**
     * 是否是字母+数字
     *
     * @param value 参数
     * @return 验证结果
     */
    public static boolean isLetterOrNumber(String value) {
        return validRegex(value, LETTER_OR_NUMBER);
    }

    /**
     * 是否是中文汉字
     *
     * @param value 参数
     * @return 验证结果
     */
    public static boolean isChinese(String value) {
        return validRegex(value, CHINESE);
    }

    /**
     * 是否是手机号
     *
     * @param value 参数
     * @return 验证结果
     */
    public static boolean isMobilePhone(String value) {
        return validRegex(value, MOBILE_PHONE);
    }

    /**
     * 是否是座机电话
     *
     * @param value 参数
     * @return 验证结果
     */
    public static boolean isTelPhone(String value) {
        return validRegex(value, TEL_PHONE);
    }

    /**
     * 是否是普通字符
     *
     * @param value 参数
     * @return 验证结果
     * @apiNote 允许字符：{@code @ # % a-z A-Z 0-9 汉字 _ - \ + /}。
     * 其中 {@code -} 常见于编码前缀（如 {@code WH-001}）、
     * {@code \} 常见于路径与转义，两者都是有安全含义的字符
     */
    public static boolean isNormalCode(String value) {
        return validRegex(value, NORMAL_CODE);
    }

    /**
     * 是否是纯字母和数字
     *
     * @param value 参数
     * @return 验证结果
     */
    public static boolean isOnlyNumberAndLetter(String value) {
        return validRegex(value, NUMBER_OR_LETTER);
    }

    /**
     * 是否是自然数
     *
     * @param value 参数
     * @return 验证结果
     */
    public static boolean isNaturalNumber(String value) {
        return validRegex(value, NATURAL_NUMBER);
    }

    /**
     * 是否是自然整数
     *
     * @param value 参数
     * @return 验证结果
     */
    public static boolean isNaturalInteger(String value) {
        return validRegex(value, NATURAL_INTEGER);
    }

    /**
     * 是否是有效二代身份证号
     *
     * @param idCard 身份证号
     * @return 验证结果
     */
    public static boolean isChina2Identity(String idCard) {
        // 二代身份证长度
        final int id2Length = 18;
        // 二代身份证求余数
        final int id2Mod = 11;
        // 系数
        final int[] factor = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
        // 尾数
        final char[] flags = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};

        if (Objects.isNull(idCard)) {
            return false;
        }
        // 15 位一代身份证没有校验位，无法做校验码比对，直接判否。
        // 这里必须返回 false 而不是抛异常：本方法是 isXxx 谓词，
        // 调用方普遍写成 if (isChina2Identity(id)) 放行 else 拒绝，
        // 抛异常会让 15 位号码落到「系统错误」分支，
        // 用户看到的是「暂不支持」这种毫无意义且无法自行处理的提示
        if (idCard.length() != id2Length) {
            return false;
        }
        // 前 17 位必须是数字，校验位允许大写 X 或小写 x
        if (!isDigits(idCard, id2Length - 1)) {
            return false;
        }
        char checkCode = idCard.charAt(idCard.length() - 1);
        if (checkCode == 'x') {
            checkCode = 'X';
        }
        int sum = IntStream.range(0, id2Length - 1)
                .map(i -> Character.digit(idCard.charAt(i), 10) * factor[i])
                .sum();
        // 求和后取余数11，得到的余数与校验码进行匹配，匹配成功，说明通过验证。
        return flags[sum % id2Mod] == checkCode;
    }

    /**
     * 判断前 {@code length} 个字符是否都是数字
     *
     * @param value  字符串
     * @param length 校验长度
     * @return 是否为纯数字
     */
    private static boolean isDigits(String value, int length) {
        for (int i = 0; i < length; i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 正则校验
     *
     * @param value   参数
     * @param pattern 正则
     * @return 验证结果
     */
    public static boolean validRegex(String value, Pattern pattern) {
        if (Objects.isNull(value) || Objects.isNull(pattern)) {
            // 空值与空正则一律视为不匹配，避免抛出空指针
            return false;
        }
        return pattern.matcher(value).matches();
    }

    /**
     * 验证传入的数据模型
     *
     * @param model   数据模型
     * @param actions {@code 可选} 校验分组
     * @param <M>     模型类型
     * @apiNote 多个违规时按属性路径排序后取第一条，保证同一对象的报错稳定可复现
     */
    public static <M extends RootModel<M>> void valid(M model, Class<?>... actions) {
        if (Objects.isNull(model)) {
            return;
        }
        initValidator();
        Class<?>[] groups = Objects.isNull(actions) ? new Class<?>[0] : actions;
        Set<ConstraintViolation<M>> violations = groups.length == 0
                ? validator.validate(model)
                : validator.validate(model, groups);
        if (violations.isEmpty()) {
            return;
        }
        throw new ValidationException(firstViolation(violations).getMessage());
    }

    /**
     * 取第一条违规
     *
     * @param violations 违规集合
     * @param <M>        模型类型
     * @return 第一条违规
     * @apiNote {@link Set} 的迭代顺序不保证稳定，直接取 {@code iterator().next()}
     * 会让同一个对象多次校验报出不同字段，上层无法据此做字段级回显
     */
    private static <M> @NotNull ConstraintViolation<M> firstViolation(
            @NotNull Set<ConstraintViolation<M>> violations
    ) {
        return violations.stream()
                .min(Comparator
                        .comparing((ConstraintViolation<M> v) -> v.getPropertyPath().toString())
                        .thenComparing(ConstraintViolation::getMessage))
                .orElseThrow();
    }

    /**
     * 关闭验证器并释放底层资源
     *
     * @apiNote {@link ValidatorFactory} 持有元数据缓存与 Provider 资源，
     * 在热部署 / 容器反复重载场景下应由应用关闭钩子调用本方法释放。
     * 调用后下次 {@link #valid} 会自动重新初始化
     */
    public static void close() {
        synchronized (ValidateUtil.class) {
            if (Objects.nonNull(validatorFactory)) {
                validatorFactory.close();
            }
            validatorFactory = null;
            validator = null;
        }
    }
}
