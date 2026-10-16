package cn.hamm.airpower.core.export;

import cn.hamm.airpower.core.common.RootModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;

import static java.lang.annotation.ElementType.METHOD;
import static org.junit.jupiter.api.Assertions.*;

/**
 * <h2>整体暴露注解测试</h2>
 *
 * @author Hamm.cn
 */
@DisplayName("@ExposeAll 整体暴露注解")
class ExposeAllTest {

    @Test
    @DisplayName("类数组可读回，仅作用于方法")
    void readable() throws NoSuchMethodException {
        ExposeAll exposeAll = ExposedModel.class.getMethod("exposed").getAnnotation(ExposeAll.class);
        assertNotNull(exposeAll, "暴露方法应能读取到 @ExposeAll");
        assertEquals(Set.of(ExposedModel.class), Set.of(exposeAll.value()), "暴露的类名应一致");
        assertNull(ExposedModel.class.getMethod("normal").getAnnotation(ExposeAll.class),
                "普通方法不应凭空出现 @ExposeAll");
    }

    @Test
    @DisplayName("可声明多个待暴露的模型")
    void multipleClasses() throws NoSuchMethodException {
        ExposeAll exposeAll = ExposedModel.class.getMethod("exposedAll").getAnnotation(ExposeAll.class);
        assertNotNull(exposeAll, "暴露方法应能读取到 @ExposeAll");
        assertEquals(2, exposeAll.value().length, "应能同时声明两个模型");
        assertEquals(Set.of(ExposedModel.class, AnotherExposedModel.class),
                Set.of(exposeAll.value()), "两个待暴露的模型都应保留");
    }

    @Test
    @DisplayName("运行时保留，只作用于方法")
    void meta() {
        assertEquals(RetentionPolicy.RUNTIME, ExposeAll.class.getAnnotation(Retention.class).value(),
                "@ExposeAll 应为运行时保留");
        assertArrayEquals(new ElementType[]{METHOD},
                ExposeAll.class.getAnnotation(Target.class).value(), "@ExposeAll 只应作用于方法");
    }

    @Test
    @DisplayName("只有一个 value 成员，且为必填（无默认值）")
    void singleRequiredMember() throws NoSuchMethodException {
        assertEquals(1, ExposeAll.class.getDeclaredMethods().length, "@ExposeAll 只应有 value 一个成员方法");
        assertNull(ExposeAll.class.getDeclaredMethod("value").getDefaultValue(),
                "value 是必填项，不应有默认值");
        assertEquals(Class[].class, ExposeAll.class.getDeclaredMethod("value").getReturnType(),
                "value 应为 Class 数组");
    }

    /**
     * 样例模型
     */
    static class ExposedModel extends RootModel {
        /**
         * 暴露单个模型
         *
         * @return 恒为 null
         */
        @ExposeAll(ExposedModel.class)
        public ExposedModel exposed() {
            return null;
        }

        /**
         * 暴露多个模型
         *
         * @return 恒为 null
         */
        @ExposeAll({ExposedModel.class, AnotherExposedModel.class})
        public ExposedModel exposedAll() {
            return null;
        }

        /**
         * 未标注的方法
         *
         * @return 恒为 null
         */
        public ExposedModel normal() {
            return null;
        }
    }

    /**
     * 第二个待暴露的模型
     */
    static class AnotherExposedModel extends RootModel {
    }
}
