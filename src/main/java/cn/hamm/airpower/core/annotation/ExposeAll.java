package cn.hamm.airpower.core.annotation;

import cn.hamm.airpower.core.RootModel;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <h1>此接口需要整体暴露的类名</h1>
 *
 * @author Hamm.cn
 * @see Meta
 */
@Target(METHOD)
@Retention(RUNTIME)
public @interface ExposeAll {
    /**
     * 需要整体暴露的类
     */
    Class<? extends RootModel<?>>[] value();
}
