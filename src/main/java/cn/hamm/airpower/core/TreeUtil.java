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
 */
@Slf4j
public class TreeUtil {
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
     * @param <E>  泛型
     * @return 树结构数组
     */
    public static <E extends IEntity<E> & ITree<E>> @Unmodifiable @NotNull List<E> buildTreeList(List<E> list) {
        if (Objects.isNull(list) || list.isEmpty()) {
            return List.of();
        }
        return buildTreeListOptimized(list, ROOT_ID);
    }

    /**
     * 生成树结构（优化版 - O(n) 复杂度）
     *
     * @param list     原始数据列表
     * @param parentId 父级 ID
     * @param <E>      泛型
     * @return 树结构数组
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
     * @param list       原始数据
     * @param declaredIds 数据中出现过的 ID 集合
     * @param <E>        泛型
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
     * @param <E>       泛型
     * @return 树结构数组
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
     * @param visited   当前递归路径上的 ID，用于检测环形数据
     * @param <E>       泛型
     * @return 树结构数组
     * @apiNote 父级 ID 构成环且从 {@link #ROOT_ID} 可达时，无环检测会一直递归到
     * {@code StackOverflowError}
     */
    private static <E extends IEntity<E> & ITree<E>> @UnmodifiableView @NotNull List<E> buildTreeWithMap(
            @NotNull Map<Long, List<E>> parentMap, long parentId, @NotNull Set<Long> visiting
    ) {
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
                result.add(child.setChildren(buildTreeWithMap(parentMap, childId, visiting)));
            } finally {
                // 回溯移除，兄弟分支中出现的同 ID 节点仍应各自展开
                visiting.remove(childId);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * 根据父级 ID 获取所有子节点
     *
     * @param parentId 父级 ID
     * @return 子节点列表
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
     * @param function 获取子节点的函数
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
     * 获取指定父ID下的所有子 ID
     *
     * @param parentId 父 ID
     */
    public static <
            E extends IEntity<E> & ITree<E>
            > @NotNull Set<Long> getChildrenIdList(
            long parentId,
            @NotNull Function<Long, List<E>> function
    ) {
        Set<Long> collected = new HashSet<>();
        collectChildrenIdList(parentId, function, collected);
        return collected;
    }

    /**
     * 递归收集所有后代 {@code ID}
     *
     * @param parentId  父 ID
     * @param function  获取子节点的函数
     * @param collected 已收集的 ID 集合，用于过滤 {@code null} ID 与环形数据
     * @param <E>       泛型
     */
    private static <
            E extends IEntity<E> & ITree<E>
            > void collectChildrenIdList(
            long parentId,
            @NotNull Function<Long, List<E>> function,
            @NotNull Set<Long> collected
    ) {
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
            if (!collected.add(id)) {
                // 已收集过，避免环形数据导致无限递归
                continue;
            }
            collectChildrenIdList(id, function, collected);
        }
    }
}
