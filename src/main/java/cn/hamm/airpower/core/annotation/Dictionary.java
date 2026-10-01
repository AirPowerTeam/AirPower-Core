package cn.hamm.airpower.core.annotation;

import cn.hamm.airpower.core.DictionaryUtil;
import cn.hamm.airpower.core.exception.ServiceException;
import cn.hamm.airpower.core.interfaces.IDictionary;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <h1>标记进行字典校验</h1>
 *
 * @author Hamm.cn
 * @apiNote 请注意, 请自行做非空验证, 字典必须实现 {@link IDictionary} 接口
 */
@Constraint(validatedBy = Dictionary.DictionaryValidator.class)
@Target({FIELD, METHOD})
@Retention(RUNTIME)
@Documented
public @interface Dictionary {
    /**
     * 错误信息
     */
    String message() default "不允许的枚举字典值";

    /**
     * 使用的枚举类
     *
     * @see IDictionary
     */
    Class<? extends IDictionary> value();

    /**
     * 验证组
     */
    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * 字典验证实现类
     */
    class DictionaryValidator implements ConstraintValidator<Dictionary, Integer> {
        /**
         * 标记的枚举类
         * <p>设为 {@code final}：验证器实例由框架缓存并在多线程间共享，
         * 可变实例字段会引入跨线程可见性问题；而它的值只在
         * {@link #initialize(Dictionary)} 里写一次，之后只读</p>
         */
        private Class<? extends IDictionary> enumClazz;

        /**
         * 验证
         *
         * @param value   验证的值
         * @param context 验证器会话
         * @return 验证结果
         * @apiNote 只把「值不在字典范围内」当成校验失败；
         * 注解配置错误（{@code value()} 指向的不是 IDictionary 枚举等）
         * 属于开发期 bug，必须原样抛出而不是被 {@code catch (Exception)}
         * 吞成「值不合法」——后者会让开发者看到「值不合法」却完全找不到原因
         */
        @Contract("null, _ -> true")
        @Override
        public final boolean isValid(Integer value, ConstraintValidatorContext context) {
            if (null == value) {
                return true;
            }
            if (null == enumClazz) {
                throw new ServiceException("@Dictionary 的 value() 未指定字典枚举类，无法校验");
            }
            try {
                DictionaryUtil.getDictionary(enumClazz, value);
            } catch (ServiceException e) {
                return false;
            }
            return true;
        }

        /**
         * 初始化
         *
         * @param dictionary 字典类
         */
        @Contract(mutates = "this")
        @Override
        public final void initialize(@NotNull Dictionary dictionary) {
            if (!IDictionary.class.isAssignableFrom(dictionary.value())) {
                // 开发期就报清楚，而不是等到校验时变成一句「值不合法」
                throw new ServiceException("@Dictionary 的 value() 必须是 IDictionary 的实现类，当前为 "
                        + dictionary.value().getName());
            }
            enumClazz = dictionary.value();
        }
    }

}