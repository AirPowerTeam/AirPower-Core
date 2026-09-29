package cn.hamm.airpower.core.exception;

import cn.hamm.airpower.core.Json;
import cn.hamm.airpower.core.interfaces.IException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * <h1>系统异常包装类</h1>
 *
 * @author Hamm.cn
 */
@NoArgsConstructor
@Getter
public class ServiceException extends RuntimeException implements IException<ServiceException> {
    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;
    /**
     * 错误代码
     */
    private int code = Json.SERVICE_ERROR;

    /**
     * 错误数据
     *
     * @apiNote 标为 {@code transient}：本类继承自 {@code RuntimeException}
     * （实现 Serializable），若 data 不可序列化，跨进程传递时会二次抛异常掩盖根因
     */
    private transient Object data = null;

    /**
     * 抛出一个自定义错误信息的默认异常
     *
     * @param message 错误信息
     */
    public ServiceException(String message) {
        super(message);
    }

    /**
     * 抛出一个自定义错误信息的默认异常
     *
     * @param message 错误信息
     * @param data    错误数据
     */
    public ServiceException(String message, Object data) {
        super(message);
        this.data = data;
    }

    /**
     * 抛出一个自定义错误信息的默认异常
     *
     * @param code    错误代码
     * @param message 错误信息
     */
    public ServiceException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 抛出一个自定义错误信息的默认异常
     *
     * @param code    错误代码
     * @param message 错误信息
     * @param data    错误数据
     */
    public ServiceException(int code, String message, Object data) {
        super(message);
        this.code = code;
        this.data = data;
    }

    /**
     * 直接抛出一个异常
     *
     * @param exception 异常
     * @param message   错误信息
     */
    public ServiceException(@NotNull IException<?> exception, String message) {
        super(message);
        this.code = exception.getCode();
    }

    /**
     * 直接抛出一个异常
     *
     * @param exception 异常
     */
    public ServiceException(@NotNull IException<?> exception) {
        super(exception.getMessage());
        this.code = exception.getCode();
    }
}
