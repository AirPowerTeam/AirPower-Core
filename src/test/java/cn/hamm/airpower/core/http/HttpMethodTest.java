package cn.hamm.airpower.core.http;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>HTTP 请求方式枚举测试</h1>
 *
 * <p>{@link HttpUtil#getHttpRequest()} 用 {@code switch} 直接枚举全部取值，
 * 增删枚举值会静默改变请求方法，因此锁定数量、名称与顺序。</p>
 *
 * @author Hamm.cn
 */
@DisplayName("HttpMethod HTTP 请求方式枚举")
class HttpMethodTest {

    @Test
    @DisplayName("共 5 个枚举值且顺序为 GET/POST/PUT/DELETE/PATCH")
    void testValues() {
        HttpMethod[] values = HttpMethod.values();
        assertEquals(5, values.length, "HttpMethod 应包含 5 个枚举值");
        List<String> names = new ArrayList<>();
        for (HttpMethod value : values) {
            names.add(value.name());
        }
        assertEquals(List.of("GET", "POST", "PUT", "DELETE", "PATCH"), names, "枚举值的声明顺序应保持一致");
    }

    @Test
    @DisplayName("每个枚举值的名称应与常量名一致")
    void testNames() {
        assertEquals("GET", HttpMethod.GET.name(), "GET 的名称应为 GET");
        assertEquals("POST", HttpMethod.POST.name(), "POST 的名称应为 POST");
        assertEquals("PUT", HttpMethod.PUT.name(), "PUT 的名称应为 PUT");
        assertEquals("DELETE", HttpMethod.DELETE.name(), "DELETE 的名称应为 DELETE");
        assertEquals("PATCH", HttpMethod.PATCH.name(), "PATCH 的名称应为 PATCH");
    }

    @Test
    @DisplayName("valueOf 应按名称取到枚举值")
    void testValueOf() {
        assertSame(HttpMethod.GET, HttpMethod.valueOf("GET"), "valueOf 应取到 GET");
        assertSame(HttpMethod.PATCH, HttpMethod.valueOf("PATCH"), "valueOf 应取到 PATCH");
        assertThrows(IllegalArgumentException.class, () -> HttpMethod.valueOf("HEAD"),
                "不存在的名称应抛出非法参数异常");
    }

    @Test
    @DisplayName("ordinal 应与声明顺序一致")
    void testOrdinal() {
        assertEquals(0, HttpMethod.GET.ordinal(), "GET 的序号应为 0");
        assertEquals(1, HttpMethod.POST.ordinal(), "POST 的序号应为 1");
        assertEquals(2, HttpMethod.PUT.ordinal(), "PUT 的序号应为 2");
        assertEquals(3, HttpMethod.DELETE.ordinal(), "DELETE 的序号应为 3");
        assertEquals(4, HttpMethod.PATCH.ordinal(), "PATCH 的序号应为 4");
    }
}
