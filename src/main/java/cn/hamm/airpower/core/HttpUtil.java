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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

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
     * 响应体最大字节数（16MB）
     */
    private static final int MAX_RESPONSE_BYTES = 16 * FileUtil.FILE_SCALE * FileUtil.FILE_SCALE;

    /**
     * Cookie 名与值里都不允许出现的字符
     *
     * @apiNote {@code ;} 是 Cookie 对的分隔符，部分实现还把 {@code ,} 当分隔符，
     * CR/LF 会造成请求头注入
     */
    private static final String COOKIE_FORBIDDEN = ";,=\r\n";

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
     * 带体积上限的字符串响应处理器
     *
     * @return 响应处理器
     */
    private static @NotNull HttpResponse.BodyHandler<String> boundedStringBodyHandler() {
        return responseInfo -> HttpResponse.BodySubscribers.mapping(
                new BoundedByteArrayBodySubscriber(MAX_RESPONSE_BYTES),
                bytes -> new String(bytes, StandardCharsets.UTF_8)
        );
    }

    /**
     * 校验 Cookie 的名或值
     *
     * @param part Cookie 名或值
     * @param kind 「名称」或「值」，用于报错信息
     */
    private static void checkCookiePart(@NotNull String part, String kind) {
        for (int i = 0; i < part.length(); i++) {
            char c = part.charAt(i);
            if (c < 0x20 || c == 0x7F || COOKIE_FORBIDDEN.indexOf(c) >= 0) {
                throw new ServiceException(
                        "Cookie " + kind + "包含非法字符（位置 " + i + "），拒绝拼进请求头以防注入");
            }
        }
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
            return httpClient.send(getHttpRequest(), boundedStringBodyHandler());
        } catch (InterruptedException e) {
            // 必须恢复中断位，否则 JDK 内部的锁与信号量会停止响应中断，拖长优雅停机
            Thread.currentThread().interrupt();
            throw new ServiceException("发起请求被中断，" + e.getMessage(), e);
        } catch (Exception e) {
            // 保留原始异常，便于上层区分超时、连接失败或请求非法。
            // 走 (String, Throwable) 重载：cause 由构造器设置，异常不会被当成 data 回传前端。
            // 不要再调 initCause —— cause 已存在时它会抛 IllegalStateException
            throw new ServiceException("发起请求失败，" + e.getMessage(), e);
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
            // HttpRequest.BodyPublishers 没有 PATCH 的便捷方法，只能用 method
            case PATCH -> requestBuilder.method("PATCH", bodyPublisher);
        }
        if (Objects.nonNull(cookies) && !cookies.isEmpty()) {
            List<String> cookieList = new ArrayList<>(cookies.size());
            cookies.forEach((key, value) -> {
                String name = String.valueOf(key);
                String val = String.valueOf(value);
                checkCookiePart(name, "名称");
                checkCookiePart(val, "值");
                cookieList.add(name + "=" + val);
            });
            requestBuilder.setHeader(
                    Header.COOKIE, String.join("; ", cookieList)
            );
        }
        if (Objects.nonNull(contentType)) {
            // 必须用 setHeader（替换）而不是 header（追加）：
            // 调用方若用 addHeader("Content-Type", ...) 传过，这里再追加一次，
            // 同一个请求就会带两个 Content-Type。很多服务端/网关对重复头
            // 是「取第一个」或直接 400，而第三方 API 往往只报「参数错误」，极难定位
            requestBuilder.setHeader(Header.CONTENT_TYPE, contentType);
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

    /**
     * <h1>带上限的响应体收集器</h1>
     *
     * <p>累积收到的字节，超过上限立即以异常结束 {@link CompletionStage}。
     * 必须在 {@code onNext} 里判断——等读完再判断就失去意义了。</p>
     */
    private static final class BoundedByteArrayBodySubscriber implements HttpResponse.BodySubscriber<byte[]> {
        /**
         * 字节上限
         */
        private final int maxBytes;

        /**
         * 累积缓冲
         */
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        /**
         * 结果
         */
        private final CompletableFuture<byte[]> result = new CompletableFuture<>();

        /**
         * 构造
         *
         * @param maxBytes 字节上限
         */
        private BoundedByteArrayBodySubscriber(int maxBytes) {
            this.maxBytes = maxBytes;
        }

        @Override
        public CompletionStage<byte[]> getBody() {
            return result;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            // 与 JDK 内置的 BodySubscribers.ofByteArray() 一致：一次性订阅全部。
            // 上限判断在 onNext 里做，超限即 completeExceptionally，
            // 后续分片不再累积
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(List<ByteBuffer> item) {
            try {
                for (ByteBuffer chunk : item) {
                    int remaining = maxBytes - buffer.size();
                    if (chunk.remaining() > remaining) {
                        // 超出上限：直接失败，不再读取后续数据
                        result.completeExceptionally(new IOException(
                                "响应体超过上限 " + maxBytes + " 字节，已中止读取"));
                        return;
                    }
                    byte[] bytes = new byte[chunk.remaining()];
                    chunk.get(bytes);
                    buffer.write(bytes);
                }
            } catch (IOException | RuntimeException e) {
                // ByteArrayOutputStream.write 不抛 IOException，这里一并兜住以防将来改动
                result.completeExceptionally(e);
            }
        }

        @Override
        public void onError(Throwable throwable) {
            result.completeExceptionally(throwable);
        }

        @Override
        public void onComplete() {
            result.complete(buffer.toByteArray());
        }
    }

    @Data
    @Accessors(chain = true)
    public static class ProxyConfig {
        /**
         * 代理地址
         */
        private String host = HttpConstant.LOCAL_IP_ADDRESS;

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
