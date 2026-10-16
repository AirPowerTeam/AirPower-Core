package cn.hamm.airpower.core.desensitize;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <h2>标记字段在 API 输出时自动脱敏</h2>
 *
 * @author Hamm.cn
 * @apiNote 如需标记不脱敏的接口，可使用 {@link DesensitizeIgnore}
 */
@Target(FIELD)
@Retention(RUNTIME)
@Documented
public @interface Desensitize {
    /**
     * 脱敏类型
     */
    DesensitizeType value();

    /**
     * 开始保留位数
     */
    int head() default 0;

    /**
     * 结束保留位数
     */
    int tail() default 0;

    /**
     * 脱敏符号
     *
     * @apiNote 掩码字符
     */
    String symbol() default "*";

    /**
     * 是否整体替换
     *
     * @apiNote {@code false}（默认）按 {@link DesensitizeType} 的 head/tail 做部分掩码，
     * 例如 {@code 138****5678}；{@code true} 则把整个字段值替换为 {@link #symbol()}
     */
    boolean replace() default false;

}
