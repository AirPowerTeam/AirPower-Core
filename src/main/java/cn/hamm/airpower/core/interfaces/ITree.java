package cn.hamm.airpower.core.interfaces;

import java.util.List;

/**
 * <h1>树结构实体标准接口</h1>
 *
 * @param <E> 树节点自身类型
 * @author Hamm.cn
 * @apiNote 根节点的 {@code parentId} 约定为 {@code 0}，由 {@code TreeUtil} 统一处理
 */
public interface ITree<E extends ITree<E>> extends IEntity<E> {
    /**
     * 获取树的父级 ID
     *
     * @return 父级 ID
     */
    Long getParentId();

    /**
     * 设置父级 ID
     *
     * @param parentId 设置父级 ID
     * @return 树实体
     */
    E setParentId(Long parentId);

    /**
     * 获取树的子集列表
     *
     * @return 树的子集
     */
    List<E> getChildren();

    /**
     * 设置树的子集列表
     *
     * @param children 子集
     * @return 树实体
     */
    E setChildren(List<E> children);
}
