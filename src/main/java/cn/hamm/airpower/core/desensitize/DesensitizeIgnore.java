package cn.hamm.airpower.core.desensitize;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <h2>此接口返回数据不脱敏</h2>
 *
 * @author Hamm.cn
 * @see Desensitize
 */
@Target(METHOD)
@Retention(RUNTIME)
public @interface DesensitizeIgnore {
}
