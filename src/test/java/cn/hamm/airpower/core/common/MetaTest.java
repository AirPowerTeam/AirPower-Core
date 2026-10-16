package cn.hamm.airpower.core.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>元数据标记注解测试</h1>
 *
 * <p>{@code @Meta} 决定字段是否随响应输出；{@code RootModel.excludeNotMeta()} 在字段与
 * 候选 Getter 上都找不到它时会把字段清空，因此注解的作用目标与运行时保留都是行为前提。</p>
 *
 * @author Hamm.cn
 */
@DisplayName("@Meta 元数据标记注解")
class MetaTest {

    @Test
    @DisplayName("字段上的 @Meta 可读回")
    void readable() throws NoSuchFieldException, NoSuchMethodException {
        assertNotNull(MarkedModel.class.getDeclaredField("name").getAnnotation(Meta.class),
                "字段上的 @Meta 应可读回");
        assertNull(MarkedModel.class.getDeclaredField("remark").getAnnotation(Meta.class),
                "未标注的字段不应凭空出现 @Meta");
    }

    @Test
    @DisplayName("Getter 上的 @Meta 可读回（字段未标注时也生效）")
    void onGetter() throws NoSuchFieldException, NoSuchMethodException {
        assertNull(MarkedModel.class.getDeclaredField("title").getAnnotation(Meta.class),
                "title 只在 Getter 上标注");
        assertNotNull(MarkedModel.class.getMethod("getTitle").getAnnotation(Meta.class),
                "Getter 上的 @Meta 应可读回");
    }

    @Test
    @DisplayName("仅作用于字段与方法，运行时保留")
    void meta() {
        assertEquals(RetentionPolicy.RUNTIME, Meta.class.getAnnotation(Retention.class).value(),
                "@Meta 应为运行时保留");
        assertArrayEquals(new ElementType[]{FIELD, METHOD},
                Meta.class.getAnnotation(Target.class).value(), "@Meta 的作用目标应保持不变");
        assertNotNull(Meta.class.getAnnotation(Documented.class), "@Meta 应进入文档");
    }

    @Test
    @DisplayName("无成员方法，是纯标记注解")
    void markerOnly() {
        assertEquals(0, Meta.class.getDeclaredMethods().length, "@Meta 不应有成员方法");
        assertEquals(3, Meta.class.getDeclaredAnnotations().length,
                "@Meta 上只应有 Documented / Retention / Target 三个元注解");
    }

    /**
     * 样例模型
     */
    static class MarkedModel extends RootModel {
        /**
         * 已标注字段
         */
        @Meta
        private String name;

        /**
         * 未标注字段
         */
        private String remark;

        /**
         * 仅在 Getter 上标注的字段
         */
        private String title;

        /**
         * 标题
         *
         * @return 标题
         */
        @Meta
        public String getTitle() {
            return title;
        }
    }
}
