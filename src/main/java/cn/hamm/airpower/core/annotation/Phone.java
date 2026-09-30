package cn.hamm.airpower.core.annotation;

import cn.hamm.airpower.core.StringUtil;
import cn.hamm.airpower.core.ValidateUtil;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <h1>标记电话验证 座机或手机</h1>
 *
 * @author Hamm.cn
 * @apiNote 请注意，请自行做非空验证
 */
@Constraint(validatedBy = Phone.PhoneValidator.class)
@Target({FIELD, METHOD})
@Retention(RUNTIME)
@Documented
public @interface Phone {
    /**
     * 错误信息
     */
    String message() default "不是有效的电话号码";

    /**
     * 验证组
     */
    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * 是否允许手机号格式
     */
    boolean mobile() default true;

    /**
     * 是否允许座机电话格式
     */
    boolean tel() default true;

    /**
     * 电话验证实现类
     */
    class PhoneValidator implements ConstraintValidator<Phone, String> {
        /**
         * 是否座机
         */
        private boolean tel = true;

        /**
         * 是否手机号
         */
        private boolean mobile = true;

        /**
         * 验证
         *
         * @param value   验证的值
         * @param context 验证会话
         * @return 验证结果
         */
        @Override
        public final boolean isValid(String value, ConstraintValidatorContext context) {
            // 空值交给 @NotNull 一类的注解处理，本注解只管格式
            if (StringUtil.isEmpty(value)) {
                return true;
            }
            if (!mobile && !tel) {
                // 两种格式都不允许时形同放弃校验，直接放行
                return true;
            }
            if (!mobile) {
                return ValidateUtil.isTelPhone(value);
            }
            if (!tel) {
                return ValidateUtil.isMobilePhone(value);
            }
            return ValidateUtil.isMobilePhone(value) || ValidateUtil.isTelPhone(value);
        }

        /**
         * 初始化
         *
         * @param phone 电话验证注解
         */
        @Override
        public final void initialize(@NotNull Phone phone) {
            mobile = phone.mobile();
            tel = phone.tel();
        }
    }

}