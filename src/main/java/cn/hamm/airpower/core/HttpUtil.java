package cn.hamm.airpower.core;

import cn.hamm.airpower.core.constant.HttpConstant;
import cn.hamm.airpower.core.constant.HttpConstant.Header;
import cn.hamm.airpower.core.enums.HttpMethod;
import cn.hamm.airpower.core.exception.ServiceException;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

import static cn.hamm.airpower.core.enums.HttpMethod.GET;

/**
 * <h1>HTTP 请求工具类</h1>
 *
 * @author Hamm.cn
 */
@Slf4j
@Data
@Accessors(chain = true, makeFinal = true)
public class HttpUtil {
    /**
     * 默认超时秒
     */
    private static final int DEFAULT_TIMEOUT_SECOND = 5;

    /**
     * HTTP 客户端
     */
    private HttpClient httpClient;

    /**
     * 请求头
     */
    private Map<String, Object> headers = new HashMap<>();

    /**
     * Cookie
     */
    private Map<String, Object> cookies = new HashMap<>();

    /**
     * 请求地址
     */
    private String url;

    /**
     * 请求体
     */
    private String body = "";

    /**
     * 请求方法
     */
    private HttpMethod method = GET;

    /**
     * 超时时间（秒），同时用作建连超时与请求级超时
     */
    private int timeoutSecond = DEFAULT_TIMEOUT_SECOND;

    /**
     * 请求体类型
     */
    private String contentType = HttpConstant.ContentType.APPLICATION_JSON_UTF8;

    /**
     * 禁止外部实例化
     */
    private HttpUtil() {
    }

    /**
     * 创建一个 HttpUtil 对象
     *
     * @param proxyConfig   代理配置
     * @param timeoutSecond 超时时间（秒）
     * @return HttpUtil
     */
    public static @NotNull HttpUtil create(@Nullable ProxyConfig proxyConfig, int timeoutSecond) {
        if (timeoutSecond <= 0) {
            // Duration.ofSeconds(0/-1) 会抛 JDK 原生 IllegalArgumentException，泄漏到调用方
            throw new ServiceException("超时时间必须大于0秒，当前为 " + timeoutSecond + " 秒");
        }
        HttpUtil httpUtil = new HttpUtil();
        HttpClient.Builder httpClientBuilder = HttpClient.newBuilder();
        // 添加 proxy 代理
        if (Objects.nonNull(proxyConfig)) {
            httpClientBuilder.proxy(new ProxySelector() {
                @Override
                public List<Proxy> select(URI uri) {
                    return List.of(new Proxy(proxyConfig.getType(), new InetSocketAddress(proxyConfig.getHost(), proxyConfig.getPort())));
                }

                @Override
                public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
                    // 代理不可用时至少留下告警，否则上层只会看到一个没有上下文的连接失败
                    log.warn("通过代理({})访问({})失败, {}", proxyConfig.getHost(), uri, ioe.getMessage());
                }
            });
        }
        httpClientBuilder.connectTimeout(Duration.ofSeconds(timeoutSecond));
        // 同时作为请求级超时，避免服务端接受连接后不响应导致调用线程永久挂起
        httpUtil.timeoutSecond = timeoutSecond;
        httpUtil.httpClient = httpClientBuilder.build();
        return httpUtil;
    }

    /**
     * 创建一个 HttpUtil 对象
     *
     * @param timeoutSecond 超时时间（秒）
     * @return HttpUtil
     */
    public static @NotNull HttpUtil create(int timeoutSecond) {
        return create(null, timeoutSecond);
    }

    /**
     * 创建一个 HttpUtil 对象
     *
     * @param proxyConfig 代理配置
     * @return HttpUtil
     */
    public static @NotNull HttpUtil create(@Nullable ProxyConfig proxyConfig) {
        return create(proxyConfig, DEFAULT_TIMEOUT_SECOND);
    }

    /**
     * 创建一个 HttpUtil 对象
     *
     * @return HttpUtil
     */
    public static @NotNull HttpUtil create() {
        return create(null);
    }

    /**
     * 添加  Cookie
     *
     * @param key   Cookie 键
     * @param value Cookie 值
     * @return HttpUtil
     */
    @Contract("_, _ -> this")
    public final HttpUtil addCookie(String key, String value) {
        cookies.put(key, value);
        return this;
    }

    /**
     * 发送 POST 请求
     *
     * @return HttpResponse
     */
    public final @NotNull HttpResponse<String> post() {
        method = HttpMethod.POST;
        return send();
    }

    /**
     * 发送 POST 请求
     *
     * @param body 请求体
     * @return HttpResponse
     */
    @SuppressWarnings("UnusedReturnValue")
    public final @NotNull HttpResponse<String> post(String body) {
        method = HttpMethod.POST;
        this.body = body;
        return send();
    }

    /**
     * 发送 GET 请求
     *
     * @return HttpResponse
     */
    public final @NotNull HttpResponse<String> get() {
        if (method == HttpMethod.GET) {
            return send();
        }
        // 链式调用 setMethod(PUT).get() 时显式提示，避免被静默改回 GET
        throw new ServiceException("当前请求方法为 " + method + "，如需发起 GET 请求请使用 HttpUtil.create().setMethod(GET)");
    }

    /**
     * 发送 PUT 请求
     *
     * @return HttpResponse
     */
    public final @NotNull HttpResponse<String> put() {
        return put(body);
    }

    /**
     * 发送 PUT 请求
     *
     * @param body 请求体
     * @return HttpResponse
     */
    public final @NotNull HttpResponse<String> put(String body) {
        method = HttpMethod.PUT;
        this.body = body;
        return send();
    }

    /**
     * 发送 PATCH 请求
     *
     * @return HttpResponse
     */
    public final @NotNull HttpResponse<String> patch() {
        return patch(body);
    }

    /**
     * 发送 PATCH 请求
     *
     * @param body 请求体
     * @return HttpResponse
     */
    public final @NotNull HttpResponse<String> patch(String body) {
        method = HttpMethod.PATCH;
        this.body = body;
        return send();
    }

    /**
     * 发送 DELETE 请求
     *
     * @return HttpResponse
     */
    public final @NotNull HttpResponse<String> delete() {
        method = HttpMethod.DELETE;
        return send();
    }

    /**
     * 发送请求
     *
     * @return HttpResponse
     */
    public final @NotNull HttpResponse<String> send() {
        try {
            return httpClient.send(getHttpRequest(), HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            // 保留原始异常，便于上层区分超时、连接失败或请求非法
            ServiceException serviceException = new ServiceException("发起请求失败，" + e.getMessage());
            serviceException.initCause(e);
            throw serviceException;
        }
    }

    /**
     * 获取 HttpRequest 对象
     *
     * @return HttpRequest
     */
    private HttpRequest getHttpRequest() {
        if (Objects.isNull(url) || url.isBlank()) {
            throw new ServiceException("请求地址不能为空");
        }
        if (Objects.isNull(body)) {
            body = "";
        }
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(timeoutSecond));
        headers.forEach((key, value) -> requestBuilder.header(key, value.toString()));
        HttpRequest.BodyPublisher bodyPublisher = HttpRequest.BodyPublishers.ofString(body);
        switch (method) {
            case GET -> requestBuilder.GET();
            case POST -> requestBuilder.POST(bodyPublisher);
            case PUT -> requestBuilder.PUT(bodyPublisher);
            case DELETE -> requestBuilder.DELETE();
            // 枚举里暴露了 PATCH，实现却缺失，此前要运行到才报错
            case PATCH -> requestBuilder.method("PATCH", bodyPublisher);
        }
        if (Objects.nonNull(cookies) && !cookies.isEmpty()) {
            List<String> cookieList = new ArrayList<>();
            cookies.forEach((key, value) -> cookieList.add(key + "=" + value));
            requestBuilder.setHeader(
                    Header.COOKIE, String.join("; ", cookieList)
            );
        }
        if (Objects.nonNull(contentType)) {
            requestBuilder.header(Header.CONTENT_TYPE, contentType);
        }
        return requestBuilder.build();
    }

    /**
     * 添加 Header
     *
     * @param key   Header 键
     * @param value Header 值
     * @return HttpUtil
     */
    @Contract("_, _ -> this")
    public final HttpUtil addHeader(String key, Object value) {
        headers.put(key, value);
        return this;
    }

    @Data
    @Accessors(chain = true)
    public static class ProxyConfig {
        /**
         * 代理地址
         */
        private String host = "127.0.0.1";

        /**
         * 代理端口
         */
        private int port = 1080;

        /**
         * 代理类型
         */
        private Proxy.Type type = Proxy.Type.SOCKS;
    }
}
