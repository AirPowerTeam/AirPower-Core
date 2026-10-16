package cn.hamm.airpower.core.http;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <h2>HTTP 常量单元测试</h2>
 *
 * <p>覆盖 {@link HttpConstant} 与全部内部常量类。这些常量直接参与请求拼装与响应判定，
 * 一旦被误改，线上会表现为「请求头莫名多一个」或「错误码判定失效」。</p>
 *
 * @author Hamm.cn
 */
@DisplayName("HttpConstant HTTP 常量")
class HttpConstantTest {

    @Test
    @DisplayName("本地地址常量")
    void testLocalAddress() {
        assertEquals("127.0.0.1", HttpConstant.LOCAL_IP_ADDRESS, "LOCAL_IP_ADDRESS 应为 127.0.0.1");
        assertEquals("localhost", HttpConstant.LOCAL_HOST, "LOCAL_HOST 应为 localhost");
    }

    @Test
    @DisplayName("Status 状态码常量")
    void testStatus() {
        assertEquals(200, HttpConstant.Status.OK, "Status.OK 应为 200");
        assertEquals(500, HttpConstant.Status.INTERNAL_SERVER_ERROR, "Status.INTERNAL_SERVER_ERROR 应为 500");
    }

    @Test
    @DisplayName("GrantType 授权类型常量（7 个）")
    void testGrantType() {
        assertEquals("password", HttpConstant.GrantType.PASSWORD, "PASSWORD 应为 password");
        assertEquals("refresh_token", HttpConstant.GrantType.REFRESH_TOKEN, "REFRESH_TOKEN 应为 refresh_token");
        assertEquals("client_credentials", HttpConstant.GrantType.CLIENT_CREDENTIALS,
                "CLIENT_CREDENTIALS 应为 client_credentials");
        assertEquals("authorization_code", HttpConstant.GrantType.AUTHORIZATION_CODE,
                "AUTHORIZATION_CODE 应为 authorization_code");
        assertEquals("implicit", HttpConstant.GrantType.IMPLICIT, "IMPLICIT 应为 implicit");
        assertEquals("Bearer", HttpConstant.GrantType.BEARER, "BEARER 应为 Bearer");
        assertEquals("Basic", HttpConstant.GrantType.BASIC, "BASIC 应为 Basic");
    }

    @Test
    @DisplayName("Header 请求头常量（6 个）")
    void testHeader() {
        assertEquals("Content-Type", HttpConstant.Header.CONTENT_TYPE, "CONTENT_TYPE 应为 Content-Type");
        assertEquals("Cookie", HttpConstant.Header.COOKIE, "COOKIE 应为 Cookie");
        assertEquals("X-Request-ID", HttpConstant.Header.REQUEST_ID, "REQUEST_ID 应为 X-Request-ID");
        assertEquals("X-Trace-ID", HttpConstant.Header.TRACE_ID, "TRACE_ID 应为 X-Trace-ID");
        assertEquals("Authorization", HttpConstant.Header.AUTHORIZATION, "AUTHORIZATION 应为 Authorization");
        assertEquals("User-Agent", HttpConstant.Header.USER_AGENT, "USER_AGENT 应为 User-Agent");
    }

    @Test
    @DisplayName("ContentType 内容类型常量（6 个）")
    void testContentType() {
        assertEquals("application/json", HttpConstant.ContentType.APPLICATION_JSON,
                "APPLICATION_JSON 应为 application/json");
        assertEquals("application/json;charset=UTF-8", HttpConstant.ContentType.APPLICATION_JSON_UTF8,
                "APPLICATION_JSON_UTF8 应为 application/json;charset=UTF-8");
        assertEquals("application/x-www-form-urlencoded", HttpConstant.ContentType.APPLICATION_FORM_URLENCODED,
                "APPLICATION_FORM_URLENCODED 应为 application/x-www-form-urlencoded");
        assertEquals("multipart/form-data", HttpConstant.ContentType.MULTIPART_FORM_DATA,
                "MULTIPART_FORM_DATA 应为 multipart/form-data");
        assertEquals("text/html", HttpConstant.ContentType.TEXT_HTML, "TEXT_HTML 应为 text/html");
        assertEquals("text/plain", HttpConstant.ContentType.TEXT_PLAIN, "TEXT_PLAIN 应为 text/plain");
    }

    @Test
    @DisplayName("Proxy.Header 代理请求头常量（7 个）")
    void testProxyHeader() {
        assertEquals("Forwarded", HttpConstant.Proxy.Header.FORWARD, "FORWARD 应为 Forwarded");
        assertEquals("X-Real-IP", HttpConstant.Proxy.Header.X_REAL_IP, "X_REAL_IP 应为 X-Real-IP");
        assertEquals("X-Forwarded-For", HttpConstant.Proxy.Header.X_FORWARDED_FOR,
                "X_FORWARDED_FOR 应为 X-Forwarded-For");
        assertEquals("Proxy-Client-IP", HttpConstant.Proxy.Header.PROXY_CLIENT_IP,
                "PROXY_CLIENT_IP 应为 Proxy-Client-IP");
        assertEquals("WL-Proxy-Client-IP", HttpConstant.Proxy.Header.WL_PROXY_CLIENT_IP,
                "WL_PROXY_CLIENT_IP 应为 WL-Proxy-Client-IP");
        assertEquals("HTTP_CLIENT_IP", HttpConstant.Proxy.Header.HTTP_CLIENT_IP,
                "HTTP_CLIENT_IP 应为 HTTP_CLIENT_IP");
        assertEquals("HTTP_X_FORWARDED_FOR", HttpConstant.Proxy.Header.HTTP_X_FORWARDED_FOR,
                "HTTP_X_FORWARDED_FOR 应为 HTTP_X_FORWARDED_FOR");
    }

    @Test
    @DisplayName("HttpConstant 及内部常量类均禁止外部实例化")
    void testNotInstantiable() throws NoSuchMethodException {
        List<Class<?>> classes = List.of(HttpConstant.class, HttpConstant.Status.class,
                HttpConstant.GrantType.class, HttpConstant.Header.class, HttpConstant.ContentType.class,
                HttpConstant.Proxy.class, HttpConstant.Proxy.Header.class);
        for (Class<?> clazz : classes) {
            assertTrue(Modifier.isPrivate(clazz.getDeclaredConstructor().getModifiers()),
                    clazz.getSimpleName() + " 的构造器应为私有");
        }
    }

    @Test
    @DisplayName("常量应为编译期常量（static final）")
    void testModifiers() throws NoSuchFieldException {
        for (String fieldName : new String[]{"LOCAL_IP_ADDRESS", "LOCAL_HOST"}) {
            int modifiers = HttpConstant.class.getDeclaredField(fieldName).getModifiers();
            assertTrue(Modifier.isPublic(modifiers), fieldName + " 应为 public");
            assertTrue(Modifier.isStatic(modifiers), fieldName + " 应为 static");
            assertTrue(Modifier.isFinal(modifiers), fieldName + " 应为 final");
        }
    }
}
