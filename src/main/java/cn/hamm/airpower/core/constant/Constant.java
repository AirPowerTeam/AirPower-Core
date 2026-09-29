package cn.hamm.airpower.core.constant;

import org.jetbrains.annotations.Contract;

/**
 * <h1>常量</h1>
 *
 * @author Hamm.cn
 */
public class Constant {
    /**
     * ID
     */
    public static final String ID = "id";

    /**
     * 禁止外部实例化
     */
    @Contract(pure = true)
    private Constant() {
    }
}
