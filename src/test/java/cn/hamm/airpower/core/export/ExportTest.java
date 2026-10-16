package cn.hamm.airpower.core.export;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>导出列注解测试</h1>
 *
 * <p>{@code @Export} 决定 CSV 的列集合、列顺序与每列的转换方式；
 * {@code sort} 的语义（值大者在前）与 {@code remove} 的排除行为都由
 * {@code ExportUtil.buildExportFieldList} 直接消费。</p>
 *
 * @author Hamm.cn
 */
@DisplayName("@Export 导出列注解")
class ExportTest {

    @Test
    @DisplayName("未显式配置时 value=TEXT sort=0 remove=false")
    void defaults() throws NoSuchFieldException {
        Export export = MarkedModel.class.getDeclaredField("name").getAnnotation(Export.class);
        assertNotNull(export, "字段应能读取到 @Export");
        assertEquals(Export.Type.TEXT, export.value(), "默认导出类型应为 TEXT");
        assertEquals(0, export.sort(), "默认排序应为 0");
        assertFalse(export.remove(), "默认不应移除该列");
    }

    @Test
    @DisplayName("显式配置的类型、排序与移除标记可读回")
    void custom() throws NoSuchFieldException {
        Export export = MarkedModel.class.getDeclaredField("gender").getAnnotation(Export.class);
        assertNotNull(export, "字段应能读取到 @Export");
        assertEquals(Export.Type.DICTIONARY, export.value(), "导出类型应一致");
        assertEquals(9, export.sort(), "排序值应一致");
        assertTrue(export.remove(), "该列应被标记为移除");
    }

    @Test
    @DisplayName("负数排序应可读回（值大的列排在前面，负数只是排最后）")
    void negativeSort() throws NoSuchFieldException {
        Export export = MarkedModel.class.getDeclaredField("negative").getAnnotation(Export.class);
        assertNotNull(export, "字段应能读取到 @Export");
        assertEquals(-5, export.sort(), "排序值应可读回");
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
        assertArrayEquals(new ElementType[]{FIELD, METHOD},
                Export.class.getAnnotation(Target.class).value(), "@Export 的作用目标应保持不变");
    }

    @Test
    @DisplayName("三个成员均有默认值，只有 value 缺省为 TEXT")
    void defaultsOfMembers() throws NoSuchMethodException {
        assertEquals(Export.Type.TEXT, Export.class.getDeclaredMethod("value").getDefaultValue(),
                "value 的默认值应为 TEXT");
        assertEquals(0, Export.class.getDeclaredMethod("sort").getDefaultValue(),
                "sort 的默认值应为 0");
        assertEquals(false, Export.class.getDeclaredMethod("remove").getDefaultValue(),
                "remove 的默认值应为 false");
    }

    /**
     * 样例模型
     */
    static class MarkedModel {
        /**
         * 姓名
         */
        @Export
        private String name;

        /**
         * 性别
         */
        @Export(value = Export.Type.DICTIONARY, sort = 9, remove = true)
        private Integer gender;

        /**
         * 负数排序列
         */
        @Export(value = Export.Type.NUMBER, sort = -5)
        private Double negative;

        /**
         * 未标注的列
         */
        private String remark;
    }
}
