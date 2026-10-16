package cn.hamm.airpower.core.tree;

import cn.hamm.airpower.core.common.IEntity;
import cn.hamm.airpower.core.fixture.DemoTree;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>树结构实体标准接口测试</h1>
 *
 * @author Hamm.cn
 */
@DisplayName("ITree 树结构实体标准接口")
class ITreeTest {

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
