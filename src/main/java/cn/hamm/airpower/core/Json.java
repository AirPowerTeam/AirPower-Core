package cn.hamm.airpower.core;

import cn.hamm.airpower.core.annotation.Description;
import cn.hamm.airpower.core.exception.ServiceException;
import cn.hamm.airpower.core.interfaces.IException;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS;

/**
 * <h1>简单 JSON 对象</h1>
 *
 * @author Hamm.cn
 */
@Data
@Accessors(chain = true)
@Slf4j
public class Json {
    /**
     * 成功的代码
     */
    public static final int SUCCESS_CODE = 200;

    /**
     * 错误代码
     */
    public static final int SERVICE_ERROR = 500;

    /**
     * 未授权代码
     */
    public static final int UNAUTHORIZED_CODE = 401;

    /**
     * {@code ObjectMapper}
     */
    private static ObjectMapper objectMapper = null;

    /**
     * 错误代码
     */
    @Description("错误代码")
    private int code = Json.SUCCESS_CODE;

    /**
     * 错误信息
     */
    @Description("错误信息")
    private String message = "";

    /**
     * 返回数据
     */
    @Description("返回数据")
    private Object data;

    @Description("TraceID")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String traceId;

    @Contract(pure = true)
    private Json() {
        // 禁止外部实例化
    }

    /**
     * 输出提示信息
     *
     * @param message 提示信息
     * @return JSON
     */
    public static Json success(String message) {
        return create().setMessage(message);
    }

    /**
     * 输出数据
     *
     * @param data 数据
     * @return JSON
     */
    public static Json data(Object data) {
        return data(data, "获取成功");
    }

    /**
     * 输出数据
     *
     * @param data    数据
     * @param message 提示信息
     * @return JSON
     */
    public static Json data(Object data, String message) {
        return create().setData(data).setMessage(message);
    }

    /**
     * 输出错误
     *
     * @param error 错误枚举
     * @return JSON
     */
    public static Json error(IException<?> error) {
        return error(error, error.getMessage());
    }

    /**
     * 输出错误
     *
     * @param error   错误枚举
     * @param message 错误信息
     * @return JSON
     */
    public static Json error(@NotNull IException<?> error, String message) {
        return error(error, message, null);
    }

    /**
     * 输出错误
     *
     * @param error   错误枚举
     * @param message 错误信息
     * @param data    错误数据
     * @return JSON
     */
    public static Json error(@NotNull IException<?> error, String message, Object data) {
        return show(error.getCode(), message, data);
    }

    /**
     * 输出错误
     *
     * @param message 错误信息
     * @return JSON
     */
    public static Json error(String message) {
        return error(message, SERVICE_ERROR);
    }

    /**
     * 输出错误
     *
     * @param message 错误信息
     * @param code    错误代码
     * @return JSON
     */
    public static Json error(String message, int code) {
        return show(code, message, null);
    }

    /**
     * 输出错误
     *
     * @param message 错误信息
     * @param code    错误代码
     * @param data    错误数据
     * @return JSON
     */
    public static Json error(String message, int code, Object data) {
        return show(code, message, data);
    }

    /**
     * 输出 JSON
     *
     * @param code    错误代码
     * @param message 提示信息
     * @param data    输出数据
     * @return JSON
     */
    public static Json show(int code, String message, Object data) {
        return create().setCode(code).setMessage(message).setData(data);
    }

    /**
     * JSON 反序列化到指定类
     *
     * @param json  字符串
     * @param clazz 目标类
     * @param <T>   目标类
     * @return 目标类的实例
     */
    public static <T> T parse(String json, Class<T> clazz) {
        try {
            return getObjectMapper().readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage(), e);
            throw new ServiceException("JSON 反序列化失败，" + e.getMessage());
        }
    }

    /**
     * JSON 反序列化到指定类
     *
     * @param json          字符串
     * @param typeReference 目标类
     * @param <T>           目标类
     * @return 目标类的实例
     */
    public static <T> T parse(String json, TypeReference<T> typeReference) {
        try {
            return getObjectMapper().readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage(), e);
            throw new ServiceException("JSON 反序列化失败，" + e.getMessage());
        }
    }

    /**
     * JSON 反序列化为数组
     *
     * @param json  字符串
     * @param clazz 目标数组类
     * @param <T>   目标类型
     * @return 目标类的实例数组
     */
    public static <T> T[] parseList(String json, Class<? extends T[]> clazz) {
        try {
            return getObjectMapper().readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage(), e);
            throw new ServiceException("JSON 反序列化失败，" + e.getMessage());
        }
    }

    /**
     * JSON 反序列化为 {@code Map}
     *
     * @param json 字符串
     * @return {@code Map}
     */
    public static Map<String, Object> parse2Map(String json) {
        try {
            TypeReference<Map<String, Object>> typeRef = new TypeReference<>() {
            };
            return getObjectMapper().readValue(json, typeRef);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ServiceException("JSON 反序列化失败，" + e.getMessage());
        }
    }

    /**
     * JSON 反序列化为 {@code ListMap}
     *
     * @param json 字符串
     * @return {@code List<Map>}
     */
    public static List<Map<String, Object>> parse2MapList(String json) {
        try {
            TypeReference<List<Map<String, Object>>> typeRef = new TypeReference<>() {
            };
            return getObjectMapper().readValue(json, typeRef);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ServiceException("JSON 反序列化失败，" + e.getMessage());
        }
    }

    /**
     * JSON 序列化到字符串
     *
     * @param object 对象
     * @return 字符串
     */
    public static String toString(Object object) {
        try {
            return getObjectMapper().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage(), e);
            throw new ServiceException("JSON 序列化失败，" + e.getMessage());
        }
    }

    /**
     * 获取一个配置后的 {@code ObjectMapper}
     *
     * @return {@code ObjectMapper}
     */
    private static @NotNull ObjectMapper getObjectMapper() {
        if (Objects.isNull(objectMapper)) {
            synchronized (Json.class) {
                if (Objects.isNull(objectMapper)) {
                    ObjectMapper mapper = new ObjectMapper();
                    // 忽略未声明的属性
                    mapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
                    // 忽略值为 null 的属性
                    mapper.configOverride(Map.class)
                            .setInclude(JsonInclude.Value.construct(JsonInclude.Include.NON_EMPTY, null));
                    // 忽略没有属性的类
                    mapper.configure(FAIL_ON_EMPTY_BEANS, false);
                    objectMapper = mapper;
                }
            }
        }
        return objectMapper;
    }

    /**
     * 初始化一个新的 JSON 对象
     *
     * @return JSON 对象
     */
    @Contract(" -> new")
    public static @NotNull Json create() {
        return new Json();
    }
}
