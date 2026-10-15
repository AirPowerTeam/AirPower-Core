package cn.hamm.airpower.core.common;

import org.jetbrains.annotations.Contract;

/**
 * <h2>通用常量</h2>
 *
 * @author Hamm.cn
 */
public class Constant {
    /**
     * 实体主键的字段名
     */
    public static final String ID = "id";

    /**
     * 禁止外部实例化
     */
    @Contract(pure = true)
    private Constant() {
    }
}
