package cn.hamm.airpower.core.annotation;

import cn.hamm.airpower.core.RootModel;
import cn.hamm.airpower.core.enums.DesensitizeType;
import cn.hamm.airpower.core.fixture.Gender;
import jakarta.validation.Constraint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.annotation.*;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>注解元数据与成员默认值测试</h1>
 *
 * <p>本项目的注解本身没有业务方法，但它们的 {@code @Retention} / {@code @Target} / 默认值
 * 直接决定了框架的运行行为，因此逐个断言，防止被误改。</p>
 *
 * @author Hamm.cn
 */
@DisplayName("注解：元数据与成员默认值")
class AnnotationsTest {

    /**
     * 读取样例模型上的字段注解
     *
     * @param fieldName 字段名
     * @return 字段
     */
    private static java.lang.reflect.Field field(String fieldName) {
        try {
            return AnnotationModel.class.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("样例模型上不存在字段：" + fieldName, e);
        }
    }

    /**
     * 暴露整个模型的接口方法
     *
     * @return 模型
     */
    @Description("方法描述")
    @ExposeAll(AnnotationModel.class)
    @DesensitizeIgnore
    public AnnotationModel exposedMethod(@Description("参数描述") String name) {
        return null;
    }

    /**
     * 用于读取注解成员的样例模型
     */
    static class AnnotationModel extends RootModel<AnnotationModel> {
        @Description("描述文案")
        private String descriptionField;

        @Desensitize(value = DesensitizeType.MOBILE)
        private String desensitizeField;

        @Desensitize(value = DesensitizeType.CUSTOM, head = 2, tail = 3, symbol = "#", replace = true)
        private String desensitizeAllField;

        @Dictionary(Gender.class)
        private Integer dictionaryField;

        @Export
        private String exportField;

        @Export(value = Export.Type.DICTIONARY, sort = 9, remove = true)
        private String exportAllField;

        @Meta
        private String metaField;

        @ReadOnly
        private String readOnlyField;

        @Phone
        private String phoneField;
    }

    @Nested
    @DisplayName("@Description 文案注解")
    class DescriptionAnnotation {

        @Test
        @DisplayName("value 读取、运行时保留、可用于字段与方法与参数")
        void members() {
            Description onField = field("descriptionField").getAnnotation(Description.class);
            assertNotNull(onField, "描述字段应能读取到 @Description");
            assertEquals("描述文案", onField.value(), "字段上的描述文案应一致");

            Description onMethod;
            try {
                onMethod = AnnotationsTest.class.getMethod("exposedMethod", String.class)
                        .getAnnotation(Description.class);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("样例方法不存在", e);
            }
            assertNotNull(onMethod, "方法应能读取到 @Description");
            assertEquals("方法描述", onMethod.value(), "方法上的描述文案应一致");

            assertEquals(RetentionPolicy.RUNTIME, Description.class.getAnnotation(Retention.class).value(),
                    "@Description 应为运行时保留");
            assertArrayEquals(new ElementType[]{ElementType.FIELD, ElementType.METHOD,
                            ElementType.TYPE, ElementType.PARAMETER},
                    Description.class.getAnnotation(Target.class).value(), "@Description 的作用目标应保持不变");
            // @Inherited 按 JLS 只对 TYPE 上的注解生效，对 FIELD/METHOD 毫无作用，
            // 标注它只会让人误以为子类字段能继承到父类注解
            assertNull(Description.class.getAnnotation(Inherited.class),
                    "@Description 的 @Target 不含 TYPE，@Inherited 对它无效，不应标注");
            assertNotNull(Description.class.getAnnotation(Documented.class), "@Description 应进入文档");
        }
    }

    @Nested
    @DisplayName("@Desensitize 脱敏注解")
    class DesensitizeAnnotation {

        @Test
        @DisplayName("未显式配置的成员使用默认值 head=0 tail=0 symbol=* replace=false")
        void defaults() {
            Desensitize desensitize = field("desensitizeField").getAnnotation(Desensitize.class);
            assertNotNull(desensitize, "字段应能读取到 @Desensitize");
            assertEquals(DesensitizeType.MOBILE, desensitize.value(), "脱敏类型应一致");
            assertEquals(0, desensitize.head(), "默认头部保留位数应为 0");
            assertEquals(0, desensitize.tail(), "默认尾部保留位数应为 0");
            assertEquals("*", desensitize.symbol(), "默认脱敏符号应为 *");
            assertFalse(desensitize.replace(), "默认不应整体替换");
        }

        @Test
        @DisplayName("显式配置的成员全部可读回")
        void custom() {
            Desensitize desensitize = field("desensitizeAllField").getAnnotation(Desensitize.class);
            assertNotNull(desensitize, "字段应能读取到 @Desensitize");
            assertEquals(DesensitizeType.CUSTOM, desensitize.value(), "脱敏类型应为自定义");
            assertEquals(2, desensitize.head(), "头部保留位数应一致");
            assertEquals(3, desensitize.tail(), "尾部保留位数应一致");
            assertEquals("#", desensitize.symbol(), "自定义符号应一致");
            assertTrue(desensitize.replace(), "应整体替换为符号");
        }

        @Test
        @DisplayName("仅可用于字段且运行时保留")
        void meta() {
            assertEquals(RetentionPolicy.RUNTIME, Desensitize.class.getAnnotation(Retention.class).value(),
                    "@Desensitize 应为运行时保留");
            assertArrayEquals(new ElementType[]{ElementType.FIELD},
                    Desensitize.class.getAnnotation(Target.class).value(), "@Desensitize 只应作用于字段");
        }
    }

    @Nested
    @DisplayName("@Export 导出注解")
    class ExportAnnotation {

        @Test
        @DisplayName("未显式配置时 value=TEXT sort=0 remove=false")
        void defaults() {
            Export export = field("exportField").getAnnotation(Export.class);
            assertNotNull(export, "字段应能读取到 @Export");
            assertEquals(Export.Type.TEXT, export.value(), "默认导出类型应为 TEXT");
            assertEquals(0, export.sort(), "默认排序应为 0");
            assertFalse(export.remove(), "默认不应移除该列");
        }

        @Test
        @DisplayName("显式配置的类型、排序与移除标记可读回")
        void custom() {
            Export export = field("exportAllField").getAnnotation(Export.class);
            assertNotNull(export, "字段应能读取到 @Export");
            assertEquals(Export.Type.DICTIONARY, export.value(), "导出类型应一致");
            assertEquals(9, export.sort(), "排序值应一致");
            assertTrue(export.remove(), "该列应被标记为移除");
        }

        @Test
        @DisplayName("列数据类型枚举共 5 种且顺序固定")
        void types() {
            List<Export.Type> types = Arrays.stream(Export.Type.values()).toList();
            assertEquals(5, types.size(), "导出列数据类型应有 5 种");
            assertEquals(Export.Type.TEXT, types.get(0), "第 1 种应为普通文本");
            assertEquals(Export.Type.DATETIME, types.get(1), "第 2 种应为时间日期");
            assertEquals(Export.Type.NUMBER, types.get(2), "第 3 种应为数字");
            assertEquals(Export.Type.DICTIONARY, types.get(3), "第 4 种应为字典");
            assertEquals(Export.Type.BOOLEAN, types.get(4), "第 5 种应为布尔值");
            assertEquals(Export.Type.BOOLEAN, Export.Type.valueOf("BOOLEAN"), "按名称取值应一致");
        }

        @Test
        @DisplayName("可作用于字段与方法且运行时保留")
        void meta() {
            assertEquals(RetentionPolicy.RUNTIME, Export.class.getAnnotation(Retention.class).value(),
                    "@Export 应为运行时保留");
            assertArrayEquals(new ElementType[]{ElementType.FIELD, ElementType.METHOD},
                    Export.class.getAnnotation(Target.class).value(), "@Export 的作用目标应保持不变");
        }
    }

    @Nested
    @DisplayName("@Dictionary 字典注解元数据")
    class DictionaryAnnotation {

        @Test
        @DisplayName("默认消息、验证组与负载为空，字典类可读回")
        void members() {
            Dictionary dictionary = field("dictionaryField").getAnnotation(Dictionary.class);
            assertNotNull(dictionary, "字段应能读取到 @Dictionary");
            assertEquals(Gender.class, dictionary.value(), "字典枚举类应一致");
            assertEquals("不允许的枚举字典值", dictionary.message(), "默认错误信息应保持不变");
            assertEquals(0, dictionary.groups().length, "默认验证组应为空");
            assertEquals(0, dictionary.payload().length, "默认负载应为空");
        }

        @Test
        @DisplayName("标注为 jakarta 约束注解且运行时保留")
        void meta() {
            assertNotNull(Dictionary.class.getAnnotation(Constraint.class), "@Dictionary 应是约束注解");
            assertEquals(RetentionPolicy.RUNTIME, Dictionary.class.getAnnotation(Retention.class).value(),
                    "@Dictionary 应为运行时保留");
        }
    }

    @Nested
    @DisplayName("@Phone 电话注解元数据")
    class PhoneAnnotation {

        @Test
        @DisplayName("默认允许手机与座机，错误信息为默认文案")
        void members() {
            Phone phone = field("phoneField").getAnnotation(Phone.class);
            assertNotNull(phone, "字段应能读取到 @Phone");
            assertTrue(phone.mobile(), "默认应允许手机号");
            assertTrue(phone.tel(), "默认应允许座机");
            assertEquals("不是有效的电话号码", phone.message(), "默认错误信息应保持不变");
            assertEquals(0, phone.groups().length, "默认验证组应为空");
            assertEquals(0, phone.payload().length, "默认负载应为空");
        }

        @Test
        @DisplayName("标注为 jakarta 约束注解且运行时保留")
        void meta() {
            assertNotNull(Phone.class.getAnnotation(Constraint.class), "@Phone 应是约束注解");
            assertEquals(RetentionPolicy.RUNTIME, Phone.class.getAnnotation(Retention.class).value(),
                    "@Phone 应为运行时保留");
        }
    }

    @Nested
    @DisplayName("标记类注解：@Meta、@ReadOnly、@DesensitizeIgnore、@ExposeAll")
    class MarkerAnnotations {

        @Test
        @DisplayName("@Meta 仅作用于字段与方法，运行时保留")
        void meta() {
            assertNotNull(field("metaField").getAnnotation(Meta.class), "元数据字段应能读取到 @Meta");
            assertEquals(RetentionPolicy.RUNTIME, Meta.class.getAnnotation(Retention.class).value(),
                    "@Meta 应为运行时保留");
            assertArrayEquals(new ElementType[]{ElementType.FIELD, ElementType.METHOD},
                    Meta.class.getAnnotation(Target.class).value(), "@Meta 的作用目标应保持不变");
        }

        @Test
        @DisplayName("@ReadOnly 仅作用于字段，运行时保留")
        void readOnly() {
            assertNotNull(field("readOnlyField").getAnnotation(ReadOnly.class), "只读字段应能读取到 @ReadOnly");
            assertEquals(RetentionPolicy.RUNTIME, ReadOnly.class.getAnnotation(Retention.class).value(),
                    "@ReadOnly 应为运行时保留");
            assertArrayEquals(new ElementType[]{ElementType.FIELD},
                    ReadOnly.class.getAnnotation(Target.class).value(), "@ReadOnly 只应作用于字段");
        }

        @Test
        @DisplayName("@DesensitizeIgnore 仅作用于方法且可继承")
        void desensitizeIgnore() {
            try {
                assertNotNull(AnnotationsTest.class.getMethod("exposedMethod", String.class)
                                .getAnnotation(DesensitizeIgnore.class),
                        "暴露方法应能读取到 @DesensitizeIgnore");
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("样例方法不存在", e);
            }
            assertEquals(RetentionPolicy.RUNTIME,
                    DesensitizeIgnore.class.getAnnotation(Retention.class).value(),
                    "@DesensitizeIgnore 应为运行时保留");
            assertArrayEquals(new ElementType[]{ElementType.METHOD},
                    DesensitizeIgnore.class.getAnnotation(Target.class).value(),
                    "@DesensitizeIgnore 只应作用于方法");
            assertNull(DesensitizeIgnore.class.getAnnotation(Inherited.class),
                    "@DesensitizeIgnore 只作用于方法，@Inherited 对它无效，不应标注");
        }

        @Test
        @DisplayName("@ExposeAll 的类数组可读回，仅作用于方法")
        void exposeAll() {
            try {
                ExposeAll exposeAll = AnnotationsTest.class.getMethod("exposedMethod", String.class)
                        .getAnnotation(ExposeAll.class);
                assertNotNull(exposeAll, "暴露方法应能读取到 @ExposeAll");
                assertEquals(Set.of(AnnotationModel.class), Set.of(exposeAll.value()),
                        "暴露的类名应一致");
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("样例方法不存在", e);
            }
            assertEquals(RetentionPolicy.RUNTIME, ExposeAll.class.getAnnotation(Retention.class).value(),
                    "@ExposeAll 应为运行时保留");
            assertArrayEquals(new ElementType[]{ElementType.METHOD},
                    ExposeAll.class.getAnnotation(Target.class).value(), "@ExposeAll 只应作用于方法");
        }

        @Test
        @DisplayName("@Meta / @ReadOnly / @DesensitizeIgnore / @ExposeAll 均无成员方法")
        void noMembers() {
            assertEquals(0, Meta.class.getDeclaredMethods().length, "@Meta 不应有成员方法");
            assertEquals(0, ReadOnly.class.getDeclaredMethods().length, "@ReadOnly 不应有成员方法");
            assertEquals(0, DesensitizeIgnore.class.getDeclaredMethods().length, "@DesensitizeIgnore 不应有成员方法");
            assertEquals(1, ExposeAll.class.getDeclaredMethods().length, "@ExposeAll 只应有 value 一个成员方法");
        }
    }
}
