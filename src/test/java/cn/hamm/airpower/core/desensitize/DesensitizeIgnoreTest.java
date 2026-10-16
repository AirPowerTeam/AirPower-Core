package cn.hamm.airpower.core.desensitize;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.METHOD;
import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>不脱敏标记注解测试</h1>
 *
 * @author Hamm.cn
 */
@DisplayName("@DesensitizeIgnore 不脱敏标记注解")
class DesensitizeIgnoreTest {

    @Test
    @DisplayName("方法上的 @DesensitizeIgnore 可读回")
    void readable() throws NoSuchMethodException {
        assertNotNull(ExposedModel.class.getMethod("exposed").getAnnotation(DesensitizeIgnore.class),
                "暴露方法应能读取到 @DesensitizeIgnore");
        assertNull(ExposedModel.class.getMethod("normal").getAnnotation(DesensitizeIgnore.class),
                "普通方法不应凭空出现 @DesensitizeIgnore");
    }

    @Test
    @DisplayName("仅作用于方法且运行时保留")
    void meta() {
        assertEquals(RetentionPolicy.RUNTIME,
                DesensitizeIgnore.class.getAnnotation(Retention.class).value(),
                "@DesensitizeIgnore 应为运行时保留");
        assertArrayEquals(new ElementType[]{METHOD},
                DesensitizeIgnore.class.getAnnotation(Target.class).value(),
                "@DesensitizeIgnore 只应作用于方法");
        assertNull(DesensitizeIgnore.class.getAnnotation(Inherited.class),
                "@DesensitizeIgnore 只作用于方法，@Inherited 对它无效，不应标注");
    }

    @Test
    @DisplayName("无成员方法，是纯标记注解")
    void markerOnly() {
        assertEquals(0, DesensitizeIgnore.class.getDeclaredMethods().length,
                "@DesensitizeIgnore 不应有成员方法");
    }

    /**
     * 样例模型
     */
    static class ExposedModel {
        /**
         * 不脱敏的接口
         *
         * @return 恒为 null
         */
        @DesensitizeIgnore
        public String exposed() {
            return null;
        }

        /**
         * 正常脱敏的接口
         *
         * @return 恒为 null
         */
        public String normal() {
            return null;
        }
    }
}
