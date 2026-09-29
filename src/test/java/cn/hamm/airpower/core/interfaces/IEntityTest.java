package cn.hamm.airpower.core.interfaces;

import cn.hamm.airpower.core.TreeUtil;
import cn.hamm.airpower.core.fixture.DemoTree;
import cn.hamm.airpower.core.fixture.Gender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>标准接口测试</h1>
 *
 * <p>覆盖 {@link IEntity}、{@link ITree}、{@link IFunction} 三个 SPI 的结构约束与默认行为。</p>
 *
 * @author Hamm.cn
 */
@DisplayName("接口：IEntity / ITree / IFunction")
class IEntityTest {

    @Nested
    @DisplayName("IEntity 实体接口")
    class Entity {

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
        @DisplayName("实现类可读写 ID，且 setter 链式返回自身")
        void accessors() {
            DemoTree entity = new DemoTree();
            assertNull(entity.getId(), "新建实体的 ID 应为 null");
            assertSame(entity, entity.setId(12L), "setId 应返回自身以支持链式调用");
            assertEquals(12L, entity.getId(), "设置后应能读回 ID");
        }

        @Test
        @DisplayName("ID 允许被置空")
        void nullId() {
            DemoTree entity = new DemoTree().setId(1L);
            entity.setId(null);
            assertNull(entity.getId(), "ID 应允许被置空");
        }
    }

    @Nested
    @DisplayName("ITree 树接口")
    class Tree {

        @Test
        @DisplayName("继承 IEntity 并声明父级与子集读写方法")
        void contract() throws NoSuchMethodException {
            assertTrue(IEntity.class.isAssignableFrom(ITree.class), "ITree 应继承 IEntity");
            assertEquals(Long.class, ITree.class.getMethod("getParentId").getReturnType(),
                    "getParentId 应返回 Long");
            assertEquals(ITree.class, ITree.class.getMethod("setParentId", Long.class).getReturnType(),
                    "setParentId 应返回树实体自身");
            assertEquals(List.class, ITree.class.getMethod("getChildren").getReturnType(),
                    "getChildren 应返回列表");
            assertEquals(ITree.class, ITree.class.getMethod("setChildren", List.class).getReturnType(),
                    "setChildren 应返回树实体自身");
            assertEquals(4, ITree.class.getDeclaredMethods().length, "ITree 应声明 4 个方法");
        }

        @Test
        @DisplayName("父子关系可链式读写")
        void accessors() {
            DemoTree node = new DemoTree().setId(1L).setParentId(0L).setName("根节点");
            DemoTree child = new DemoTree().setId(2L).setParentId(1L).setName("子节点");
            assertSame(node, node.setChildren(List.of(child)), "setChildren 应返回自身");
            assertEquals("根节点", node.getName(), "名称应可读回");
            assertEquals(1, node.getChildren().size(), "子节点数量应为 1");
            assertEquals(1L, node.getChildren().get(0).getParentId(), "子节点父级 ID 应为 1");
        }

        @Test
        @DisplayName("父级 ID 为 null 的节点在建树时按根节点处理")
        void nullParentId() {
            DemoTree root = new DemoTree().setId(1L).setName("根");
            DemoTree orphan = new DemoTree().setId(3L).setName("同为根");
            DemoTree child = new DemoTree().setId(2L).setParentId(1L).setName("子");
            List<DemoTree> tree = TreeUtil.buildTreeList(List.of(root, child, orphan));
            assertEquals(2, tree.size(), "父级为 null 的两个节点都应作为根节点");
            assertEquals(1, tree.get(0).getChildren().size(), "根节点只应有一个子节点");
            assertEquals("子", tree.get(0).getChildren().get(0).getName(), "子节点应挂在根节点下");
        }

        @Test
        @DisplayName("新建实体的子集默认为 null")
        void nullChildren() {
            assertNull(new DemoTree().getChildren(), "新建实体的子集应为 null");
        }
    }

    @Nested
    @DisplayName("IFunction 可序列化函数接口")
    class Function_ {

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
    }
}
