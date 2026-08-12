package cn.hamm.airpower.core;

import cn.hamm.airpower.core.constant.HttpConstant;
import cn.hamm.airpower.core.enums.HttpMethod;
import cn.hamm.airpower.core.exception.ServiceException;
import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>HttpUtil 单元测试</h1>
 *
 * @author Hamm.cn
 */
class HttpUtilTest {

    private static final String TEST_URL = "https://miai.hamm.cn";
    private static final String TEST_BODY = "{\"test\":\"data\"}";

    // ==================== create 方法测试 ====================

    @Test
    void testCreate() {
        HttpUtil httpUtil = HttpUtil.create();
        assertNotNull(httpUtil);
        assertNotNull(httpUtil.getHeaders());
        assertNotNull(httpUtil.getCookies());
        assertEquals("", httpUtil.getBody());
        assertEquals(HttpMethod.GET, httpUtil.getMethod());
        assertEquals(HttpConstant.ContentType.APPLICATION_JSON_UTF8, httpUtil.getContentType());
    }

    // ==================== addHeader 方法测试 ====================

    @Test
    void testAddHeader() {
        HttpUtil httpUtil = HttpUtil.create()
                .addHeader("X-Custom-Header", "value");
        Map<String, Object> headers = httpUtil.getHeaders();
        assertTrue(headers.containsKey("X-Custom-Header"));
        assertEquals("value", headers.get("X-Custom-Header"));
    }

    @Test
    void testAddHeaderWithMultipleHeaders() {
        HttpUtil httpUtil = HttpUtil.create()
                .addHeader("Header1", "Value1")
                .addHeader("Header2", "Value2");
        Map<String, Object> headers = httpUtil.getHeaders();
        assertEquals(2, headers.size());
        assertEquals("Value1", headers.get("Header1"));
        assertEquals("Value2", headers.get("Header2"));
    }

    @Test
    void testAddHeaderWithNullValue() {
        HttpUtil httpUtil = HttpUtil.create()
                .addHeader("Null-Header", null);
        assertNull(httpUtil.getHeaders().get("Null-Header"));
    }

    @Test
    void testAddHeaderOverwriteExisting() {
        HttpUtil httpUtil = HttpUtil.create()
                .addHeader("Same-Header", "Old")
                .addHeader("Same-Header", "New");
        assertEquals("New", httpUtil.getHeaders().get("Same-Header"));
    }

    // ==================== addCookie 方法测试 ====================

    @Test
    void testAddCookie() {
        HttpUtil httpUtil = HttpUtil.create()
                .addCookie("session", "abc123");
        Map<String, Object> cookies = httpUtil.getCookies();
        assertTrue(cookies.containsKey("session"));
        assertEquals("abc123", cookies.get("session"));
    }

    @Test
    void testAddCookieWithMultipleCookies() {
        HttpUtil httpUtil = HttpUtil.create()
                .addCookie("cookie1", "value1")
                .addCookie("cookie2", "value2");
        Map<String, Object> cookies = httpUtil.getCookies();
        assertEquals(2, cookies.size());
        assertEquals("value1", cookies.get("cookie1"));
        assertEquals("value2", cookies.get("cookie2"));
    }

    @Test
    void testAddCookieOverwriteExisting() {
        HttpUtil httpUtil = HttpUtil.create()
                .addCookie("same", "old")
                .addCookie("same", "new");
        assertEquals("new", httpUtil.getCookies().get("same"));
    }

    // ==================== Getter/Setter 测试 ====================

    @Test
    void testSetAndGetUrl() {
        HttpUtil httpUtil = HttpUtil.create();
        httpUtil.setUrl(TEST_URL);
        assertEquals(TEST_URL, httpUtil.getUrl());
    }

    @Test
    void testSetAndGetBody() {
        HttpUtil httpUtil = HttpUtil.create();
        httpUtil.setBody(TEST_BODY);
        assertEquals(TEST_BODY, httpUtil.getBody());
    }

    @Test
    void testSetAndGetMethod() {
        HttpUtil httpUtil = HttpUtil.create();
        httpUtil.setMethod(HttpMethod.POST);
        assertEquals(HttpMethod.POST, httpUtil.getMethod());
    }

    @Test
    void testSetAndGetContentType() {
        HttpUtil httpUtil = HttpUtil.create();
        httpUtil.setContentType(HttpConstant.ContentType.APPLICATION_JSON);
        assertEquals(HttpConstant.ContentType.APPLICATION_JSON, httpUtil.getContentType());
    }

    // ==================== 链式调用测试 ====================

    @Test
    void testFluentApi() {
        HttpUtil httpUtil = HttpUtil.create()
                .setUrl(TEST_URL)
                .setBody(TEST_BODY)
                .setMethod(HttpMethod.POST)
                .setContentType(HttpConstant.ContentType.APPLICATION_JSON)
                .addHeader("Authorization", "Bearer token123")
                .addCookie("session", "abc");

        assertEquals(TEST_URL, httpUtil.getUrl());
        assertEquals(TEST_BODY, httpUtil.getBody());
        assertEquals(HttpMethod.POST, httpUtil.getMethod());
        assertEquals(HttpConstant.ContentType.APPLICATION_JSON, httpUtil.getContentType());
        assertEquals("Bearer token123", httpUtil.getHeaders().get("Authorization"));
        assertEquals("abc", httpUtil.getCookies().get("session"));
    }

    // ==================== 实际网络请求测试 ====================

    @Test
    void testGetRequest() {
        HttpUtil httpUtil = HttpUtil.create()
                .setUrl(TEST_URL);
        HttpResponse<String> response = httpUtil.get();
        assertNotNull(response);
        assertEquals(200, response.statusCode());
        assertNotNull(response.body());
    }

    @Test
    void testPostRequest() {
        HttpUtil httpUtil = HttpUtil.create()
                .setUrl(TEST_URL)
                .setBody(TEST_BODY);
        HttpResponse<String> response = httpUtil.post();
        assertNotNull(response);
        assertEquals(200, response.statusCode());
    }

    @Test
    void testPostRequestWithBodyParameter() {
        HttpUtil httpUtil = HttpUtil.create()
                .setUrl(TEST_URL);
        HttpResponse<String> response = httpUtil.post(TEST_BODY);
        assertNotNull(response);
        assertEquals(200, response.statusCode());
        assertEquals(TEST_BODY, httpUtil.getBody());
    }

    @Test
    void testSendWithCustomHeaders() {
        HttpUtil httpUtil = HttpUtil.create()
                .setUrl(TEST_URL)
                .addHeader("User-Agent", "HttpUtilTest/1.0")
                .addHeader("Accept", "application/json");
        HttpResponse<String> response = httpUtil.get();
        assertNotNull(response);
        assertEquals(200, response.statusCode());
    }

    @Test
    void testSendWithCookies() {
        HttpUtil httpUtil = HttpUtil.create()
                .setUrl(TEST_URL)
                .addCookie("test", "value");
        HttpResponse<String> response = httpUtil.get();
        assertNotNull(response);
        assertEquals(200, response.statusCode());
    }

    // ==================== 错误处理测试 ====================

    @Test
    void testSendWithInvalidUrl() {
        HttpUtil httpUtil = HttpUtil.create()
                .setUrl("not-a-valid-url");
        assertThrows(ServiceException.class, () -> httpUtil.get());
    }

    @Test
    void testSendWithNullUrl() {
        HttpUtil httpUtil = HttpUtil.create()
                .setUrl(null);
        assertThrows(ServiceException.class, () -> httpUtil.get());
    }

    @Test
    void testSendWithUnsupportedMethod() {
        HttpUtil httpUtil = HttpUtil.create()
                .setUrl(TEST_URL)
                .setMethod(HttpMethod.PATCH);
        assertThrows(ServiceException.class, () -> httpUtil.send());
    }

    // ==================== 默认状态测试 ====================

    @Test
    void testDefaultState() {
        HttpUtil httpUtil = HttpUtil.create();
        assertNotNull(httpUtil.getHeaders());
        assertTrue(httpUtil.getHeaders().isEmpty());
        assertNotNull(httpUtil.getCookies());
        assertTrue(httpUtil.getCookies().isEmpty());
        assertEquals("", httpUtil.getBody());
        assertEquals(HttpMethod.GET, httpUtil.getMethod());
        assertEquals(HttpConstant.ContentType.APPLICATION_JSON_UTF8, httpUtil.getContentType());
        assertNull(httpUtil.getUrl());
    }
}
