package cn.hamm.airpower.core.exception;

import cn.hamm.airpower.core.Json;
import cn.hamm.airpower.core.interfaces.IException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.Objects;

/**
 * <h1>业务异常</h1>
 *
 * @author Hamm.cn
 * @apiNote 不传错误码时使用 {@link Json#SERVICE_ERROR}；{@code data} 会随响应体返回给前端，
 * 因此不要往里塞敏感信息
 */
@NoArgsConstructor
@Getter
public class ServiceException extends RuntimeException implements IException<ServiceException> {
    /**
     * 序列化版本号
     */
    @Serial
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
     * 抛出使用默认错误码的业务异常
     *
     * @param message 错误信息
     */
    public ServiceException(String message) {
        super(message);
    }

    /**
     * 抛出携带错误数据的业务异常
     *
     * @param message 错误信息
     * @param data    错误数据
     */
    public ServiceException(String message, Object data) {
        super(message);
        this.data = data;
    }

    /**
     * 抛出指定错误码的业务异常
     *
     * @param code    错误代码
     * @param message 错误信息
     */
    public ServiceException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 抛出指定错误码并携带错误数据的业务异常
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
     * 复用已有异常的错误码，替换错误信息
     *
     * @param exception 异常
     * @param message   错误信息
     * @apiNote {@code exception} 为 {@code null} 时沿用默认错误码，不报错
     */
    public ServiceException(IException<?> exception, String message) {
        super(message);
        if (Objects.nonNull(exception)) {
            this.code = exception.getCode();
        }
    }

    /**
     * 按已有异常直接抛出业务异常
     *
     * @param exception 异常
     */
    public ServiceException(@NotNull IException<?> exception) {
        super(exception.getMessage());
        this.code = exception.getCode();
    }
}
