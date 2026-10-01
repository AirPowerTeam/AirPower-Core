package cn.hamm.airpower.core.annotation;

import cn.hamm.airpower.core.RootModel;
import cn.hamm.airpower.core.ValidateUtil;
import cn.hamm.airpower.core.fixture.ValidDemoModel;
import jakarta.validation.Constraint;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>电话验证注解测试</h1>
 *
 * @author Hamm.cn
 */
@DisplayName("电话验证注解测试")
class PhoneTest {

    /**
     * 从指定字段上取出 {@link Phone} 注解实例
     *
     * @param clazz     目标类
     * @param fieldName 字段名
     * @return 注解实例
     */
    private static Phone phoneOf(Class<?> clazz, String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            Phone phone = field.getAnnotation(Phone.class);
            assertNotNull(phone, "字段 " + clazz.getSimpleName() + "#" + fieldName + " 应标注 @Phone");
            return phone;
        } catch (NoSuchFieldException e) {
            throw new AssertionError("未找到字段 " + fieldName, e);
        }
    }

    /**
     * 构造一个除电话字段外都合法的模型
     *
     * @return 合法模型
     */
    private static ValidDemoModel baseModel() {
        return new ValidDemoModel()
                .setName("张三")
                .setCount(1)
                .setGender(1)
                .setCreateName("创建名称");
    }

    /**
     * 一个 {@code mobile = false, tel = false} 的场景，用于覆盖「两种格式都不允许」的分支
     */
    static class NeverPhoneModel extends RootModel<NeverPhoneModel> {
        @Phone(mobile = false, tel = false)
        private String contact;
    }

    @Nested
    @DisplayName("注解元数据")
    class Meta {

        @Test
        @DisplayName("默认属性值")
        void defaultValues() throws NoSuchMethodException {
            assertEquals("不是有效的电话号码", Phone.class.getMethod("message").getDefaultValue(),
                    "message 默认值应与源码一致");
            assertEquals(Boolean.TRUE, Phone.class.getMethod("mobile").getDefaultValue(),
                    "mobile 默认值应为 true");
            assertEquals(Boolean.TRUE, Phone.class.getMethod("tel").getDefaultValue(),
                    "tel 默认值应为 true");
            assertEquals(0, ((Class<?>[]) Phone.class.getMethod("groups").getDefaultValue()).length,
                    "groups 默认为空数组");
            assertEquals(0, ((Class<?>[]) Phone.class.getMethod("payload").getDefaultValue()).length,
                    "payload 默认为空数组");
        }

        @Test
        @DisplayName("标注在字段上时取到的属性值")
        void onField() {
            Phone phone = phoneOf(ValidDemoModel.class, "mobileOrTel");
            assertEquals("不是有效的电话号码", phone.message(), "message 应为默认值");
            assertTrue(phone.mobile(), "mobile 应为默认值 true");
            assertTrue(phone.tel(), "tel 应为默认值 true");
            assertEquals(0, phone.groups().length, "groups 应为空数组");
            assertEquals(0, phone.payload().length, "payload 应为空数组");
        }

        @Test
        @DisplayName("显式指定属性值")
        void explicitValues() {
            Phone onlyMobile = phoneOf(ValidDemoModel.class, "onlyMobile");
            assertTrue(onlyMobile.mobile(), "onlyMobile 的 mobile 应为 true");
            assertFalse(onlyMobile.tel(), "onlyMobile 的 tel 应为 false");

            Phone onlyTel = phoneOf(ValidDemoModel.class, "onlyTel");
            assertFalse(onlyTel.mobile(), "onlyTel 的 mobile 应为 false");
            assertTrue(onlyTel.tel(), "onlyTel 的 tel 应为 true");

            Phone never = phoneOf(NeverPhoneModel.class, "contact");
            assertFalse(never.mobile(), "NeverPhoneModel 的 mobile 应为 false");
            assertFalse(never.tel(), "NeverPhoneModel 的 tel 应为 false");
        }

        @Test
        @DisplayName("运行时保留、可标注在字段和方法上")
        void metaAnnotations() {
            assertEquals(RUNTIME, Phone.class.getAnnotation(Retention.class).value(),
                    "@Phone 应为运行时保留");
            assertArrayEquals(new ElementType[]{FIELD, METHOD},
                    Phone.class.getAnnotation(Target.class).value(),
                    "@Phone 应可标注在字段和方法上");
            assertNotNull(Phone.class.getAnnotation(Documented.class), "@Phone 应被 @Documented 标注");
            assertArrayEquals(new Class<?>[]{Phone.PhoneValidator.class},
                    Phone.class.getAnnotation(Constraint.class).validatedBy(),
                    "验证器实现类应为 Phone.PhoneValidator");
        }
    }

    @Nested
    @DisplayName("PhoneValidator - 只允许手机号（mobile=true, tel=false）")
    class OnlyMobile {

        @Test
        @DisplayName("手机号通过，其他格式不通过")
        void branches() {
            Phone.PhoneValidator validator = new Phone.PhoneValidator();
            validator.initialize(phoneOf(ValidDemoModel.class, "onlyMobile"));
            // 源码未使用 context，传 null 安全
            assertTrue(validator.isValid("13800138000", null), "标准手机号应通过");
            assertTrue(validator.isValid("+8613800138000", null), "带国家码的手机号应通过");
            assertFalse(validator.isValid("010-12345678", null), "座机不应通过只允许手机号的校验");
            assertFalse(validator.isValid("4001234567", null), "400 客服号不应通过只允许手机号的校验");
            assertFalse(validator.isValid("abc", null), "非号码字符串不应通过");
        }

        @Test
        @DisplayName("空值视为通过（需自行做非空校验）")
        void emptyValue() {
            Phone.PhoneValidator validator = new Phone.PhoneValidator();
            validator.initialize(phoneOf(ValidDemoModel.class, "onlyMobile"));
            assertTrue(validator.isValid(null, null), "null 值应视为通过");
            assertTrue(validator.isValid("", null), "空串应视为通过");
            // 源码判断的是 StringUtil.isEmpty（只判 null 与空串），纯空格会继续走号码正则，故不通过
            assertFalse(validator.isValid("   ", null), "源码用 isEmpty 而非 isBlank，纯空格仍走号码分支并判定不通过");
        }
    }

    @Nested
    @DisplayName("PhoneValidator - 只允许座机（mobile=false, tel=true）")
    class OnlyTel {

        @Test
        @DisplayName("座机通过，手机号不通过")
        void branches() {
            Phone.PhoneValidator validator = new Phone.PhoneValidator();
            validator.initialize(phoneOf(ValidDemoModel.class, "onlyTel"));
            assertTrue(validator.isValid("010-12345678", null), "带区号座机应通过");
            assertTrue(validator.isValid("12345678", null), "8 位座机应通过");
            assertTrue(validator.isValid("0755-12345678-123", null), "带分机座机应通过");
            assertFalse(validator.isValid("13800138000", null), "手机号不应通过只允许座机的校验");
            assertFalse(validator.isValid("abc", null), "非号码字符串不应通过");
        }

        @Test
        @DisplayName("空值视为通过")
        void emptyValue() {
            Phone.PhoneValidator validator = new Phone.PhoneValidator();
            validator.initialize(phoneOf(ValidDemoModel.class, "onlyTel"));
            assertTrue(validator.isValid(null, null), "null 值应视为通过");
            assertTrue(validator.isValid("", null), "空串应视为通过");
        }
    }

    @Nested
    @DisplayName("PhoneValidator - 手机或座机均可（默认值）")
    class MobileOrTel {

        @Test
        @DisplayName("两种格式都通过")
        void branches() {
            Phone.PhoneValidator validator = new Phone.PhoneValidator();
            validator.initialize(phoneOf(ValidDemoModel.class, "mobileOrTel"));
            assertTrue(validator.isValid("13800138000", null), "手机号应通过");
            assertTrue(validator.isValid("010-12345678", null), "座机应通过");
            assertTrue(validator.isValid("4001234567", null), "400 客服号应通过");
            assertFalse(validator.isValid("abc", null), "非号码字符串不应通过");
            assertFalse(validator.isValid("1380013800", null), "位数不足的手机号不应通过");
        }

        @Test
        @DisplayName("未调用 initialize 时使用字段初始值（均为 true）")
        void defaultFieldValues() {
            Phone.PhoneValidator validator = new Phone.PhoneValidator();
            assertTrue(validator.isValid("13800138000", null), "未初始化时等价于默认注解：手机号通过");
            assertTrue(validator.isValid("010-12345678", null), "未初始化时等价于默认注解：座机通过");
            assertFalse(validator.isValid("abc", null), "未初始化时等价于默认注解：非法值不通过");
        }

        @Test
        @DisplayName("空值视为通过")
        void emptyValue() {
            Phone.PhoneValidator validator = new Phone.PhoneValidator();
            validator.initialize(phoneOf(ValidDemoModel.class, "mobileOrTel"));
            assertTrue(validator.isValid(null, null), "null 值应视为通过");
            assertTrue(validator.isValid("", null), "空串应视为通过");
        }
    }

    @Nested
    @DisplayName("PhoneValidator - 两种格式都不允许（mobile=false, tel=false）")
    class NeverPhone {

        @Test
        @DisplayName("任何值都视为通过")
        void alwaysPass() {
            Phone.PhoneValidator validator = new Phone.PhoneValidator();
            validator.initialize(phoneOf(NeverPhoneModel.class, "contact"));
            assertTrue(validator.isValid("13800138000", null), "两种格式都关闭时手机号也通过");
            assertTrue(validator.isValid("010-12345678", null), "两种格式都关闭时座机也通过");
            assertTrue(validator.isValid("abc", null), "两种格式都关闭时任意字符串也通过");
            assertTrue(validator.isValid(null, null), "两种格式都关闭时 null 也通过");
            assertTrue(validator.isValid("", null), "两种格式都关闭时空串也通过");
        }
    }

    @Nested
    @DisplayName("与 ValidateUtil.valid 集成校验")
    class Integration {

        @Test
        @DisplayName("只允许手机号的字段")
        void onlyMobileField() {
            assertDoesNotThrow(() -> ValidateUtil.valid(baseModel().setOnlyMobile("13800138000")),
                    "手机号应通过集成校验");
            assertDoesNotThrow(() -> ValidateUtil.valid(baseModel().setOnlyMobile(null)),
                    "null 值应通过集成校验（非空需自行校验）");
            assertDoesNotThrow(() -> ValidateUtil.valid(baseModel().setOnlyMobile("")),
                    "空串应通过集成校验（非空需自行校验）");
            ValidationException e = assertThrows(ValidationException.class,
                    () -> ValidateUtil.valid(baseModel().setOnlyMobile("010-12345678")),
                    "座机应触发校验失败");
            assertEquals("不是有效的电话号码", e.getMessage(), "异常消息应为 @Phone 的默认 message");
        }

        @Test
        @DisplayName("只允许座机的字段")
        void onlyTelField() {
            assertDoesNotThrow(() -> ValidateUtil.valid(baseModel().setOnlyTel("010-12345678")),
                    "座机应通过集成校验");
            ValidationException e = assertThrows(ValidationException.class,
                    () -> ValidateUtil.valid(baseModel().setOnlyTel("13800138000")),
                    "手机号应触发校验失败");
            assertEquals("不是有效的电话号码", e.getMessage(), "异常消息应为 @Phone 的默认 message");
        }

        @Test
        @DisplayName("手机或座机均可的字段")
        void mobileOrTelField() {
            assertDoesNotThrow(() -> ValidateUtil.valid(baseModel().setMobileOrTel("13800138000")),
                    "手机号应通过集成校验");
            assertDoesNotThrow(() -> ValidateUtil.valid(baseModel().setMobileOrTel("010-12345678")),
                    "座机应通过集成校验");
            ValidationException e = assertThrows(ValidationException.class,
                    () -> ValidateUtil.valid(baseModel().setMobileOrTel("not-a-phone")),
                    "非号码字符串应触发校验失败");
            assertEquals("不是有效的电话号码", e.getMessage(), "异常消息应为 @Phone 的默认 message");
        }
    }
}
