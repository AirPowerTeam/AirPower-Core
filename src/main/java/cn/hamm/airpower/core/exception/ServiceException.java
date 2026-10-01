package cn.hamm.airpower.core.exception;

import cn.hamm.airpower.core.Json;
import cn.hamm.airpower.core.interfaces.IException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.Objects;

/**
 * <h1>业务异常</h1>
 *
 * @author Hamm.cn
 * @apiNote 不传错误码时使用 {@link Json#SERVICE_ERROR}
 * @apiNote <b>铁律：{@code data} 绝对不能放异常对象或堆栈。</b>
 * {@code data} 会被 Jackson 序列化进 HTTP 响应体，异常一旦落在里面，
 * 其类型、message 与完整 {@code stackTrace}（类名、文件名、行号）全部泄露给前端。
 * 要保留原始异常请用 {@link #ServiceException(String, Throwable)}，
 * 堆栈需要落盘时由本类统一 {@code log.error} 输出
 */
@Slf4j
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
     * 抛出携带原始异常的业务异常
     *
     * @param message 错误信息
     * @param cause   原始异常，只进 cause，<b>不会</b>回传前端
     * @apiNote 必须用它而不是 {@code (String, Object)}：后者会把异常当成 data 回传前端
     */
    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 抛出携带错误数据的业务异常
     *
     * @param message 错误信息
     * @param data    错误数据，会随响应体返回给前端
     * @apiNote <b>禁止把异常对象传进来</b>：{@code data} 会被 Jackson 序列化进响应体，
     * 异常一旦落在里面，其类型、message 与完整 {@code stackTrace}
     * （含类名、文件名、行号）都会泄露给前端。
     * 要保留原始异常请用 {@link #ServiceException(String, Throwable)}，
     * 堆栈由本类统一 {@code log.error} 输出
     */
    public ServiceException(String message, Object data) {
        super(message);
        if (data instanceof Throwable cause) {
            // 兜底：即便调用方漏看了 javadoc 硬传了异常，也不能让它进 data
            rejectThrowableData(cause);
            return;
        }
        this.data = data;
    }

    /**
     * 抛出指定错误码并携带错误数据的业务异常
     *
     * @param code    错误代码
     * @param message 错误信息
     * @param data    错误数据，会随响应体返回给前端
     * @apiNote 与 {@link #ServiceException(String, Object)} 同样禁止传入异常对象
     */
    public ServiceException(int code, String message, Object data) {
        super(message);
        this.code = code;
        if (data instanceof Throwable cause) {
            rejectThrowableData(cause);
            return;
        }
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

    /**
     * 拒绝把异常放进 {@code data}
     *
     * @param cause 被误当成 data 传入的异常
     */
    private void rejectThrowableData(@NotNull Throwable cause) {
        log.error("异常: {}", cause.getMessage(), cause);
        this.initCause(cause);
    }
}
