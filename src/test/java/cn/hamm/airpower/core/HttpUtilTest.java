package cn.hamm.airpower.core;

import cn.hamm.airpower.core.constant.HttpConstant;
import cn.hamm.airpower.core.enums.HttpMethod;
import cn.hamm.airpower.core.exception.ServiceException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>HttpUtil 单元测试</h1>
 *
 * <p>使用 JDK 自带的 {@link com.sun.net.httpserver.HttpServer} 在本机随机端口启动测试服务器，
 * 覆盖工厂方法、链式赋值、请求头 / Cookie 透传、真实 GET / POST / PUT / DELETE 请求以及异常分支。</p>
 *
 * @author Hamm.cn
 */
@DisplayName("HttpUtil 单元测试")
class HttpUtilTest {
    /**
     * 测试服务器返回的响应体
     */
    private static final String RESPONSE_BODY = "RESPONSE-OK";

    /**
     * 自定义请求头名称
     */
    private static final String CUSTOM_HEADER = "X-Custom-Token";

    /**
     * 本地测试服务器
     */
    private static HttpServer server;

    /**
     * 测试服务器线程池
     */
    private static ExecutorService executor;

    /**
     * 测试服务器基础地址
     */
    private static String baseUrl;

    /**
     * 服务端收到的请求方法
     */
    private static volatile String recordedMethod;

    /**
     * 服务端收到的请求体
     */
    private static volatile String recordedBody;

    /**
     * 服务端收到的 Content-Type 请求头
     */
    private static volatile String recordedContentType;

    /**
     * 服务端收到的自定义请求头
     */
    private static volatile String recordedCustomHeader;

    /**
     * 服务端收到的 Cookie 请求头
     */
    private static volatile String recordedCookie;

    /**
     * 启动本地测试服务器（随机端口）
     *
     * @throws IOException 服务器启动失败
     */
    @BeforeAll
    static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(HttpConstant.LOCAL_IP_ADDRESS, 0), 0);
        server.createContext("/", HttpUtilTest::handle);
        AtomicInteger threadIndex = new AtomicInteger();
        executor = Executors.newFixedThreadPool(4, runnable -> {
            Thread thread = new Thread(runnable, "airpower-http-test-" + threadIndex.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
        server.setExecutor(executor);
        server.start();
        baseUrl = "http://" + HttpConstant.LOCAL_IP_ADDRESS + ":" + server.getAddress().getPort() + "/api/test";
    }

    /**
     * 停止本地测试服务器
     */
    @AfterAll
    static void stopServer() {
        if (null != server) {
            server.stop(0);
        }
        if (null != executor) {
            executor.shutdownNow();
        }
    }

    /**
     * 测试服务器的请求处理器
     *
     * @param exchange 一次 HTTP 交互
     * @throws IOException 写出响应失败
     */
    private static void handle(HttpExchange exchange) throws IOException {
        String body;
        try (InputStream input = exchange.getRequestBody()) {
            body = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        recordedMethod = exchange.getRequestMethod();
        recordedBody = body;
        recordedContentType = firstHeader(exchange, HttpConstant.Header.CONTENT_TYPE);
        recordedCustomHeader = firstHeader(exchange, CUSTOM_HEADER);
        recordedCookie = firstHeader(exchange, HttpConstant.Header.COOKIE);
        byte[] bytes = RESPONSE_BODY.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set(HttpConstant.Header.CONTENT_TYPE, HttpConstant.ContentType.APPLICATION_JSON_UTF8);
        exchange.sendResponseHeaders(HttpConstant.Status.OK, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    /**
     * 读取第一个请求头
     *
     * @param exchange 一次 HTTP 交互
     * @param name     请求头名称
     * @return 请求头值，不存在时返回 {@code null}
     */
    private static String firstHeader(HttpExchange exchange, String name) {
        List<String> values = exchange.getRequestHeaders().get(name);
        return (null == values || values.isEmpty()) ? null : values.get(0);
    }

    /**
     * 每个用例前重置服务端记录
     */
    @BeforeEach
    void resetRecorded() {
        recordedMethod = null;
        recordedBody = null;
        recordedContentType = null;
        recordedCustomHeader = null;
        recordedCookie = null;
    }

    @Nested
    @DisplayName("工厂方法与默认值")
    class CreateTest {
        @Test
        @DisplayName("create() 应返回可用的 HttpUtil 并带有默认字段值")
        void testCreateDefault() {
            HttpUtil httpUtil = HttpUtil.create();
            assertNotNull(httpUtil, "create() 不应返回 null");
            assertNotNull(httpUtil.getHttpClient(), "create() 应初始化 HttpClient");
            assertNull(httpUtil.getUrl(), "新建对象的请求地址应为 null");
            assertEquals("", httpUtil.getBody(), "默认请求体应为空字符串");
            assertEquals(HttpMethod.GET, httpUtil.getMethod(), "默认请求方法应为 GET");
            assertEquals(HttpConstant.ContentType.APPLICATION_JSON_UTF8, httpUtil.getContentType(),
                    "默认请求体类型应为 application/json;charset=UTF-8");
            assertTrue(httpUtil.getHeaders().isEmpty(), "默认请求头集合应为空");
            assertTrue(httpUtil.getCookies().isEmpty(), "默认 Cookie 集合应为空");
        }

        @Test
        @DisplayName("create(int) 应按指定超时时间创建对象")
        void testCreateWithTimeoutSecond() {
            HttpUtil httpUtil = HttpUtil.create(3);
            assertNotNull(httpUtil, "create(int) 不应返回 null");
            assertNotNull(httpUtil.getHttpClient(), "create(int) 应初始化 HttpClient");
        }

        @Test
        @DisplayName("create(ProxyConfig) 应可创建对象")
        void testCreateWithProxyConfig() {
            HttpUtil httpUtil = HttpUtil.create(new HttpUtil.ProxyConfig());
            assertNotNull(httpUtil, "create(ProxyConfig) 不应返回 null");
            assertNotNull(httpUtil.getHttpClient(), "create(ProxyConfig) 应初始化 HttpClient");
        }

        @Test
        @DisplayName("create(ProxyConfig, int) 应可创建对象")
        void testCreateWithProxyConfigAndTimeoutSecond() {
            HttpUtil httpUtil = HttpUtil.create(new HttpUtil.ProxyConfig().setHost("127.0.0.1").setPort(9), 2);
            assertNotNull(httpUtil, "create(ProxyConfig, int) 不应返回 null");
            assertNotNull(httpUtil.getHttpClient(), "create(ProxyConfig, int) 应初始化 HttpClient");
        }

        @Test
        @DisplayName("create(null, int) 代理配置为 null 时应退化为直连")
        void testCreateWithNullProxyConfig() {
            HttpUtil httpUtil = HttpUtil.create(null, 5);
            assertNotNull(httpUtil, "create(null, int) 不应返回 null");
            assertNotNull(httpUtil.getHttpClient(), "create(null, int) 应初始化 HttpClient");
        }

        @Test
        @DisplayName("传入 HTTP 代理配置时对象创建不应受影响")
        void testCreateWithHttpProxyConfig() {
            HttpUtil.ProxyConfig proxyConfig = new HttpUtil.ProxyConfig()
                    .setType(Proxy.Type.HTTP)
                    .setHost(HttpConstant.LOCAL_IP_ADDRESS)
                    .setPort(1);
            HttpUtil httpUtil = HttpUtil.create(proxyConfig);
            assertNotNull(httpUtil, "传入 HTTP 代理配置后仍应创建出对象");
            assertNotNull(httpUtil.getHttpClient(), "传入 HTTP 代理配置后应初始化 HttpClient");
            assertNull(httpUtil.getUrl(), "传入 HTTP 代理配置后请求地址仍应为 null");
        }

        @Test
        @DisplayName("ProxyConfig 默认值应为 127.0.0.1:1080 SOCKS")
        void testProxyConfigDefaults() {
            HttpUtil.ProxyConfig proxyConfig = new HttpUtil.ProxyConfig();
            assertEquals(HttpConstant.LOCAL_IP_ADDRESS, proxyConfig.getHost(), "代理默认地址应为 127.0.0.1");
            assertEquals(1080, proxyConfig.getPort(), "代理默认端口应为 1080");
            assertEquals(Proxy.Type.SOCKS, proxyConfig.getType(), "代理默认类型应为 SOCKS");
        }

        @Test
        @DisplayName("ProxyConfig 的 setter 应为链式并可写回字段")
        void testProxyConfigChain() {
            HttpUtil.ProxyConfig proxyConfig = new HttpUtil.ProxyConfig();
            assertSame(proxyConfig, proxyConfig.setHost("10.1.1.2"), "setHost 应返回自身");
            assertSame(proxyConfig, proxyConfig.setPort(1088), "setPort 应返回自身");
            assertSame(proxyConfig, proxyConfig.setType(Proxy.Type.HTTP), "setType 应返回自身");
            assertEquals("10.1.1.2", proxyConfig.getHost(), "setHost 应写回代理地址");
            assertEquals(1088, proxyConfig.getPort(), "setPort 应写回代理端口");
            assertEquals(Proxy.Type.HTTP, proxyConfig.getType(), "setType 应写回代理类型");
        }

        @Test
        @DisplayName("HttpUtil 构造器应为私有，禁止外部实例化")
        void testConstructorIsPrivate() throws NoSuchMethodException {
            Constructor<HttpUtil> constructor = HttpUtil.class.getDeclaredConstructor();
            assertTrue(Modifier.isPrivate(constructor.getModifiers()), "HttpUtil 构造器应为私有");
            assertThrows(IllegalAccessException.class, constructor::newInstance, "未开放权限时反射实例化应失败");
        }
    }

    @Nested
    @DisplayName("链式赋值与请求头 / Cookie 缓存")
    class ChainTest {
        @Test
        @DisplayName("addHeader 应返回自身并写入请求头集合")
        void testAddHeader() {
            HttpUtil httpUtil = HttpUtil.create();
            assertSame(httpUtil, httpUtil.addHeader(CUSTOM_HEADER, "abc-123"), "addHeader 应返回自身");
            assertEquals("abc-123", httpUtil.getHeaders().get(CUSTOM_HEADER), "addHeader 应写入请求头集合");
            assertEquals(1, httpUtil.getHeaders().size(), "重复添加同名请求头应覆盖而非新增");
            httpUtil.addHeader(CUSTOM_HEADER, "abc-456");
            assertEquals("abc-456", httpUtil.getHeaders().get(CUSTOM_HEADER), "同名请求头应被后值覆盖");
        }

        @Test
        @DisplayName("addCookie 应返回自身并写入 Cookie 集合")
        void testAddCookie() {
            HttpUtil httpUtil = HttpUtil.create();
            assertSame(httpUtil, httpUtil.addCookie("token", "abc"), "addCookie 应返回自身");
            assertSame(httpUtil, httpUtil.addCookie("uid", "18"), "addCookie 应返回自身");
            assertEquals("abc", httpUtil.getCookies().get("token"), "addCookie 应写入 Cookie 集合");
            assertEquals("18", httpUtil.getCookies().get("uid"), "addCookie 应写入 Cookie 集合");
            assertEquals(2, httpUtil.getCookies().size(), "应保存两个 Cookie");
        }

        @Test
        @DisplayName("setCookies(null) 后 Cookie 集合应为 null")
        void testSetCookiesToNull() {
            HttpUtil httpUtil = HttpUtil.create().setCookies(null);
            assertNull(httpUtil.getCookies(), "setCookies(null) 后 Cookie 集合应为 null");
        }

        @Test
        @DisplayName("各 setter 应为链式并可写回字段")
        void testSettersChain() {
            HttpUtil httpUtil = HttpUtil.create();
            assertSame(httpUtil, httpUtil.setUrl(baseUrl), "setUrl 应返回自身");
            assertSame(httpUtil, httpUtil.setBody("{}"), "setBody 应返回自身");
            assertSame(httpUtil, httpUtil.setMethod(HttpMethod.PUT), "setMethod 应返回自身");
            assertSame(httpUtil, httpUtil.setContentType(HttpConstant.ContentType.TEXT_PLAIN), "setContentType 应返回自身");
            assertSame(httpUtil, httpUtil.setHeaders(new java.util.HashMap<>()), "setHeaders 应返回自身");
            assertEquals(baseUrl, httpUtil.getUrl(), "setUrl 应写回请求地址");
            assertEquals("{}", httpUtil.getBody(), "setBody 应写回请求体");
            assertEquals(HttpMethod.PUT, httpUtil.getMethod(), "setMethod 应写回请求方法");
            assertEquals(HttpConstant.ContentType.TEXT_PLAIN, httpUtil.getContentType(), "setContentType 应写回请求体类型");
        }

        @Test
        @DisplayName("setBody(null) 后请求体应为 null")
        void testSetBodyToNull() {
            HttpUtil httpUtil = HttpUtil.create().setBody(null);
            assertNull(httpUtil.getBody(), "setBody(null) 后请求体应为 null");
        }
    }

    @Nested
    @DisplayName("真实请求（本地测试服务器）")
    @Timeout(20)
    class RealRequestTest {
        @Test
        @DisplayName("get() 应返回 200 与响应体，且服务端收到 GET 请求")
        void testGet() {
            HttpResponse<String> response = HttpUtil.create().setUrl(baseUrl).get();
            assertEquals(HttpConstant.Status.OK, response.statusCode(), "响应状态码应为 200");
            assertEquals(RESPONSE_BODY, response.body(), "响应体应为测试服务器返回的内容");
            assertEquals(HttpMethod.GET.name(), recordedMethod, "服务端收到的请求方法应为 GET");
            assertEquals(HttpConstant.ContentType.APPLICATION_JSON_UTF8, recordedContentType,
                    "请求头 Content-Type 应为默认的 application/json;charset=UTF-8");
        }

        @Test
        @DisplayName("send() 在未设置方法时应按默认 GET 发出请求")
        void testSendWithDefaultMethod() {
            HttpResponse<String> response = HttpUtil.create().setUrl(baseUrl).send();
            assertEquals(HttpConstant.Status.OK, response.statusCode(), "响应状态码应为 200");
            assertEquals(HttpMethod.GET.name(), recordedMethod, "默认方法应为 GET");
            assertEquals("", recordedBody, "GET 请求不应携带请求体");
        }

        @Test
        @DisplayName("get() 应覆盖此前设置的 POST 方法")
        void testGetOverridesMethod() {
            HttpResponse<String> response = HttpUtil.create().setUrl(baseUrl).setMethod(HttpMethod.POST).get();
            assertEquals(HttpConstant.Status.OK, response.statusCode(), "响应状态码应为 200");
            assertEquals(HttpMethod.GET.name(), recordedMethod, "get() 应把方法重置为 GET");
        }

        @Test
        @DisplayName("post(body) 应以 POST 方式发送指定请求体")
        void testPostWithBody() {
            HttpResponse<String> response = HttpUtil.create().setUrl(baseUrl).post("{\"name\":\"Hamm\"}");
            assertEquals(HttpConstant.Status.OK, response.statusCode(), "响应状态码应为 200");
            assertEquals(RESPONSE_BODY, response.body(), "响应体应为测试服务器返回的内容");
            assertEquals(HttpMethod.POST.name(), recordedMethod, "服务端收到的请求方法应为 POST");
            assertEquals("{\"name\":\"Hamm\"}", recordedBody, "服务端收到的请求体应与发送内容一致");
        }

        @Test
        @DisplayName("post() 无参重载应发送空请求体")
        void testPostWithoutBody() {
            HttpResponse<String> response = HttpUtil.create().setUrl(baseUrl).post();
            assertEquals(HttpConstant.Status.OK, response.statusCode(), "响应状态码应为 200");
            assertEquals(HttpMethod.POST.name(), recordedMethod, "服务端收到的请求方法应为 POST");
            assertEquals("", recordedBody, "无参 post() 应发送空请求体");
        }

        @Test
        @DisplayName("setMethod(PUT) 应以 PUT 方式发送请求体")
        void testPut() {
            HttpResponse<String> response = HttpUtil.create()
                    .setUrl(baseUrl)
                    .setMethod(HttpMethod.PUT)
                    .setBody("{\"id\":1}")
                    .send();
            assertEquals(HttpConstant.Status.OK, response.statusCode(), "响应状态码应为 200");
            assertEquals(HttpMethod.PUT.name(), recordedMethod, "服务端收到的请求方法应为 PUT");
            assertEquals("{\"id\":1}", recordedBody, "服务端收到的请求体应与发送内容一致");
        }

        @Test
        @DisplayName("setMethod(DELETE) 应以 DELETE 方式发出请求")
        void testDelete() {
            HttpResponse<String> response = HttpUtil.create()
                    .setUrl(baseUrl)
                    .setMethod(HttpMethod.DELETE)
                    .send();
            assertEquals(HttpConstant.Status.OK, response.statusCode(), "响应状态码应为 200");
            assertEquals(HttpMethod.DELETE.name(), recordedMethod, "服务端收到的请求方法应为 DELETE");
        }

        @Test
        @DisplayName("addHeader 添加的自定义请求头应被服务端读到")
        void testCustomHeaderIsTransferred() {
            HttpUtil.create().setUrl(baseUrl).addHeader(CUSTOM_HEADER, "abc-123").get();
            assertEquals("abc-123", recordedCustomHeader, "服务端应收到自定义请求头的值");
        }

        @Test
        @DisplayName("addHeader 的值可以是任意对象，应调用 toString 后发送")
        void testCustomHeaderWithObjectValue() {
            HttpUtil.create().setUrl(baseUrl).addHeader(CUSTOM_HEADER, 10086).get();
            assertEquals("10086", recordedCustomHeader, "非字符串请求头的值应按 toString 发送");
        }

        @Test
        @DisplayName("单个 Cookie 应以 key=value 形式写入 Cookie 请求头")
        void testSingleCookie() {
            HttpUtil.create().setUrl(baseUrl).addCookie("token", "abc").get();
            assertEquals("token=abc", recordedCookie, "服务端收到的 Cookie 请求头应为 token=abc");
        }

        @Test
        @DisplayName("多个 Cookie 应以 \"; \" 拼接后写入 Cookie 请求头")
        void testMultipleCookies() {
            HttpUtil.create().setUrl(baseUrl).addCookie("k", "v").addCookie("k2", "v2").get();
            assertNotNull(recordedCookie, "服务端应收到 Cookie 请求头");
            Set<String> items = new HashSet<>(Arrays.asList(recordedCookie.split("; ")));
            assertEquals(Set.of("k=v", "k2=v2"), items, "多个 Cookie 应以 \"; \" 拼接为 k=v; k2=v2 形式");
        }

        @Test
        @DisplayName("Cookie 集合为 null 时请求仍可正常发出")
        void testCookiesIsNull() {
            HttpResponse<String> response = HttpUtil.create().setUrl(baseUrl).setCookies(null).get();
            assertEquals(HttpConstant.Status.OK, response.statusCode(), "Cookie 为 null 时请求仍应返回 200");
        }

        @Test
        @DisplayName("contentType 为 null 时不应发送 Content-Type 请求头")
        void testContentTypeIsNull() {
            HttpResponse<String> response = HttpUtil.create().setUrl(baseUrl).setContentType(null).get();
            assertEquals(HttpConstant.Status.OK, response.statusCode(), "contentType 为 null 时请求仍应返回 200");
            assertNull(recordedContentType, "contentType 为 null 时不应发送 Content-Type 请求头");
        }

        @Test
        @DisplayName("自定义 contentType 应写入请求头")
        void testCustomContentType() {
            HttpResponse<String> response = HttpUtil.create()
                    .setUrl(baseUrl)
                    .setContentType(HttpConstant.ContentType.TEXT_PLAIN)
                    .post("hello");
            assertEquals(HttpConstant.Status.OK, response.statusCode(), "响应状态码应为 200");
            assertEquals(HttpConstant.ContentType.TEXT_PLAIN, recordedContentType, "应使用自定义的 Content-Type");
            assertEquals("hello", recordedBody, "服务端收到的请求体应与发送内容一致");
        }

        @Test
        @DisplayName("连续发起多次请求应均能成功")
        void testMultipleRequests() {
            HttpUtil httpUtil = HttpUtil.create().setUrl(baseUrl);
            assertEquals(HttpConstant.Status.OK, httpUtil.get().statusCode(), "第 1 次请求应返回 200");
            assertEquals(HttpConstant.Status.OK, httpUtil.get().statusCode(), "第 2 次请求应返回 200");
            assertEquals(RESPONSE_BODY, httpUtil.get().body(), "第 3 次请求的响应体应正确");
        }

        @Test
        @DisplayName("HttpClient 实例在多次调用间应被复用")
        void testHttpClientReused() {
            HttpUtil httpUtil = HttpUtil.create().setUrl(baseUrl);
            HttpClient client = httpUtil.getHttpClient();
            httpUtil.get();
            assertSame(client, httpUtil.getHttpClient(), "同一 HttpUtil 的 HttpClient 应为同一实例");
        }
    }

    @Nested
    @DisplayName("异常分支")
    @Timeout(20)
    class ExceptionTest {
        @Test
        @DisplayName("url 为 null 时应抛出 ServiceException")
        void testUrlIsNull() {
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> HttpUtil.create().get(), "url 为 null 时应抛出业务异常");
            assertTrue(exception.getMessage().startsWith("发起请求失败"), "异常消息应以「发起请求失败」开头");
            assertEquals(Json.SERVICE_ERROR, exception.getCode(), "异常码应为默认的服务错误码");
        }

        @Test
        @DisplayName("url 为非法字符串时应抛出 ServiceException")
        void testUrlIsIllegal() {
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> HttpUtil.create().setUrl("这不是一个URL").get(), "非法 url 应抛出业务异常");
            assertTrue(exception.getMessage().startsWith("发起请求失败"), "异常消息应以「发起请求失败」开头");
        }

        @Test
        @DisplayName("url 为空字符串时应抛出 ServiceException")
        void testUrlIsEmpty() {
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> HttpUtil.create().setUrl("").get(), "空 url 应抛出业务异常");
            assertTrue(exception.getMessage().startsWith("发起请求失败"), "异常消息应以「发起请求失败」开头");
        }

        @Test
        @DisplayName("方法为 PATCH 时应被包装为「发起请求失败，不支持的请求方法」")
        void testUnsupportedMethod() {
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> HttpUtil.create().setUrl(baseUrl).setMethod(HttpMethod.PATCH).send(),
                    "PATCH 不在支持列表中，应抛出业务异常");
            assertEquals("发起请求失败，不支持的请求方法", exception.getMessage(), "异常消息应包含不支持的请求方法提示");
        }

        @Test
        @DisplayName("连接到未监听的端口时应抛出 ServiceException")
        void testConnectRefused() {
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> HttpUtil.create().setUrl("http://127.0.0.1:1/api").get(),
                    "未监听的端口应抛出业务异常");
            assertTrue(exception.getMessage().startsWith("发起请求失败"), "异常消息应以「发起请求失败」开头");
        }
    }
}
