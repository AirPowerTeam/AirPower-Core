package cn.hamm.airpower.core;

import cn.hamm.airpower.core.exception.ServiceException;
import cn.hamm.airpower.core.interfaces.IEntity;
import cn.hamm.airpower.core.interfaces.ITree;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>TreeUtil 单元测试</h1>
 *
 * @author Hamm.cn
 */
class TreeUtilTest {

    /**
     * 测试用的树节点实体
     */
    static class TreeNode implements ITree<TreeNode> {
        private Long id;
        private Long parentId;
        private List<TreeNode> children;
        private String name;

        TreeNode(Long id, Long parentId, String name) {
            this.id = id;
            this.parentId = parentId;
            this.name = name;
            this.children = new ArrayList<>();
        }

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public TreeNode setId(Long id) {
            this.id = id;
            return this;
        }

        @Override
        public Long getParentId() {
            return parentId;
        }

        @Override
        public TreeNode setParentId(Long parentId) {
            this.parentId = parentId;
            return this;
        }

        @Override
        public List<TreeNode> getChildren() {
            return children;
        }

        @Override
        public TreeNode setChildren(List<TreeNode> children) {
            this.children = children;
            return this;
        }

        String getName() {
            return name;
        }
    }

    // ==================== buildTreeList 方法测试 ====================

    @Test
    void testBuildTreeListWithEmptyList() {
        List<TreeNode> list = Collections.emptyList();
        List<TreeNode> result = TreeUtil.buildTreeList(list);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testBuildTreeListWithSingleNode() {
        List<TreeNode> list = Collections.singletonList(
                new TreeNode(1L, 0L, "Root")
        );
        List<TreeNode> result = TreeUtil.buildTreeList(list);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals("Root", result.get(0).getName());
        assertTrue(result.get(0).getChildren().isEmpty());
    }

    @Test
    void testBuildTreeListWithSimpleHierarchy() {
        // 构建简单的树结构：
        // Root (id=1, parentId=0)
        //   Child1 (id=2, parentId=1)
        //   Child2 (id=3, parentId=1)
        List<TreeNode> list = Arrays.asList(
                new TreeNode(1L, 0L, "Root"),
                new TreeNode(2L, 1L, "Child1"),
                new TreeNode(3L, 1L, "Child2")
        );
        List<TreeNode> result = TreeUtil.buildTreeList(list);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(2, result.get(0).getChildren().size());
        assertEquals("Child1", result.get(0).getChildren().get(0).getName());
        assertEquals("Child2", result.get(0).getChildren().get(1).getName());
    }

    @Test
    void testBuildTreeListWithDeepHierarchy() {
        // 构建深层树结构：
        // Root (id=1, parentId=0)
        //   Child (id=2, parentId=1)
        //     GrandChild (id=3, parentId=2)
        List<TreeNode> list = Arrays.asList(
                new TreeNode(1L, 0L, "Root"),
                new TreeNode(2L, 1L, "Child"),
                new TreeNode(3L, 2L, "GrandChild")
        );
        List<TreeNode> result = TreeUtil.buildTreeList(list);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(1, result.get(0).getChildren().size());
        assertEquals(2L, result.get(0).getChildren().get(0).getId());
        assertEquals(1, result.get(0).getChildren().get(0).getChildren().size());
        assertEquals(3L, result.get(0).getChildren().get(0).getChildren().get(0).getId());
    }

    @Test
    void testBuildTreeListWithMultipleRoots() {
        // 构建多根树结构：
        // Root1 (id=1, parentId=0)
        // Root2 (id=2, parentId=0)
        List<TreeNode> list = Arrays.asList(
                new TreeNode(1L, 0L, "Root1"),
                new TreeNode(2L, 0L, "Root2")
        );
        List<TreeNode> result = TreeUtil.buildTreeList(list);

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());
    }

    @Test
    void testBuildTreeListWithNodesWithoutParent() {
        // 节点没有父节点（parentId 不存在于列表中）
        List<TreeNode> list = Arrays.asList(
                new TreeNode(1L, 0L, "Root"),
                new TreeNode(2L, 99L, "Orphan") // parentId=99 不存在
        );
        List<TreeNode> result = TreeUtil.buildTreeList(list);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    void testBuildTreeListWithNullParentId() {
        // parentId 为 null 时应该被视为根节点
        TreeNode node = new TreeNode(1L, null, "RootWithNullParent");
        List<TreeNode> list = Collections.singletonList(node);
        List<TreeNode> result = TreeUtil.buildTreeList(list);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    void testBuildTreeListResultIsUnmodifiable() {
        List<TreeNode> list = Collections.singletonList(
                new TreeNode(1L, 0L, "Root")
        );
        List<TreeNode> result = TreeUtil.buildTreeList(list);

        // 返回的列表应该是不可修改的
        assertThrows(UnsupportedOperationException.class, () -> result.add(new TreeNode(2L, 0L, "Another")));
    }

    // ==================== findByParentId 方法测试 ====================

    @Test
    void testFindByParentIdWithValidParentId() {
        Function<Long, List<TreeNode>> function = parentId -> Arrays.asList(
                new TreeNode(1L, parentId, "Child1"),
                new TreeNode(2L, parentId, "Child2")
        );

        List<TreeNode> result = TreeUtil.findByParentId(1L, function);
        assertEquals(2, result.size());
    }

    @Test
    void testFindByParentIdWithNullParentId() {
        Function<Long, List<TreeNode>> function = parentId -> {
            assertEquals(0L, parentId); // null 应该被转换为 ROOT_ID (0)
            return Arrays.asList(
                    new TreeNode(1L, parentId, "Child1")
            );
        };

        List<TreeNode> result = TreeUtil.findByParentId(null, function);
        assertEquals(1, result.size());
    }

    @Test
    void testFindByParentIdWithNullResult() {
        Function<Long, List<TreeNode>> function = parentId -> null;

        List<TreeNode> result = TreeUtil.findByParentId(1L, function);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindByParentIdWithEmptyResult() {
        Function<Long, List<TreeNode>> function = parentId -> Collections.emptyList();

        List<TreeNode> result = TreeUtil.findByParentId(1L, function);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== ensureNoChildrenBeforeDelete 方法测试 ====================

    @Test
    void testEnsureNoChildrenBeforeDeleteWithNoChildren() {
        Function<Long, List<TreeNode>> function = parentId -> Collections.emptyList();

        // 没有子节点，不应该抛出异常
        assertDoesNotThrow(() -> TreeUtil.ensureNoChildrenBeforeDelete(1L, function));
    }

    @Test
    void testEnsureNoChildrenBeforeDeleteWithNullResult() {
        Function<Long, List<TreeNode>> function = parentId -> null;

        // null 结果不应该抛出异常
        assertDoesNotThrow(() -> TreeUtil.ensureNoChildrenBeforeDelete(1L, function));
    }

    @Test
    void testEnsureNoChildrenBeforeDeleteWithChildren() {
        Function<Long, List<TreeNode>> function = parentId -> Collections.singletonList(
                new TreeNode(2L, 1L, "Child")
        );

        // 有子节点，应该抛出 ServiceException
        ServiceException exception = assertThrows(ServiceException.class,
                () -> TreeUtil.ensureNoChildrenBeforeDelete(1L, function));
        assertTrue(exception.getMessage().contains("无法删除含有下级的数据"));
    }

    // ==================== getChildrenIdList 方法测试 ====================

    @Test
    void testGetChildrenIdListWithNoChildren() {
        Function<Long, List<TreeNode>> function = parentId -> Collections.emptyList();

        Set<Long> result = TreeUtil.getChildrenIdList(1L, function);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetChildrenIdListWithNullResult() {
        Function<Long, List<TreeNode>> function = parentId -> null;

        Set<Long> result = TreeUtil.getChildrenIdList(1L, function);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetChildrenIdListWithDirectChildren() {
        // 模拟：id=1 下有 id=2 和 id=3 两个子节点
        Function<Long, List<TreeNode>> function = parentId -> {
            if (parentId == 1L) {
                return Arrays.asList(
                        new TreeNode(2L, 1L, "Child1"),
                        new TreeNode(3L, 1L, "Child2")
                );
            }
            return Collections.emptyList();
        };

        Set<Long> result = TreeUtil.getChildrenIdList(1L, function);
        assertEquals(2, result.size());
        assertTrue(result.contains(2L));
        assertTrue(result.contains(3L));
    }

    @Test
    void testGetChildrenIdListWithNestedChildren() {
        // 模拟嵌套子节点：
        // 1 -> [2, 3]
        // 2 -> [4]
        // 3 -> []
        // 4 -> []
        Function<Long, List<TreeNode>> function = parentId -> {
            switch (parentId.intValue()) {
                case 1:
                    return Arrays.asList(
                            new TreeNode(2L, 1L, "Child1"),
                            new TreeNode(3L, 1L, "Child2")
                    );
                case 2:
                    return Collections.singletonList(
                            new TreeNode(4L, 2L, "GrandChild")
                    );
                default:
                    return Collections.emptyList();
            }
        };

        Set<Long> result = TreeUtil.getChildrenIdList(1L, function);
        assertEquals(3, result.size());
        assertTrue(result.contains(2L));
        assertTrue(result.contains(3L));
        assertTrue(result.contains(4L));
    }

    // ==================== 复杂场景测试 ====================

    @Test
    void testBuildTreeListWithComplexTree() {
        // 构建复杂树结构：
        // Root (id=1, parentId=0)
        //   Child1 (id=2, parentId=1)
        //     GrandChild1 (id=4, parentId=2)
        //     GrandChild2 (id=5, parentId=2)
        //   Child2 (id=3, parentId=1)
        //     GrandChild3 (id=6, parentId=3)
        List<TreeNode> list = Arrays.asList(
                new TreeNode(1L, 0L, "Root"),
                new TreeNode(2L, 1L, "Child1"),
                new TreeNode(3L, 1L, "Child2"),
                new TreeNode(4L, 2L, "GrandChild1"),
                new TreeNode(5L, 2L, "GrandChild2"),
                new TreeNode(6L, 3L, "GrandChild3")
        );
        List<TreeNode> result = TreeUtil.buildTreeList(list);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(2, result.get(0).getChildren().size()); // Child1, Child2

        TreeNode child1 = result.get(0).getChildren().get(0);
        assertEquals(2L, child1.getId());
        assertEquals(2, child1.getChildren().size()); // GrandChild1, GrandChild2

        TreeNode child2 = result.get(0).getChildren().get(1);
        assertEquals(3L, child2.getId());
        assertEquals(1, child2.getChildren().size()); // GrandChild3
    }

    @Test
    void testBuildTreeListWithUnorderedInput() {
        // 输入顺序打乱
        List<TreeNode> list = Arrays.asList(
                new TreeNode(4L, 2L, "GrandChild1"),
                new TreeNode(2L, 1L, "Child1"),
                new TreeNode(1L, 0L, "Root"),
                new TreeNode(3L, 1L, "Child2")
        );
        List<TreeNode> result = TreeUtil.buildTreeList(list);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(2, result.get(0).getChildren().size());
    }

    @Test
    void testBuildTreeListPreservesAllNodes() {
        // 确保所有节点都被包含在树中
        List<TreeNode> list = Arrays.asList(
                new TreeNode(1L, 0L, "Root"),
                new TreeNode(2L, 1L, "Child1"),
                new TreeNode(3L, 1L, "Child2"),
                new TreeNode(4L, 2L, "GrandChild")
        );
        List<TreeNode> result = TreeUtil.buildTreeList(list);

        // 统计树中的节点数
        int count = countNodes(result);
        assertEquals(4, count);
    }

    /**
     * 递归计算树中的节点数
     */
    private int countNodes(List<TreeNode> nodes) {
        int count = 0;
        for (TreeNode node : nodes) {
            count++;
            count += countNodes(node.getChildren());
        }
        return count;
    }
}
