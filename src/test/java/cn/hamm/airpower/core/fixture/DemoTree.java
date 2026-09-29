package cn.hamm.airpower.core.fixture;

import cn.hamm.airpower.core.interfaces.IEntity;
import cn.hamm.airpower.core.interfaces.ITree;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * <h1>测试用的树实体</h1>
 *
 * @author Hamm.cn
 */
@Data
@Accessors(chain = true)
public class DemoTree implements IEntity<DemoTree>, ITree<DemoTree> {
    /**
     * ID
     */
    private Long id;

    /**
     * 父级 ID
     */
    private Long parentId;

    /**
     * 名称
     */
    private String name;

    /**
     * 子节点
     */
    private List<DemoTree> children;
}
