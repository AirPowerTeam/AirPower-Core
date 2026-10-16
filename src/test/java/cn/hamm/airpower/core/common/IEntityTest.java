package cn.hamm.airpower.core.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <h1>实体标准接口测试</h1>
 *
 * @author Hamm.cn
 */
@DisplayName("IEntity 实体标准接口")
class IEntityTest {

    @Test
    @DisplayName("只有 getId 与 setId 两个抽象方法，且都是 Long 语义")
    void contract() throws NoSuchMethodException {
        Method[] methods = IEntity.class.getDeclaredMethods();
        assertEquals(2, methods.length, "IEntity 应只有 getId 与 setId 两个方法");
        Method getId = IEntity.class.getMethod("getId");
        assertEquals(Long.class, getId.getReturnType(), "getId 应返回 Long");
        Method setId = IEntity.class.getMethod("setId", Long.class);
        assertEquals(IEntity.class, setId.getReturnType(), "setId 应返回实体自身");
    }

    @Test
    @DisplayName("泛型自引用约束：E 必须实现 IEntity")
    void selfTypeBound() {
        // E extends IEntity<E> 保证链式赋值后仍是具体类型，编译期即生效
        assertEquals(1, IEntity.class.getTypeParameters().length, "IEntity 应只有一个类型参数");
        String bound = IEntity.class.getTypeParameters()[0].getBounds()[0].getTypeName();
        assertEquals(IEntity.class.getTypeParameters()[0].getBounds().length, 1, "类型参数只应有一个上界");
        assertTrue(bound.contains("IEntity"), "类型参数的上界应为 IEntity，实际为 " + bound);
    }
}
