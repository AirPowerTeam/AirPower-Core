package cn.hamm.airpower.core.constant;

import org.jetbrains.annotations.Contract;

import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

/**
 * <h1>正则常量</h1>
 *
 * @author Hamm.cn
 */
public class PatternConstant {
    /**
     * 数字（可带负号与小数）
     */
    public static final Pattern NUMBER = compile("^-?\\d+(\\.\\d+)?$");

    /**
     * 字母
     */
    public static final Pattern LETTER = compile("^[A-Za-z]+$");

    /**
     * 整数
     */
    public static final Pattern INTEGER = compile("^-?\\d+$");

    /**
     * 邮箱
     *
     * @apiNote 开头的负向前瞻禁止出现连续两个点，且域名至少两段
     */
    public static final Pattern EMAIL = compile(
            "^(?!.*\\.\\.)[a-zA-Z0-9]+([._%+-][a-zA-Z0-9]+)*@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    /**
     * 字母或数字
     */
    public static final Pattern LETTER_OR_NUMBER = compile("^[A-Za-z0-9]+$");

    /**
     * 中文
     */
    public static final Pattern CHINESE = compile("^[\\u4e00-\\u9fff]+$");

    /**
     * 手机
     */
    public static final Pattern MOBILE_PHONE = compile("^(\\+(\\d{1,4}))?1[3-9](\\d{9})$");

    /**
     * 座机电话
     */
    public static final Pattern TEL_PHONE = compile(
            "^(((0\\d{2,3})-)?((\\d{7,8})|(400\\d{7})|(800\\d{7}))(-(\\d{1,4}))?)$"
    );

    /**
     * 普通字符
     *
     * @apiNote 允许 {@code @ # % - _ + \ /}、a-z A-Z 0-9 与汉字。
     * 汉字区间与 {@link #CHINESE} 保持一致（\u4e00-\u9fff）：
     * 原先这里是 \u9fa5，导致 \u9fa6~\u9fff（含大量生僻字与扩展汉字）
     被本正则接受却被 {@link #CHINESE} 拒绝
     */
    public static final Pattern NORMAL_CODE = compile("^[@#%a-zA-Z0-9\\u4e00-\\u9fff_\\-\\\\/+]+$");

    /**
     * 数字或字母
     */
    public static final Pattern NUMBER_OR_LETTER = compile("^[0-9a-zA-Z]+$");

    /**
     * 自然数
     *
     * @apiNote 允许小数（{@code 1.5} 也算），需要纯数字请用 {@link #NATURAL_INTEGER}
     */
    public static final Pattern NATURAL_NUMBER = compile("^[0-9]+(\\.[0-9]+)?$");

    /**
     * 自然整数
     */
    public static final Pattern NATURAL_INTEGER = compile("^[0-9]+$");

    /**
     * 禁止外部实例化
     */
    @Contract(pure = true)
    private PatternConstant() {
    }
}
