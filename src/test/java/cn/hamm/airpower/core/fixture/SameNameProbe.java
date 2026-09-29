package cn.hamm.airpower.core.fixture;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <h1>同名类隔离加载测试用模型</h1>
 *
 * <p>仅供 {@code ReflectUtilTest} 用自定义 {@code ClassLoader} 重复加载，
 * 用于验证字段缓存以 {@code Class} 为键时不会发生同名类串号。
 * 业务代码不应引用本类。</p>
 *
 * @author Hamm.cn
 */
@Data
@Accessors(chain = true)
public class SameNameProbe {
    /**
     * 用于验证缓存隔离的字段
     */
    private String probeField;
}
