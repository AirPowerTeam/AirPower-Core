package cn.hamm.airpower.core.annotation;

import cn.hamm.airpower.core.ValidateUtil;
import cn.hamm.airpower.core.fixture.Gender;
import cn.hamm.airpower.core.fixture.ValidDemoModel;
import jakarta.validation.Constraint;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import cn.hamm.airpower.core.exception.ServiceException;
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
 * <h1>字典验证注解测试</h1>
 *
 * @author Hamm.cn
 */
@DisplayName("字典验证注解测试")
class DictionaryTest {

    /**
     * 从指定字段上取出 {@link Dictionary} 注解实例
     *
     * @param clazz     目标类
     * @param fieldName 字段名
     * @return 注解实例
     */
    private static Dictionary dictionaryOf(Class<?> clazz, String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            Dictionary dictionary = field.getAnnotation(Dictionary.class);
            assertNotNull(dictionary, "字段 " + clazz.getSimpleName() + "#" + fieldName + " 应标注 @Dictionary");
            return dictionary;
        } catch (NoSuchFieldException e) {
            throw new AssertionError("未找到字段 " + fieldName, e);
        }
    }

    /**
     * 构造一个除字典字段外都合法的模型
     *
     * @return 合法模型
     */
    private static ValidDemoModel baseModel() {
        return new ValidDemoModel()
                .setName("张三")
                .setCount(1)
                .setOnlyMobile("13800138000")
                .setOnlyTel("010-12345678")
                .setMobileOrTel("13800138000")
                .setCreateName("创建名称");
    }

    @Nested
    @DisplayName("注解元数据")
    class Meta {

        @Test
        @DisplayName("默认属性值")
        void defaultValues() throws NoSuchMethodException {
            assertEquals("不允许的枚举字典值", Dictionary.class.getMethod("message").getDefaultValue(),
                    "message 默认值应与源码一致");
            assertEquals(0, ((Class<?>[]) Dictionary.class.getMethod("groups").getDefaultValue()).length,
                    "groups 默认为空数组");
            assertEquals(0, ((Class<?>[]) Dictionary.class.getMethod("payload").getDefaultValue()).length,
                    "payload 默认为空数组");
            assertNotNull(Dictionary.class.getMethod("value"), "value 是必填属性");
            assertNull(Dictionary.class.getMethod("value").getDefaultValue(), "value 没有默认值，必须显式指定");
        }

        @Test
        @DisplayName("标注在字段上时取到的属性值")
        void onField() {
            Dictionary dictionary = dictionaryOf(ValidDemoModel.class, "gender");
            assertEquals("不允许的枚举字典值", dictionary.message(), "message 应为默认值");
            assertEquals(Gender.class, dictionary.value(), "value 应为标注时指定的字典枚举类");
            assertEquals(0, dictionary.groups().length, "groups 应为空数组");
            assertEquals(0, dictionary.payload().length, "payload 应为空数组");
        }

        @Test
        @DisplayName("运行时保留、可标注在字段和方法上")
        void metaAnnotations() {
            assertEquals(RUNTIME, Dictionary.class.getAnnotation(Retention.class).value(),
                    "@Dictionary 应为运行时保留");
            assertArrayEquals(new ElementType[]{FIELD, METHOD},
                    Dictionary.class.getAnnotation(Target.class).value(),
                    "@Dictionary 应可标注在字段和方法上");
            assertNotNull(Dictionary.class.getAnnotation(Documented.class), "@Dictionary 应被 @Documented 标注");
            assertArrayEquals(new Class<?>[]{Dictionary.DictionaryValidator.class},
                    Dictionary.class.getAnnotation(Constraint.class).validatedBy(),
                    "验证器实现类应为 Dictionary.DictionaryValidator");
        }
    }

    @Nested
    @DisplayName("DictionaryValidator - 字典值分支")
    class Validator {


        @Test
        @DisplayName("字典中存在的值校验通过")
        void existsInDictionary() {
            Dictionary.DictionaryValidator validator = new Dictionary.DictionaryValidator();
            validator.initialize(dictionaryOf(ValidDemoModel.class, "gender"));
            // 源码未使用 context，传 null 安全
            assertTrue(validator.isValid(0, null), "UNKNOWN(0) 应校验通过");
            assertTrue(validator.isValid(1, null), "MALE(1) 应校验通过");
            assertTrue(validator.isValid(2, null), "FEMALE(2) 应校验通过");
        }

        @Test
        @DisplayName("字典中不存在的值校验失败")
        void notExistsInDictionary() {
            Dictionary.DictionaryValidator validator = new Dictionary.DictionaryValidator();
            validator.initialize(dictionaryOf(ValidDemoModel.class, "gender"));
            assertFalse(validator.isValid(3, null), "超出字典范围的值应校验失败");
            assertFalse(validator.isValid(-1, null), "负数应校验失败");
            assertFalse(validator.isValid(Integer.MAX_VALUE, null), "超大值应校验失败");
        }

        @Test
        @DisplayName("null 值视为通过（需自行做非空校验）")
        void nullValue() {
            Dictionary.DictionaryValidator validator = new Dictionary.DictionaryValidator();
            validator.initialize(dictionaryOf(ValidDemoModel.class, "gender"));
            assertTrue(validator.isValid(null, null), "null 值应视为通过");
        }

        @Test
        @DisplayName("未调用 initialize 时应暴露配置错误而不是谎报「值不合法」")
        void withoutInitialize() {
            Dictionary.DictionaryValidator validator = new Dictionary.DictionaryValidator();
            // 原来的 catch (Exception) 会把「字典类没配」也吞成 false，
            // 开发者看到「值不合法」却完全找不到真正原因
            assertThrows(ServiceException.class, () -> validator.isValid(1, null),
                    "未 initialize 属于注解配置错误，必须原样抛出而不是返回 false");
            assertTrue(validator.isValid(null, null), "null 值仍视为通过，不依赖初始化");
        }
    }

    @Nested
    @DisplayName("与 ValidateUtil.valid 集成校验")
    class Integration {

        @Test
        @DisplayName("字典中存在的值不抛异常")
        void validDictionaryValue() {
            assertDoesNotThrow(() -> ValidateUtil.valid(baseModel().setGender(0)), "UNKNOWN(0) 应通过集成校验");
            assertDoesNotThrow(() -> ValidateUtil.valid(baseModel().setGender(1)), "MALE(1) 应通过集成校验");
            assertDoesNotThrow(() -> ValidateUtil.valid(baseModel().setGender(2)), "FEMALE(2) 应通过集成校验");
        }

        @Test
        @DisplayName("null 值不抛异常（非空需自行校验）")
        void nullDictionaryValue() {
            assertDoesNotThrow(() -> ValidateUtil.valid(baseModel().setGender(null)), "null 值应通过集成校验");
        }

        @Test
        @DisplayName("字典中不存在的值抛 ValidationException 且消息为默认 message")
        void invalidDictionaryValue() {
            ValidationException e = assertThrows(ValidationException.class,
                    () -> ValidateUtil.valid(baseModel().setGender(99)),
                    "非法字典值应触发校验失败");
            assertEquals("不允许的枚举字典值", e.getMessage(), "异常消息应为 @Dictionary 的默认 message");
        }
    }
}
