package cn.hamm.airpower.core;

import cn.hamm.airpower.core.exception.ServiceException;
import cn.hamm.airpower.core.fixture.DemoTree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.*;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>树结构处理工具类单元测试</h1>
 *
 * @author Hamm.cn
 * @see TreeUtil
 */
@DisplayName("树结构处理工具类 TreeUtil 测试")
class TreeUtilTest {

    /**
     * 三层结构:1 → 2 → 3,10 → 11
     */
    private List<DemoTree> nodes;

    /**
     * 构造一个树节点
     *
     * @param id       节点 ID
     * @param parentId 父级 ID
     * @param name     节点名称
     * @return 树节点
     */
    private static DemoTree node(Long id, Long parentId, String name) {
        return new DemoTree().setId(id).setParentId(parentId).setName(name);
    }

    /**
     * 构造一个按父级 ID 查询子节点的取值函数
     *
     * @param nodes 原始数据
     * @return 取值函数
     */
    private static Function<Long, List<DemoTree>> childLookup(List<DemoTree> nodes) {
        Map<Long, List<DemoTree>> map = new HashMap<>();
        for (DemoTree item : nodes) {
            map.computeIfAbsent(item.getParentId(), key -> new ArrayList<>()).add(item);
        }
        return map::get;
    }

    @BeforeEach
    void setUp() {
        nodes = new ArrayList<>(Arrays.asList(
                node(1L, 0L, "一级A"),
                node(2L, 1L, "二级A"),
                node(3L, 2L, "三级A"),
                node(10L, 0L, "一级B"),
                node(11L, 10L, "二级B")
        ));
    }

    /**
     * <h2>根节点常量</h2>
     */
    @Nested
    @DisplayName("常量 ROOT_ID")
    class RootIdConstant {

        @Test
        @DisplayName("正常:ROOT_ID 应为 0L")
        void rootIdIsZero() {
            assertEquals(0L, TreeUtil.ROOT_ID, "根节点 ID 常量应为 0L");
            assertEquals(Long.valueOf(0L), Long.valueOf(TreeUtil.ROOT_ID), "ROOT_ID 应可安全装箱为 Long");
        }
    }

    /**
     * <h2>构建树结构</h2>
     */
    @Nested
    @DisplayName("buildTreeList 构建树结构")
    class BuildTreeList {

        @Test
        @DisplayName("正常:三层数据应被正确组装为树")
        void buildThreeLevelTree() {
            List<DemoTree> tree = TreeUtil.buildTreeList(nodes);
            assertEquals(2, tree.size(), "根层应有两个节点(1 与 10)");

            DemoTree a = tree.get(0);
            assertEquals(1L, a.getId(), "根层第一个节点 ID 应为 1");
            assertEquals("一级A", a.getName(), "根层第一个节点名称应为 一级A");
            assertNotNull(a.getChildren(), "节点的 children 不应为 null");
            assertEquals(1, a.getChildren().size(), "节点 1 应有 1 个子节点");
            assertEquals(2L, a.getChildren().get(0).getId(), "节点 1 的子节点 ID 应为 2");

            DemoTree b = a.getChildren().get(0);
            assertEquals(1, b.getChildren().size(), "节点 2 应有 1 个子节点");
            assertEquals(3L, b.getChildren().get(0).getId(), "节点 2 的子节点 ID 应为 3");

            DemoTree c = b.getChildren().get(0);
            assertNotNull(c.getChildren(), "叶子节点的 children 也会被设置为空列表而非 null");
            assertTrue(c.getChildren().isEmpty(), "节点 3 是叶子节点,子节点列表应为空");

            DemoTree bRoot = tree.get(1);
            assertEquals(10L, bRoot.getId(), "根层第二个节点 ID 应为 10");
            assertEquals(1, bRoot.getChildren().size(), "节点 10 应有 1 个子节点");
            assertEquals(11L, bRoot.getChildren().get(0).getId(), "节点 10 的子节点 ID 应为 11");
        }

        @Test
        @DisplayName("正常:构建结果是就地修改,返回的仍是入参中的同一批对象")
        void buildTreeListReusesSameInstances() {
            DemoTree original = nodes.get(0);
            assertNull(original.getChildren(), "构建前节点的 children 应为 null");

            List<DemoTree> tree = TreeUtil.buildTreeList(nodes);

            assertNotSame(nodes, tree, "返回的根列表是新建的列表,不是入参本身");
            assertSame(original, tree.get(0), "返回的节点应与入参中的节点是同一个对象");
            assertSame(nodes.get(1), original.getChildren().get(0), "子节点也应复用入参中的对象");
            assertNotNull(original.getChildren(), "入参对象上的 children 已被就地填充");
            assertEquals(1, original.getChildren().size(), "入参对象上的 children 应含 1 个子节点");
            assertEquals(5, nodes.size(), "入参列表本身的长度不应被改变");
        }

        @Test
        @DisplayName("正常:入参为不可变列表时也能正常构建")
        void buildFromImmutableList() {
            List<DemoTree> immutable = List.copyOf(nodes);
            List<DemoTree> tree = assertDoesNotThrow(() -> TreeUtil.buildTreeList(immutable),
                    "入参为不可变列表时源码只读不写,应能正常构建");
            assertEquals(2, tree.size(), "不可变列表同样应构建出 2 个根节点");
        }

        @Test
        @DisplayName("正常:同一父级下的兄弟节点保持入参顺序")
        void keepsSiblingOrder() {
            List<DemoTree> list = new ArrayList<>(Arrays.asList(
                    node(1L, 0L, "父"),
                    node(2L, 1L, "子1"),
                    node(3L, 1L, "子2"),
                    node(4L, 1L, "子3")
            ));
            List<DemoTree> tree = TreeUtil.buildTreeList(list);
            assertEquals(1, tree.size(), "应只有 1 个根节点");
            assertEquals(3, tree.get(0).getChildren().size(), "父节点应有 3 个子节点");
            assertEquals(Arrays.asList(2L, 3L, 4L), tree.get(0).getChildren().stream().map(DemoTree::getId).toList(),
                    "子节点顺序应与入参顺序一致");
        }

        @Test
        @DisplayName("边界:空列表应返回空的不可修改列表")
        void buildFromEmptyList() {
            List<DemoTree> tree = TreeUtil.buildTreeList(new ArrayList<DemoTree>());
            assertTrue(tree.isEmpty(), "空列表应返回空树");
            assertThrows(UnsupportedOperationException.class, () -> tree.add(new DemoTree()),
                    "返回的列表被标注为 @Unmodifiable,不允许新增元素");
        }

        @Test
        @DisplayName("边界:空集合 List.of() 同样返回空树")
        void buildFromListOfEmpty() {
            List<DemoTree> tree = TreeUtil.buildTreeList(List.<DemoTree>of());
            assertTrue(tree.isEmpty(), "List.of() 应返回空树");
        }

        @Test
        @DisplayName("边界:parentId 为 null 的节点按根节点处理")
        void nullParentIdTreatedAsRoot() {
            List<DemoTree> list = new ArrayList<>(Arrays.asList(
                    node(1L, null, "无父节点"),
                    node(2L, 1L, "有父节点")
            ));
            List<DemoTree> tree = TreeUtil.buildTreeList(list);
            assertEquals(1, tree.size(), "parentId 为 null 的节点应被当作根节点");
            assertEquals(1L, tree.get(0).getId(), "parentId 为 null 的节点 ID 应为 1");
            assertEquals(1, tree.get(0).getChildren().size(), "该节点应正常挂载其子节点");
        }

        @Test
        @DisplayName("边界:父级不存在的孤儿节点不会出现在结果中")
        void orphanNodeIsDropped() {
            DemoTree orphan = node(99L, 888L, "孤儿");
            List<DemoTree> list = new ArrayList<>(Arrays.asList(node(1L, 0L, "正常节点"), orphan));

            List<DemoTree> tree = TreeUtil.buildTreeList(list);

            assertEquals(1, tree.size(), "孤儿节点不应出现在结果中");
            assertEquals(1L, tree.get(0).getId(), "结果中只应保留根节点 1");
            assertNull(orphan.getChildren(), "孤儿节点未被遍历,其 children 仍为 null");
        }

        @Test
        @DisplayName("边界:全部为孤儿节点时返回空树")
        void allOrphanNodesReturnEmptyTree() {
            List<DemoTree> list = new ArrayList<>(Arrays.asList(
                    node(1L, 7L, "孤儿1"),
                    node(2L, 8L, "孤儿2")
            ));
            assertTrue(TreeUtil.buildTreeList(list).isEmpty(), "没有根节点时返回空树");
        }

        @Test
        @DisplayName("边界:子节点列表同样不可修改")
        void childrenListIsUnmodifiable() {
            List<DemoTree> tree = TreeUtil.buildTreeList(nodes);
            List<DemoTree> children = tree.get(0).getChildren();
            assertThrows(UnsupportedOperationException.class, () -> children.add(new DemoTree()),
                    "节点的 children 列表也应不可修改");
        }

        @Test
        @DisplayName("边界:入参为 null 时返回空列表")
        void buildFromNullList() {
            assertDoesNotThrow(() -> TreeUtil.buildTreeList(null), "入参为 null 时应返回空列表，不应抛空指针");
            assertEquals(0, TreeUtil.buildTreeList(null).size(), "入参为 null 时结果应为空");
        }

        @Test
        @DisplayName("正常路径:从根可达的环形数据被剪断,不会栈溢出")
        void reachableCycleIsPruned() {
            // root(0) 自身是根；10 的父级被改成 11、11 的父级是 10，
            // 且 10 挂在根下形成 0 -> 10 -> 11 -> 10 的可达环
            DemoTree root = new DemoTree().setId(0L).setParentId(0L);
            DemoTree ten = new DemoTree().setId(10L).setParentId(0L);
            DemoTree eleven = new DemoTree().setId(11L).setParentId(10L);
            ten.setParentId(11L);

            List<DemoTree> tree = assertDoesNotThrow(
                    () -> TreeUtil.buildTreeList(List.of(root, ten, eleven)),
                    "可达的环形数据不应导致栈溢出");
            assertEquals(1, tree.size(), "从根出发的顶层节点应为 1 个");
            assertNotNull(tree.get(0), "根节点应正常返回");
        }

        @Test
        @DisplayName("正常路径:同一节点出现在多个分支下时各自展开")
        void sharedNodeIsExpandedInEachBranch() {
            DemoTree left = new DemoTree().setId(1L).setParentId(0L);
            DemoTree right = new DemoTree().setId(2L).setParentId(0L);
            DemoTree leftChild = new DemoTree().setId(4L).setParentId(1L);
            DemoTree rightChild = new DemoTree().setId(5L).setParentId(2L);

            List<DemoTree> tree = TreeUtil.buildTreeList(List.of(left, right, leftChild, rightChild));

            assertEquals(2, tree.size(), "两个根节点都应出现在结果中");
            assertEquals(1, tree.get(0).getChildren().size(), "左根节点应有 1 个子节点");
            assertEquals(1, tree.get(1).getChildren().size(), "右根节点应有 1 个子节点");
            assertEquals(4L, tree.get(0).getChildren().get(0).getId(), "左根节点挂的是 4");
            assertEquals(5L, tree.get(1).getChildren().get(0).getId(), "右根节点挂的是 5");
        }

        @Test
        @DisplayName("边界:ID 为空的节点按叶子处理,不抛空指针")
        void nullIdNodeTreatedAsLeaf() {
            DemoTree root = new DemoTree().setId(1L).setParentId(0L);
            DemoTree noId = new DemoTree().setId(null).setParentId(1L);

            List<DemoTree> tree = assertDoesNotThrow(() -> TreeUtil.buildTreeList(List.of(root, noId)),
                    "ID 为空的节点不应导致拆箱空指针");
            assertEquals(1, tree.size(), "根节点应正常返回");
            assertEquals(0, tree.get(0).getChildren().get(0).getChildren().size(),
                    "ID 为空的节点按叶子处理");
        }
    }

    /**
     * <h2>按父级 ID 查询子节点</h2>
     */
    @Nested
    @DisplayName("findByParentId 按父级 ID 查询子节点")
    class FindByParentId {

        @Test
        @DisplayName("正常:按父级 ID 返回子节点列表")
        void findByGivenParentId() {
            Function<Long, List<DemoTree>> fn = childLookup(nodes);
            List<DemoTree> children = TreeUtil.findByParentId(1L, fn);
            assertEquals(1, children.size(), "父级 1 应有 1 个子节点");
            assertEquals(2L, children.get(0).getId(), "父级 1 的子节点 ID 应为 2");
        }

        @Test
        @DisplayName("正常:无子节点时返回函数给出的空列表")
        void findWithoutChildren() {
            Function<Long, List<DemoTree>> fn = childLookup(nodes);
            assertTrue(TreeUtil.findByParentId(3L, fn).isEmpty(), "叶子节点 3 没有子节点");
        }

        @Test
        @DisplayName("正常:parentId 为 null 时按 ROOT_ID 查询")
        void nullParentIdFallsBackToRoot() {
            List<Long> queried = new ArrayList<>();
            List<DemoTree> roots = TreeUtil.findByParentId(null, parentId -> {
                queried.add(parentId);
                return List.of(node(1L, 0L, "根"));
            });
            assertEquals(1, queried.size(), "取值函数应被调用 1 次");
            assertEquals(TreeUtil.ROOT_ID, queried.get(0), "parentId 为 null 时应使用 ROOT_ID 查询");
            assertEquals(1, roots.size(), "应返回 1 个根节点");
        }

        @Test
        @DisplayName("正常:直接返回取值函数给出的同一个列表引用")
        void returnsSameReferenceFromFunction() {
            List<DemoTree> children = List.of(node(2L, 1L, "子"));
            List<DemoTree> result = TreeUtil.findByParentId(1L, parentId -> children);
            assertSame(children, result, "函数返回非 null 时应原样返回该列表");
        }

        @Test
        @DisplayName("边界:函数返回 null 时返回空且不可修改的列表")
        void functionReturnsNull() {
            List<DemoTree> result = TreeUtil.findByParentId(1L, parentId -> null);
            assertTrue(result.isEmpty(), "函数返回 null 时应返回空列表");
            assertThrows(UnsupportedOperationException.class, () -> result.add(new DemoTree()),
                    "返回的应是不可修改的空列表(List.of())");
        }
    }

    /**
     * <h2>测试辅助方法</h2>
     */

    /**
     * <h2>删除前校验子节点</h2>
     */
    @Nested
    @DisplayName("ensureNoChildrenBeforeDelete 删除前校验子节点")
    class EnsureNoChildrenBeforeDelete {

        @Test
        @DisplayName("正常:没有子节点时应直接放行")
        void passesWhenNoChildren() {
            assertDoesNotThrow(() -> TreeUtil.ensureNoChildrenBeforeDelete(3L, childLookup(nodes)),
                    "叶子节点没有子节点,应允许删除");
        }

        @Test
        @DisplayName("边界:函数返回 null 时也应直接放行")
        void passesWhenFunctionReturnsNull() {
            assertDoesNotThrow(() -> TreeUtil.<DemoTree>ensureNoChildrenBeforeDelete(1L, parentId -> null),
                    "函数返回 null 时源码直接 return,应允许删除");
        }

        @Test
        @DisplayName("边界:函数返回空列表时也应直接放行")
        void passesWhenFunctionReturnsEmptyList() {
            assertDoesNotThrow(() -> TreeUtil.<DemoTree>ensureNoChildrenBeforeDelete(1L, parentId -> new ArrayList<>()),
                    "函数返回空列表时应允许删除");
        }

        @Test
        @DisplayName("异常:存在子节点时应抛出 ServiceException")
        void throwsWhenHasChildren() {
            ServiceException exception = assertThrows(
                    ServiceException.class,
                    () -> TreeUtil.ensureNoChildrenBeforeDelete(1L, childLookup(nodes)),
                    "存在下级时不允许删除"
            );
            assertEquals("无法删除含有下级的数据，请先删除所有下级！",
                    exception.getMessage(), "异常信息应与源码一致");
        }

        @Test
        @DisplayName("正常:根节点 0L 的校验也遵循相同规则")
        void rootNodeFollowsSameRule() {
            assertDoesNotThrow(() -> TreeUtil.<DemoTree>ensureNoChildrenBeforeDelete(TreeUtil.ROOT_ID,
                            parentId -> List.of()),
                    "根节点没有子节点时应允许删除");
            assertThrows(ServiceException.class,
                    () -> TreeUtil.ensureNoChildrenBeforeDelete(TreeUtil.ROOT_ID, childLookup(nodes)),
                    "根节点存在下级时不允许删除");
        }
    }

    /**
     * <h2>递归收集所有后代 ID</h2>
     */
    @Nested
    @DisplayName("getChildrenIdList 递归收集所有后代 ID")
    class GetChildrenIdList {

        @Test
        @DisplayName("正常:三层数据应收集到所有后代 ID(不含自身)")
        void collectAllDescendants() {
            Set<Long> ids = TreeUtil.getChildrenIdList(1L, childLookup(nodes));
            assertEquals(Set.of(2L, 3L), ids, "节点 1 的所有后代应为 2 与 3,且不含自身");
        }

        @Test
        @DisplayName("正常:从根节点 0L 开始应收集到全部后代")
        void collectFromRoot() {
            Set<Long> ids = TreeUtil.getChildrenIdList(TreeUtil.ROOT_ID, childLookup(nodes));
            assertEquals(Set.of(1L, 2L, 3L, 10L, 11L), ids, "从根节点开始应收集到全部 5 个后代 ID");
        }

        @Test
        @DisplayName("正常:单层数据只收集直接子节点")
        void collectDirectChildrenOnly() {
            Set<Long> ids = TreeUtil.getChildrenIdList(10L, childLookup(nodes));
            assertEquals(Set.of(11L), ids, "节点 10 的后代只有 11");
        }

        @Test
        @DisplayName("边界:叶子节点没有后代时返回空集合")
        void collectFromLeaf() {
            Set<Long> ids = TreeUtil.getChildrenIdList(3L, childLookup(nodes));
            assertTrue(ids.isEmpty(), "叶子节点 3 没有后代,应返回空集合");
        }

        @Test
        @DisplayName("边界:函数返回 null 时返回空集合")
        void functionReturnsNull() {
            Set<Long> ids = TreeUtil.getChildrenIdList(1L, parentId -> null);
            assertTrue(ids.isEmpty(), "函数返回 null 时应返回空集合");
        }

        @Test
        @DisplayName("边界:空数据集合应返回空集合")
        void collectFromEmptyData() {
            assertTrue(TreeUtil.getChildrenIdList(0L, childLookup(new ArrayList<>())).isEmpty(),
                    "没有任何数据时返回空集合");
        }

        @Test
        @DisplayName("边界:多个分支指向同一后代时会自动去重")
        void duplicateDescendantsAreDeduplicated() {
            List<DemoTree> list = new ArrayList<>(Arrays.asList(
                    node(1L, 0L, "父1"),
                    node(2L, 1L, "子"),
                    node(3L, 1L, "父2"),
                    node(2L, 3L, "重复的子")
            ));
            Set<Long> ids = TreeUtil.getChildrenIdList(1L, childLookup(list));
            assertEquals(2, ids.size(), "重复出现的 ID 应被 Set 去重");
            assertTrue(ids.contains(2L), "去重后仍应包含 ID 2");
            assertTrue(ids.contains(3L), "去重后仍应包含 ID 3");
        }

        @Test
        @DisplayName("边界:子节点 ID 为 null 时跳过该节点,但仍收集其子树")
        void nullChildIdIsSkipped() {
            // 唯一的子节点 ID 为 null
            List<DemoTree> onlyNull = new ArrayList<>(Arrays.asList(
                    node(1L, 0L, "父"),
                    node(null, 1L, "无ID子节点")
            ));
            Set<Long> ids = assertDoesNotThrow(() -> TreeUtil.getChildrenIdList(1L, childLookup(onlyNull)),
                    "子节点 ID 为 null 时应跳过该节点，而不是在递归调用处自动拆箱抛 NPE");
            assertTrue(ids.isEmpty(), "唯一的子节点 ID 为 null 时应收集不到任何 ID，实际为：" + ids);

            // 混合场景：null ID 被跳过，正常 ID 照常收集
            List<DemoTree> mixed = new ArrayList<>(Arrays.asList(
                    node(1L, 0L, "父"),
                    node(null, 1L, "无ID子节点"),
                    node(2L, 1L, "正常子节点")
            ));
            Set<Long> mixedIds = TreeUtil.getChildrenIdList(1L, childLookup(mixed));
            assertEquals(Set.of(2L), mixedIds, "null ID 应被跳过，其余正常 ID 不受影响");
        }

        @Test
        @DisplayName("边界:ID 为 null 的节点会中断遍历并告警,当前 API 无法收集其子树")
        void nullIdNodeBreaksTraversal() {
            // 1 -> (无ID) -> 2 -> 3
            // 注意：查询函数以 parentId 为键，ID 为 null 的节点没有可用的键去查它的子节点，
            // 因此挂在它下面的 2、3 无法被收集。这是 API 形态决定的，需要调用方自行保证 ID 非空
            List<DemoTree> data = new ArrayList<>(Arrays.asList(
                    node(1L, 0L, "父"),
                    node(null, 1L, "无ID中间节点"),
                    node(2L, 2L, "无ID节点的下级"),
                    node(3L, 2L, "再下一级")
            ));
            Set<Long> ids = assertDoesNotThrow(() -> TreeUtil.getChildrenIdList(1L, childLookup(data)),
                    "ID 为 null 的节点应被跳过而不是抛异常");
            assertTrue(ids.isEmpty(), "无法确定键时遍历在此中断，实际收集到：" + ids);
        }

        @Test
        @DisplayName("边界:环形数据（A 的父是 B、B 的父是 A）应正常收敛，不发生 StackOverflowError")
        @Timeout(5)
        void cyclicDataDoesNotOverflow() {
            // 互相指向：A(1) 的父是 B(2)，B(2) 的父是 A(1)，并向下挂一条链 C(3) → D(4)
            List<DemoTree> cycle = new ArrayList<>(Arrays.asList(
                    node(1L, 2L, "A的父是B"),
                    node(2L, 1L, "B的父是A"),
                    node(3L, 2L, "B的子节点C"),
                    node(4L, 3L, "C的子节点D")
            ));
            Set<Long> ids = assertDoesNotThrow(() -> TreeUtil.getChildrenIdList(1L, childLookup(cycle)),
                    "已收集过的 ID 不再递归，环形数据应收敛返回而不是抛 StackOverflowError");
            assertEquals(Set.of(1L, 2L, 3L, 4L), ids, "环形数据应一次性收集到全部可达 ID");

            // 自环：节点的父级就是自己
            List<DemoTree> selfLoop = new ArrayList<>(List.of(node(5L, 5L, "自环节点")));
            Set<Long> selfIds = assertDoesNotThrow(() -> TreeUtil.getChildrenIdList(5L, childLookup(selfLoop)),
                    "自环节点也应收敛返回，不应无限递归");
            assertEquals(Set.of(5L), selfIds, "自环节点应只收集到自身 ID 一次");
        }

        @Test
        @DisplayName("正常:返回的是可变的 HashSet")
        void returnsMutableSet() {
            Set<Long> ids = TreeUtil.getChildrenIdList(1L, childLookup(nodes));
            assertInstanceOf(HashSet.class, ids, "源码返回的是 HashSet");
            assertDoesNotThrow(() -> ids.add(666L), "返回的集合未被标注为不可修改,允许新增元素");
            assertEquals(3, ids.size(), "新增后集合中应有 3 个元素");
        }
    }

}
