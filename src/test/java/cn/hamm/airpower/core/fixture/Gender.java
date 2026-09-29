package cn.hamm.airpower.core.fixture;

import cn.hamm.airpower.core.interfaces.IDictionary;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <h1>测试用的枚举字典</h1>
 *
 * @author Hamm.cn
 */
@Getter
@AllArgsConstructor
public enum Gender implements IDictionary {
    /**
     * 未知
     */
    UNKNOWN(0, "未知"),
    /**
     * 男
     */
    MALE(1, "男"),
    /**
     * 女
     */
    FEMALE(2, "女"),
    ;

    /**
     * 字典值
     */
    private final int key;

    /**
     * 字典描述
     */
    private final String label;
}
