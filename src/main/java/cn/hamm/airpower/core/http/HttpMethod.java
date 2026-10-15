package cn.hamm.airpower.core.http;

/**
 * <h2>HTTP 请求方式</h2>
 *
 * @author Hamm.cn
 */
public enum HttpMethod {
    /**
     * 查询
     */
    GET,

    /**
     * 新增
     */
    POST,

    /**
     * 整体更新
     */
    PUT,

    /**
     * 删除
     */
    DELETE,

    /**
     * 局部更新
     */
    PATCH,
}
