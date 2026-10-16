package cn.hamm.airpower.core.reflect;

import cn.hamm.airpower.core.common.RootModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.annotation.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>文案注解测试</h1>
 *
 * <p>{@code @Description} 是 {@code ReflectUtil.getDescription(...)} 与 CSV 导出表头的唯一来源，
 * 作用目标与保留策略被改动会直接让表头与错误提示退化成字段名。</p>
 *
 * @author Hamm.cn
 */
@DisplayName("@Description 文案注解")
class DescriptionTest {

    @Test
    @DisplayName("value 可从字段、方法与参数上读回")
    void readable() throws NoSuchMethodException, NoSuchFieldException {
        Description onField = DescribedModel.class.getDeclaredField("name").getAnnotation(Description.class);
        assertNotNull(onField, "描述字段应能读取到 @Description");
        assertEquals("描述文案", onField.value(), "字段上的描述文案应一致");

        Description onMethod = DescribedModel.class.getMethod("describe", String.class)
                .getAnnotation(Description.class);
        assertNotNull(onMethod, "方法应能读取到 @Description");
        assertEquals("方法描述", onMethod.value(), "方法上的描述文案应一致");

        Description onParameter = DescribedModel.class.getMethod("describe", String.class)
                .getParameters()[0].getAnnotation(Description.class);
        assertNotNull(onParameter, "参数应能读取到 @Description");
        assertEquals("参数描述", onParameter.value(), "参数上的描述文案应一致");
    }

    @Test
    @DisplayName("可标注在类上，且值可被 getDescription(Class) 取到")
    void onType() {
        assertEquals("模型描述", ReflectUtil.getDescription(DescribedModel.class),
                "类级 @Description 应被 ReflectUtil 读到");
    }

    @Test
    @DisplayName("作用于字段、方法、类型与参数，运行时保留")
    void meta() {
        assertEquals(RetentionPolicy.RUNTIME, Description.class.getAnnotation(Retention.class).value(),
                "@Description 应为运行时保留");
        assertArrayEquals(new ElementType[]{ElementType.FIELD, ElementType.METHOD,
                        ElementType.TYPE, ElementType.PARAMETER},
                Description.class.getAnnotation(Target.class).value(), "@Description 的作用目标应保持不变");
        assertNotNull(Description.class.getAnnotation(Documented.class), "@Description 应进入文档");
        // @Inherited 按 JLS 只对 TYPE 上的注解生效，对 FIELD/METHOD 毫无作用，
        // 标注它只会让人误以为子类字段能继承到父类注解
        assertNull(Description.class.getAnnotation(Inherited.class),
                "@Description 的 @Target 含 TYPE，@Inherited 对它无效，不应标注");
    }

    @Test
    @DisplayName("只有一个 value 成员")
    void singleMember() {
        assertEquals(1, Description.class.getDeclaredMethods().length, "@Description 只应有 value 一个成员方法");
        assertEquals(String.class, Description.class.getDeclaredMethods()[0].getReturnType(),
                "value 应为 String");
    }

    /**
     * 样例模型
     */
    @Description("模型描述")
    static class DescribedModel extends RootModel {
        /**
         * 名称
         */
        @Description("描述文案")
        private String name;

        /**
         * 描述方法
         *
         * @param name 名称
         * @return 描述
         */
        @Description("方法描述")
        public String describe(@Description("参数描述") String name) {
            return name;
        }
    }
}
