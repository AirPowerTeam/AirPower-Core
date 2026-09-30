package cn.hamm.airpower.core.interfaces;

import cn.hamm.airpower.core.ReflectUtil;

/**
 * <h1>字典枚举标准接口</h1>
 *
 * @author Hamm.cn
 * @see ReflectUtil
 * @apiNote {@code key} 是落库和传输用的数值，{@code label} 仅用于展示，
 * 调整 {@code label} 不影响历史数据
 */
public interface IDictionary {
    /**
     * 获取枚举的 Key
     *
     * @return Key
     */
    int getKey();

    /**
     * 获取枚举的描述
     *
     * @return 描述
     */
    String getLabel();

    /**
     * 判断 Key 是否相等
     *
     * @param key 被判断的 Key
     * @return 对比结果
     */
    default boolean equalsKey(int key) {
        return getKey() == key;
    }

    /**
     * 判断 Key 是否不相等
     *
     * @param key 被判断的 Key
     * @return 对比结果
     */
    default boolean notEqualsKey(int key) {
        return !equalsKey(key);
    }
}
