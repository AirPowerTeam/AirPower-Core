package cn.hamm.airpower.core.fixture;

import cn.hamm.airpower.core.annotation.Description;

/**
 * 把 {@link Description} 挂在方法上的接口
 *
 * @author Hamm.cn
 * @apiNote 用于验证「实现类未标注时能否回溯到接口上的注解」：
 * 业务里常在接口上定义契约，实现类不重复标注
 */
public interface AnnotatedInterface {

    /**
     * 接口上标注了 @Description
     *
     * @return 描述
     */
    @Description("接口上的中文描述")
    String getName();
}
