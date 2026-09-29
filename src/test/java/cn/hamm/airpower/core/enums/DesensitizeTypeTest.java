package cn.hamm.airpower.core.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>枚举单元测试</h1>
 *
 * <p>覆盖 {@link DesensitizeType} 与 {@link HttpMethod}。</p>
 *
 * @author Hamm.cn
 */
@DisplayName("枚举单元测试")
class DesensitizeTypeTest {
    @Nested
    @DisplayName("DesensitizeType 脱敏方式")
    class DesensitizeTypeTestInner {
        @Test
        @DisplayName("共 10 个枚举值且顺序固定")
        void testValues() {
            DesensitizeType[] values = DesensitizeType.values();
            assertEquals(10, values.length, "DesensitizeType 应包含 10 个枚举值");
            List<String> names = new ArrayList<>();
            for (DesensitizeType value : values) {
                names.add(value.name());
            }
            assertEquals(List.of("TELEPHONE", "MOBILE", "ID_CARD", "BANK_CARD", "CAR_NUMBER",
                    "EMAIL", "CHINESE_NAME", "ADDRESS", "IP_V4", "CUSTOM"), names, "枚举值的声明顺序应保持一致");
        }

        @Test
        @DisplayName("valueOf 应按名称取到枚举值")
        void testValueOf() {
            assertSame(DesensitizeType.MOBILE, DesensitizeType.valueOf("MOBILE"), "valueOf 应取到 MOBILE");
            assertSame(DesensitizeType.CUSTOM, DesensitizeType.valueOf("CUSTOM"), "valueOf 应取到 CUSTOM");
            assertThrows(IllegalArgumentException.class, () -> DesensitizeType.valueOf("MOBILE_PHONE"),
                    "不存在的名称应抛出非法参数异常");
        }

        @Test
        @DisplayName("TELEPHONE 座机号码的保留位数为 0 和 0")
        void testTelephone() {
            assertEquals(0, DesensitizeType.TELEPHONE.getMinHead(), "TELEPHONE 开始至少保留位数应为 0");
            assertEquals(0, DesensitizeType.TELEPHONE.getMinTail(), "TELEPHONE 结束至少保留位数应为 0");
        }

        @Test
        @DisplayName("MOBILE 手机号码的保留位数为 3 和 4")
        void testMobile() {
            assertEquals(3, DesensitizeType.MOBILE.getMinHead(), "MOBILE 开始至少保留位数应为 3");
            assertEquals(4, DesensitizeType.MOBILE.getMinTail(), "MOBILE 结束至少保留位数应为 4");
        }

        @Test
        @DisplayName("ID_CARD 身份证号的保留位数为 6 和 4")
        void testIdCard() {
            assertEquals(6, DesensitizeType.ID_CARD.getMinHead(), "ID_CARD 开始至少保留位数应为 6");
            assertEquals(4, DesensitizeType.ID_CARD.getMinTail(), "ID_CARD 结束至少保留位数应为 4");
        }

        @Test
        @DisplayName("BANK_CARD 银行卡号的保留位数为 4 和 4")
        void testBankCard() {
            assertEquals(4, DesensitizeType.BANK_CARD.getMinHead(), "BANK_CARD 开始至少保留位数应为 4");
            assertEquals(4, DesensitizeType.BANK_CARD.getMinTail(), "BANK_CARD 结束至少保留位数应为 4");
        }

        @Test
        @DisplayName("CAR_NUMBER 车牌号的保留位数为 2 和 1")
        void testCarNumber() {
            assertEquals(2, DesensitizeType.CAR_NUMBER.getMinHead(), "CAR_NUMBER 开始至少保留位数应为 2");
            assertEquals(1, DesensitizeType.CAR_NUMBER.getMinTail(), "CAR_NUMBER 结束至少保留位数应为 1");
        }

        @Test
        @DisplayName("EMAIL 邮箱的保留位数为 2 和 2")
        void testEmail() {
            assertEquals(2, DesensitizeType.EMAIL.getMinHead(), "EMAIL 开始至少保留位数应为 2");
            assertEquals(2, DesensitizeType.EMAIL.getMinTail(), "EMAIL 结束至少保留位数应为 2");
        }

        @Test
        @DisplayName("CHINESE_NAME 中文名的保留位数为 1 和 1")
        void testChineseName() {
            assertEquals(1, DesensitizeType.CHINESE_NAME.getMinHead(), "CHINESE_NAME 开始至少保留位数应为 1");
            assertEquals(1, DesensitizeType.CHINESE_NAME.getMinTail(), "CHINESE_NAME 结束至少保留位数应为 1");
        }

        @Test
        @DisplayName("ADDRESS 地址的保留位数为 3 和 0")
        void testAddress() {
            assertEquals(3, DesensitizeType.ADDRESS.getMinHead(), "ADDRESS 开始至少保留位数应为 3");
            assertEquals(0, DesensitizeType.ADDRESS.getMinTail(), "ADDRESS 结束至少保留位数应为 0");
        }

        @Test
        @DisplayName("IP_V4 IPv4 地址的保留位数为 0 和 0")
        void testIpV4() {
            assertEquals(0, DesensitizeType.IP_V4.getMinHead(), "IP_V4 开始至少保留位数应为 0");
            assertEquals(0, DesensitizeType.IP_V4.getMinTail(), "IP_V4 结束至少保留位数应为 0");
        }

        @Test
        @DisplayName("CUSTOM 自定义的保留位数为 0 和 0")
        void testCustom() {
            assertEquals(0, DesensitizeType.CUSTOM.getMinHead(), "CUSTOM 开始至少保留位数应为 0");
            assertEquals(0, DesensitizeType.CUSTOM.getMinTail(), "CUSTOM 结束至少保留位数应为 0");
        }

        @Test
        @DisplayName("ordinal 应与声明顺序一致")
        void testOrdinal() {
            assertEquals(0, DesensitizeType.TELEPHONE.ordinal(), "TELEPHONE 的序号应为 0");
            assertEquals(9, DesensitizeType.CUSTOM.ordinal(), "CUSTOM 的序号应为 9");
        }

        @Test
        @DisplayName("toString 应返回枚举名")
        void testToString() {
            assertEquals("BANK_CARD", DesensitizeType.BANK_CARD.toString(), "toString 应返回枚举名");
            assertNotNull(DesensitizeType.MOBILE.name(), "name() 不应为 null");
        }
    }

    @Nested
    @DisplayName("HttpMethod 请求方式")
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
}
