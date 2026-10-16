package cn.hamm.airpower.core.http;

import cn.hamm.airpower.core.exception.ServiceException;
import cn.hamm.airpower.core.file.FileUtil;
import cn.hamm.airpower.core.http.HttpConstant.Header;
import lombok.*;
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

import static cn.hamm.airpower.core.http.HttpMethod.GET;

/**
 * <h2>HTTP 请求工具类</h2>
 *
 * @author Hamm.cn
 */
@Slf4j
@Getter
@Setter
@Accessors(chain = true, makeFinal = true)
@ToString(exclude = {"httpClient", "headers", "cookies", "body"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
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
     * 共享的 HTTP 客户端
     */
    private static final HttpClient SHARED_HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECOND))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    /**
     * HTTP 客户端
     *
     * @apiNote 仅由 {@link #create(ProxyConfig, int)} 赋值，不对外暴露 setter
     */
    @Setter(AccessLevel.NONE)
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
    @Setter(AccessLevel.NONE)
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
        if (Objects.isNull(proxyConfig)) {
            httpUtil.timeoutSecond = timeoutSecond;
            httpUtil.httpClient = SHARED_HTTP_CLIENT;
            return httpUtil;
        }
        HttpClient.Builder httpClientBuilder = HttpClient.newBuilder();
        // 添加 proxy 代理
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
        httpClientBuilder.connectTimeout(Duration.ofSeconds(timeoutSecond));
        httpClientBuilder.followRedirects(HttpClient.Redirect.NEVER);
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
    @Contract(pure = true)
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
     * 设置超时时间（秒）
     *
     * @param timeoutSecond 超时秒数
     * @return 当前对象
     */
    public HttpUtil setTimeoutSecond(int timeoutSecond) {
        if (timeoutSecond <= 0) {
            throw new ServiceException("超时时间必须大于0秒，当前为 " + timeoutSecond + " 秒");
        }
        this.timeoutSecond = timeoutSecond;
        return this;
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
        HttpRequest request = getHttpRequest();
        try {
            return httpClient.send(request, boundedStringBodyHandler());
        } catch (InterruptedException e) {
            // 必须恢复中断位，否则 JDK 内部的锁与信号量会停止响应中断，拖长优雅停机
            Thread.currentThread().interrupt();
            throw new ServiceException("发起请求被中断", e);
        } catch (ServiceException e) {
            throw e;
        } catch (IOException e) {
            Throwable cause = e.getCause();
            while (Objects.nonNull(cause)) {
                if (cause instanceof ServiceException serviceException) {
                    throw serviceException;
                }
                cause = cause.getCause();
            }
            throw new ServiceException("发起请求失败", e);
        } catch (Exception e) {
            throw new ServiceException("发起请求失败", e);
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
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            throw new ServiceException("非法的请求地址: " + url, e);
        }
        if (Objects.isNull(uri.getHost())) {
            throw new ServiceException("非法的请求地址: " + url);
        }
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(uri)
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
            default -> throw new ServiceException("不支持的 HTTP 方法: " + method);
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
     * <h2>带上限的响应体收集器</h2>
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
         * 底层订阅
         *
         * @apiNote onSubscribe 由 JDK 的 HTTP 客户端线程调用，onNext 由其数据线程调用，
         * 用 volatile 保证可见性；不 volatile 的话 onNext 可能看到 null 而跳过 cancel
         */
        private volatile Flow.Subscription subscription;

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
            // 必须把 subscription 存下来：不存就永远无法 cancel()，
            // 「限制响应体大小」就只限制了本方的堆占用，管不住网络传输本身
            this.subscription = subscription;
            // 与 JDK 内置的 BodySubscribers.ofByteArray() 一致：一次性订阅全部。
            // 上限判断在 onNext 里做，超限即 cancel + completeExceptionally
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(List<ByteBuffer> item) {
            try {
                for (ByteBuffer chunk : item) {
                    int remaining = maxBytes - buffer.size();
                    if (chunk.remaining() > remaining) {
                        // 超出上限：先取消订阅真正停掉传输，再让 future 失败。
                        // cancel() 幂等，重复调用安全。
                        // 这里刻意抛 ServiceException 而不是 IOException：
                        // send() 会对 ServiceException 原样上抛，这条消息正是写给调用方看的
                        cancelSubscription();
                        result.completeExceptionally(new ServiceException(
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

        /**
         * 取消底层订阅，真正停止数据传输
         */
        private void cancelSubscription() {
            Flow.Subscription current = subscription;
            if (Objects.nonNull(current)) {
                current.cancel();
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
