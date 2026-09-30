package cn.hamm.airpower.core.interfaces;

/**
 * <h1>实体标准接口</h1>
 *
 * @author Hamm.cn
 * @param <E> 实体自身类型，链式赋值后仍返回具体类型
 */
public interface IEntity<E extends IEntity<E>> {
    /**
     * 获取 ID
     *
     * @return ID
     */
    Long getId();

    /**
     * 设置 ID
     *
     * @param id ID
     * @return 实体
     */
    E setId(Long id);
}
