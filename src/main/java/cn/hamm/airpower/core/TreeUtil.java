package cn.hamm.airpower.core;

import cn.hamm.airpower.core.exception.ServiceException;
import cn.hamm.airpower.core.interfaces.IEntity;
import cn.hamm.airpower.core.interfaces.ITree;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.function.Function;

/**
 * <h1>树结构处理工具类</h1>
 *
 * @author Hamm.cn
 * @apiNote 约定根节点 {@link #ROOT_ID} 为 {@code 0}：父级为 {@code null} 或 {@code 0}
 * 都视为顶级节点；数据不完整时（父级缺失、成环、ID 为空）只告警并剪枝，不抛异常
 */
@Slf4j
public class TreeUtil {
    /**
     * 树递归深度上限
     * <p>超过这个深度就截断并告警。正常业务的树（部门、分类、BOM 工序）远远用不到，
     * 而没有上限时一条脏数据造成的深链就会 StackOverflowError</p>
     */
    private static final int MAX_TREE_DEPTH = 1000;
    /**
     * 根节点 ID
     */
    public static final long ROOT_ID = 0L;

    /**
     * 禁止外部实例化
     */
    @Contract(pure = true)
    private TreeUtil() {
    }

    /**
     * 生成树结构
     *
     * @param list 原始数据列表
     * @param <E>  树节点类型
     * @return 树结构列表（不可修改）
     * @apiNote 返回值只展开从 {@link #ROOT_ID} 可达的节点，父级不在 {@code list} 中的
     * 孤儿节点会被排除
     */
    public static <E extends IEntity<E> & ITree<E>> @Unmodifiable @NotNull List<E> buildTreeList(List<E> list) {
        if (Objects.isNull(list) || list.isEmpty()) {
            return List.of();
        }
        return buildTreeListOptimized(list, ROOT_ID);
    }

    /**
     * 生成指定父级下的树结构
     *
     * @param list     原始数据列表
     * @param parentId 父级 ID
     * @param <E>      树节点类型
     * @return 树结构列表
     * @apiNote 使用 Map 预构建 parentId -> children 映射，将时间复杂度从 O(n²) 优化到 O(n)
     */
    private static <E extends IEntity<E> & ITree<E>> @Unmodifiable @NotNull List<E> buildTreeListOptimized(
            @NotNull List<E> list, long parentId
    ) {
        Map<Long, List<E>> parentMap = new HashMap<>();
        Set<Long> declaredIds = new HashSet<>();
        for (E item : list) {
            if (Objects.isNull(item)) {
                continue;
            }
            if (Objects.nonNull(item.getId())) {
                declaredIds.add(item.getId());
            }
            Long pid = item.getParentId() != null ? item.getParentId() : ROOT_ID;
            parentMap.computeIfAbsent(pid, k -> new ArrayList<>()).add(item);
        }
        warnOrphans(list, declaredIds);
        return buildTreeWithMap(parentMap, parentId);
    }

    /**
     * 找出父级不存在于原始数据中的孤儿节点
     *
     * @param list        原始数据
     * @param declaredIds 数据中出现过的 ID 集合
     * @param <E>         树节点类型
     * @apiNote 孤儿节点永远不会出现在构建结果中，调用方无任何提示，
     * 表现为"数据莫名少了一截"，因此这里显式告警
     */
    private static <E extends IEntity<E> & ITree<E>> void warnOrphans(
            @NotNull List<E> list, @NotNull Set<Long> declaredIds
    ) {
        List<Long> orphanIds = new ArrayList<>();
        for (E item : list) {
            if (Objects.isNull(item)) {
                continue;
            }
            Long pid = item.getParentId();
            if (Objects.isNull(pid) || pid == ROOT_ID) {
                continue;
            }
            if (!declaredIds.contains(pid)) {
                orphanIds.add(item.getId());
            }
        }
        if (!orphanIds.isEmpty()) {
            log.warn("构建树结构时发现 {} 个父级不存在的孤儿节点，已从结果中排除，节点ID={}",
                    orphanIds.size(), orphanIds);
        }
    }

    /**
     * 使用 Map 构建树结构
     *
     * @param parentMap parentId -> children 映射
     * @param parentId  父级 ID
     * @param <E>       树节点类型
     * @return 树结构列表
     */
    private static <E extends IEntity<E> & ITree<E>> @UnmodifiableView @NotNull List<E> buildTreeWithMap(
            @NotNull Map<Long, List<E>> parentMap, long parentId
    ) {
        return buildTreeWithMap(parentMap, parentId, new HashSet<>());
    }

    /**
     * 使用 Map 构建树结构
     *
     * @param parentMap parentId -> children 映射
     * @param parentId  父级 ID
     * @param visiting  当前递归路径上的 ID，用于检测环形数据
     * @param <E>       树节点类型
     * @return 树结构列表
     * @apiNote 父级 ID 构成环且从 {@link #ROOT_ID} 可达时，无环检测会一直递归到
     * {@code StackOverflowError}
     */
    private static <E extends IEntity<E> & ITree<E>> @UnmodifiableView @NotNull List<E> buildTreeWithMap(
            @NotNull Map<Long, List<E>> parentMap, long parentId, @NotNull Set<Long> visiting
    ) {
        return buildTreeWithMap(parentMap, parentId, visiting, 0);
    }

    /**
     * 递归构建树结构（带深度上限）
     *
     * @param parentMap 父级 ID 到子节点列表的映射
     * @param parentId  父级 ID
     * @param visiting  当前递归路径上的 ID 集合
     * @param depth     当前深度
     * @param <E>       树节点类型
     * @return 树结构列表
     * @apiNote {@code visiting} 只拦成环，拦不住深链，故需配合 {@link #MAX_TREE_DEPTH}
     */
    private static <E extends IEntity<E> & ITree<E>> @UnmodifiableView @NotNull List<E> buildTreeWithMap(
            @NotNull Map<Long, List<E>> parentMap, long parentId,
            @NotNull Set<Long> visiting, int depth
    ) {
        if (depth > MAX_TREE_DEPTH) {
            log.warn("构建树结构递归深度超过上限({})，已在 parentId={} 处截断", MAX_TREE_DEPTH, parentId);
            return Collections.emptyList();
        }
        List<E> children = parentMap.getOrDefault(parentId, Collections.emptyList());
        List<E> result = new ArrayList<>(children.size());
        for (E child : children) {
            Long childId = child.getId();
            // ID 为空的节点无法参与后续查找，按叶子节点处理
            if (Objects.isNull(childId)) {
                result.add(child.setChildren(List.of()));
                continue;
            }
            if (!visiting.add(childId)) {
                // 正在展开的路径上再次出现同一个 ID，说明数据成环，剪断该分支
                log.warn("构建树结构时检测到环形父子关系(nodeId={})，已剪断该分支", childId);
                result.add(child.setChildren(List.of()));
                continue;
            }
            try {
                result.add(child.setChildren(buildTreeWithMap(parentMap, childId, visiting, depth + 1)));
            } finally {
                // 回溯移除，兄弟分支中出现的同 ID 节点仍应各自展开
                visiting.remove(childId);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * 根据父级 ID 获取直接子节点
     *
     * @param parentId 父级 ID
     * @param function 以父级 ID 为键获取子节点的函数
     * @param <E>      树节点类型
     * @return 子节点列表
     * @apiNote {@code parentId} 为 {@code null} 时按根节点 {@link #ROOT_ID} 处理；
     * 返回的是新集合，修改它不会影响数据源
     */
    public static <
            E extends IEntity<E> & ITree<E>
            > @NotNull List<E> findByParentId(@Nullable Long parentId, @NotNull Function<Long, List<E>> function) {
        Long targetId = Objects.isNull(parentId) ? ROOT_ID : parentId;
        List<E> children = function.apply(targetId);
        if (Objects.isNull(children) || children.isEmpty()) {
            // 返回不可变空列表，与 buildTreeList 的不可变约定保持一致
            return List.of();
        }
        // 复制一份，调用方对返回值的修改不应影响数据源
        return new ArrayList<>(children);
    }

    /**
     * 删除前确认是否包含子节点数据
     *
     * @param id       待删除的 ID
     * @param function 以父级 ID 为键获取子节点的函数
     * @param <E>      树节点类型
     * @apiNote 只判断<b>直接</b>子节点；存在子节点时抛异常
     */
    public static <
            E extends IEntity<E> & ITree<E>
            > void ensureNoChildrenBeforeDelete(long id, @NotNull Function<Long, List<E>> function) {
        List<E> apply = function.apply(id);
        if (Objects.isNull(apply)) {
            return;
        }
        if (!apply.isEmpty()) {
            throw new ServiceException("无法删除含有下级的数据，请先删除所有下级！");
        }
    }

    /**
     * 获取指定父 ID 下的所有后代 ID
     *
     * @param parentId 父 ID
     * @param function 以父级 ID 为键获取子节点的函数
     * @param <E>      树节点类型
     * @return 所有后代的 ID 集合
     * @apiNote 逐层递归展开，ID 为空的节点其子树无法继续收集，会告警并跳过
     */
    public static <
            E extends IEntity<E> & ITree<E>
            > @NotNull Set<Long> getChildrenIdList(
            long parentId,
            @NotNull Function<Long, List<E>> function
    ) {
        Set<Long> collected = new HashSet<>();
        collectChildrenIdList(parentId, function, collected, new HashSet<>(), 0);
        return collected;
    }

    /**
     * 递归收集所有后代 {@code ID}
     *
     * @param parentId  父 ID
     * @param function  以父级 ID 为键获取子节点的函数
     * @param collected 已收集的 ID 集合（返回值）
     * @param visiting  <b>当前递归路径</b>上的 ID 集合，与 {@code collected} 职责不同
     * @param depth     当前深度
     * @param <E>       树节点类型
     * @apiNote {@code collected} 只累积结果，环检测交给 {@code visiting}：
     * 两者共用一个集合会把「已收集过」误判为成环，菱形结构的子树会被整片漏掉
     */
    private static <
            E extends IEntity<E> & ITree<E>
            > void collectChildrenIdList(
            long parentId,
            @NotNull Function<Long, List<E>> function,
            @NotNull Set<Long> collected,
            @NotNull Set<Long> visiting,
            int depth
    ) {
        if (depth > MAX_TREE_DEPTH) {
            log.warn("收集子节点ID时递归深度超过上限({})，已在 parentId={} 处中止，"
                    + "该子树可能未被收集，请检查是否存在环或异常深的树", MAX_TREE_DEPTH, parentId);
            return;
        }
        List<E> children = function.apply(parentId);
        if (Objects.isNull(children)) {
            return;
        }
        for (E child : children) {
            if (Objects.isNull(child)) {
                continue;
            }
            Long id = child.getId();
            if (Objects.isNull(id)) {
                // 查询函数以 parentId 为键，ID 为空的节点没有可用的键去查它的子节点，
                // 遍历只能在此中断；显式告警避免"数据莫名少了 subtree"却无从排查
                log.warn("收集子节点ID时遇到 ID 为空的节点(parentId={})，其子树无法被收集，请检查数据",
                        child.getParentId());
                continue;
            }
            // 结果集合只负责累积，重复出现不影响正确性（DAG 里同一节点只该算一次）
            collected.add(id);
            if (!visiting.add(id)) {
                // 当前路径上再次出现同一个 ID，这才是真的成环
                log.warn("收集子节点ID时检测到环形父子关系(nodeId={})，已剪断该分支", id);
                continue;
            }
            try {
                collectChildrenIdList(id, function, collected, visiting, depth + 1);
            } finally {
                // 回溯移除：菱形结构下同 ID 从不同路径再次出现时应各自展开
                visiting.remove(id);
            }
        }
    }
}
