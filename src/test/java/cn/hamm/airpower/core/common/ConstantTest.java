package cn.hamm.airpower.core.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <h1>通用常量单元测试</h1>
 *
 * @author Hamm.cn
 */
@DisplayName("Constant 通用常量")
class ConstantTest {

    @Test
    @DisplayName("ID 常量应为 id")
    void testId() {
        assertEquals("id", Constant.ID, "Constant.ID 应为 id");
    }

    @Test
    @DisplayName("Constant 禁止外部实例化（私有构造器）")
    void testNotInstantiable() throws NoSuchMethodException {
        Constructor<Constant> constructor = Constant.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()), "Constant 的构造器应为私有");
        assertEquals(1, Constant.class.getDeclaredConstructors().length, "Constant 应仅有一个构造器");
    }
}
