package cn.hamm.airpower.core.fixture;

import cn.hamm.airpower.core.interfaces.IException;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <h1>测试用的异常枚举</h1>
 *
 * @author Hamm.cn
 */
@Getter
@AllArgsConstructor
public enum DemoError implements IException<DemoError> {
    /**
     * 参数错误
     */
    PARAM_ERROR(400, "参数错误"),
    /**
     * 未授权
     */
    UNAUTHORIZED(401, "未授权，请先登录"),
    /**
     * 未知错误
     */
    UNKNOWN(500, "未知错误"),
    ;

    /**
     * 错误代码
     */
    private final int code;

    /**
     * 错误信息
     */
    private final String message;
}
