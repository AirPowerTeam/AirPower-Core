package cn.hamm.airpower.core.desensitize;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.FIELD;
import static org.junit.jupiter.api.Assertions.*;

/**
 * <h2>字段脱敏注解测试</h2>
 *
 * <p>{@code @Desensitize} 的默认值与 {@code replace} 开关共同决定
 * {@code RootModel.desensitize()} 的输出；{@code head} / {@code tail} 只是下限，
 * 会被 {@link DesensitizeType} 自带的保留位数抬高。</p>
 *
 * @author Hamm.cn
 */
@DisplayName("@Desensitize 字段脱敏注解")
class DesensitizeTest {

    @Test
    @DisplayName("未显式配置的成员使用默认值 head=0 tail=0 symbol=* replace=false")
    void defaults() throws NoSuchFieldException {
        Desensitize desensitize = MarkedModel.class.getDeclaredField("mobile")
                .getAnnotation(Desensitize.class);
        assertNotNull(desensitize, "字段应能读取到 @Desensitize");
        assertEquals(DesensitizeType.MOBILE, desensitize.value(), "脱敏类型应一致");
        assertEquals(0, desensitize.head(), "默认头部保留位数应为 0");
        assertEquals(0, desensitize.tail(), "默认尾部保留位数应为 0");
        assertEquals("*", desensitize.symbol(), "默认脱敏符号应为 *");
        assertFalse(desensitize.replace(), "默认不应整体替换");
    }

    @Test
    @DisplayName("显式配置的成员全部可读回")
    void custom() throws NoSuchFieldException {
        Desensitize desensitize = MarkedModel.class.getDeclaredField("secret")
                .getAnnotation(Desensitize.class);
        assertNotNull(desensitize, "字段应能读取到 @Desensitize");
        assertEquals(DesensitizeType.CUSTOM, desensitize.value(), "脱敏类型应为自定义");
        assertEquals(2, desensitize.head(), "头部保留位数应一致");
        assertEquals(3, desensitize.tail(), "尾部保留位数应一致");
        assertEquals("#", desensitize.symbol(), "自定义符号应一致");
        assertTrue(desensitize.replace(), "应整体替换为符号");
    }

    @Test
    @DisplayName("未标注的字段不应凭空出现 @Desensitize")
    void absent() throws NoSuchFieldException {
        assertNull(MarkedModel.class.getDeclaredField("name").getAnnotation(Desensitize.class),
                "未标注的字段不应出现 @Desensitize");
    }

    @Test
    @DisplayName("仅作用于字段且运行时保留")
    void meta() {
        assertEquals(RetentionPolicy.RUNTIME, Desensitize.class.getAnnotation(Retention.class).value(),
                "@Desensitize 应为运行时保留");
        assertArrayEquals(new ElementType[]{FIELD},
                Desensitize.class.getAnnotation(Target.class).value(), "@Desensitize 只应作用于字段");
        assertNotNull(Desensitize.class.getAnnotation(Documented.class),
                "@Desensitize 应进入文档");
    }

    @Test
    @DisplayName("value 没有默认值，缺失时编译期即报错")
    void valueRequired() throws NoSuchMethodException {
        assertNull(Desensitize.class.getDeclaredMethod("value").getDefaultValue(),
                "@Desensitize 的 value 是必填项，不应有默认值");
        assertNotNull(Desensitize.class.getDeclaredMethod("head").getDefaultValue(),
                "head 应有默认值");
        assertNotNull(Desensitize.class.getDeclaredMethod("tail").getDefaultValue(),
                "tail 应有默认值");
        assertNotNull(Desensitize.class.getDeclaredMethod("symbol").getDefaultValue(),
                "symbol 应有默认值");
        assertEquals(false, Desensitize.class.getDeclaredMethod("replace").getDefaultValue(),
                "replace 的默认值应为 false");
    }

    /**
     * 样例模型
     */
    static class MarkedModel {
        /**
         * 名称
         */
        private String name;

        /**
         * 手机号
         */
        @Desensitize(DesensitizeType.MOBILE)
        private String mobile;

        /**
         * 密钥
         */
        @Desensitize(value = DesensitizeType.CUSTOM, head = 2, tail = 3, symbol = "#", replace = true)
        private String secret;
    }
}
