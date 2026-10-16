package cn.hamm.airpower.core.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.FIELD;
import static org.junit.jupiter.api.Assertions.*;

/**
 * <h2>只读标记注解测试</h2>
 *
 * <p>{@code @ReadOnly} 会被 {@code RootModel.excludeReadOnly()} 清空，只允许标在字段上。</p>
 *
 * @author Hamm.cn
 */
@DisplayName("@ReadOnly 只读标记注解")
class ReadOnlyTest {

    @Test
    @DisplayName("字段上的 @ReadOnly 可读回")
    void readable() throws NoSuchFieldException {
        assertNotNull(MarkedModel.class.getDeclaredField("createTime").getAnnotation(ReadOnly.class),
                "字段上的 @ReadOnly 应可读回");
        assertNull(MarkedModel.class.getDeclaredField("name").getAnnotation(ReadOnly.class),
                "未标注的字段不应凭空出现 @ReadOnly");
    }

    @Test
    @DisplayName("仅作用于字段，运行时保留")
    void meta() {
        assertEquals(RetentionPolicy.RUNTIME, ReadOnly.class.getAnnotation(Retention.class).value(),
                "@ReadOnly 应为运行时保留");
        assertArrayEquals(new ElementType[]{FIELD},
                ReadOnly.class.getAnnotation(Target.class).value(), "@ReadOnly 只应作用于字段");
        assertNotNull(ReadOnly.class.getAnnotation(Documented.class), "@ReadOnly 应进入文档");
    }

    @Test
    @DisplayName("无成员方法，是纯标记注解")
    void markerOnly() {
        assertEquals(0, ReadOnly.class.getDeclaredMethods().length, "@ReadOnly 不应有成员方法");
        assertEquals(3, ReadOnly.class.getDeclaredAnnotations().length,
                "@ReadOnly 上只应有 Documented / Retention / Target 三个元注解");
    }

    /**
     * 样例模型
     */
    static class MarkedModel extends RootModel {
        /**
         * 创建时间
         */
        @ReadOnly
        private Long createTime;

        /**
         * 名称
         */
        private String name;
    }
}
