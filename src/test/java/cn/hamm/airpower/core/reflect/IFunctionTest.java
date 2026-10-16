package cn.hamm.airpower.core.reflect;

import cn.hamm.airpower.core.fixture.Gender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h2>可序列化函数接口测试</h2>
 *
 * @author Hamm.cn
 */
@DisplayName("IFunction 可序列化函数接口")
class IFunctionTest {

    @Test
    @DisplayName("是函数式接口，可由方法引用实现")
    void functional() {
        IFunction<Gender, Integer> byKey = Gender::getKey;
        assertEquals(1, byKey.apply(Gender.MALE), "按 key 取值应一致");
    }

    @Test
    @DisplayName("同时是 Function 与 Serializable")
    void hierarchy() {
        assertTrue(Function.class.isAssignableFrom(IFunction.class), "IFunction 应继承 Function");
        assertTrue(Serializable.class.isAssignableFrom(IFunction.class), "IFunction 应继承 Serializable");
        assertNotNull(IFunction.class.getAnnotation(FunctionalInterface.class), "应标注 @FunctionalInterface");
    }

    @Test
    @DisplayName("可作为普通 Function 参与 Stream 运算")
    void stream() {
        IFunction<Gender, String> byLabel = Gender::getLabel;
        assertEquals(List.of("男", "女"), List.of(Gender.MALE, Gender.FEMALE).stream()
                .map((Function<Gender, String>) byLabel)
                .toList(), "应能按 label 依次取出字典描述");
    }

    @Test
    @DisplayName("Lambda 实现同样可用")
    void lambda() {
        IFunction<Gender, Boolean> isMale = gender -> gender.equalsKey(1);
        assertInstanceOf(Boolean.class, isMale.apply(Gender.MALE), "应返回布尔结果");
        assertTrue(isMale.apply(Gender.MALE), "男字典的 key 判定应为真");
    }

    @Test
    @DisplayName("必须可序列化：DictionaryUtil 的字典列表要靠它反查方法名")
    void serializable() throws Exception {
        IFunction<Gender, Integer> byKey = Gender::getKey;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(buffer)) {
            out.writeObject(byKey);
        }
        Object restored;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray()))) {
            restored = in.readObject();
        }
        assertInstanceOf(IFunction.class, restored, "序列化后仍应是 IFunction");
        assertEquals(Gender.MALE.getKey(),
                ((IFunction<Gender, Integer>) restored).apply(Gender.MALE), "反序列化后仍可正常取值");
    }
}
