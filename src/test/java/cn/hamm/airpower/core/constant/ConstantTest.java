package cn.hamm.airpower.core.constant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>常量类单元测试</h1>
 *
 * <p>覆盖 {@link Constant}、{@link HttpConstant}（含全部内部常量类）与 {@link PatternConstant}。</p>
 *
 * @author Hamm.cn
 */
@DisplayName("常量类单元测试")
class ConstantTest {
    @Nested
    @DisplayName("Constant")
    class ConstantInnerTest {
        @Test
        @DisplayName("ID 常量应为 id")
        void testId() {
            assertEquals("id", Constant.ID, "Constant.ID 应为 id");
        }

        @Test
        @DisplayName("Constant 禁止外部实例化（私有构造器）")
        void testNotInstantiable() throws NoSuchMethodException {
            Constructor<Constant> constructor = Constant.class.getDeclaredConstructor();
            assertTrue(Modifier.isPrivate(constructor.getModifiers()), "Constant 的构造器应为私有");
        }
    }

    @Nested
    @DisplayName("HttpConstant")
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
        @DisplayName("Proxy.Header 代理请求头常量（5 个）")
        void testProxyHeader() {
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

    @Nested
    @DisplayName("PatternConstant 私有构造器")
    class PatternConstructorTest {
        @Test
        @DisplayName("构造器应为私有且禁止外部实例化")
        void testPrivateConstructor() throws NoSuchMethodException {
            Constructor<PatternConstant> constructor = PatternConstant.class.getDeclaredConstructor();
            assertTrue(Modifier.isPrivate(constructor.getModifiers()), "PatternConstant 构造器应为私有");
            assertThrows(IllegalAccessException.class, constructor::newInstance, "未开放权限时反射实例化应失败");
        }

        @Test
        @DisplayName("仅有一个私有构造器")
        void testOnlyOneConstructor() {
            assertEquals(1, PatternConstant.class.getDeclaredConstructors().length, "PatternConstant 应仅有一个构造器");
        }
    }

    @Nested
    @DisplayName("PatternConstant 正则匹配")
    class PatternMatchTest {
        /**
         * 断言正则匹配结果
         *
         * @param pattern     被测正则
         * @param input       测试串
         * @param expected    预期结果
         * @param description 用例说明
         */
        private void assertMatch(Pattern pattern, String input, boolean expected, String description) {
            assertEquals(expected, pattern.matcher(input).matches(), description);
        }

        @Test
        @DisplayName("NUMBER 数字")
        void testNumber() {
            assertMatch(PatternConstant.NUMBER, "123", true, "纯整数应匹配数字");
            assertMatch(PatternConstant.NUMBER, "-1.5", true, "负小数应匹配数字");
            assertMatch(PatternConstant.NUMBER, "abc", false, "字母不应匹配数字");
            assertMatch(PatternConstant.NUMBER, "", false, "空串不应匹配数字");
            assertMatch(PatternConstant.NUMBER, "1.2.3", false, "多小数点不应匹配数字");
        }

        @Test
        @DisplayName("LETTER 字母")
        void testLetter() {
            assertMatch(PatternConstant.LETTER, "abcXYZ", true, "纯字母应匹配字母");
            assertMatch(PatternConstant.LETTER, "abc123", false, "字母与数字组合不应匹配字母");
            assertMatch(PatternConstant.LETTER, "", false, "空串不应匹配字母");
        }

        @Test
        @DisplayName("INTEGER 整数")
        void testInteger() {
            assertMatch(PatternConstant.INTEGER, "-42", true, "负整数应匹配整数");
            assertMatch(PatternConstant.INTEGER, "0", true, "0 应匹配整数");
            assertMatch(PatternConstant.INTEGER, "3.14", false, "小数不应匹配整数");
            assertMatch(PatternConstant.INTEGER, "abc", false, "字母不应匹配整数");
        }

        @Test
        @DisplayName("EMAIL 邮箱")
        void testEmail() {
            assertMatch(PatternConstant.EMAIL, "hamm@airpower.cn", true, "普通邮箱应匹配");
            assertMatch(PatternConstant.EMAIL, "hamm.cn@gmail.com", true, "带点的用户名应匹配");
            assertMatch(PatternConstant.EMAIL, "bad@@airpower.cn", false, "双 @ 不应匹配邮箱");
            assertMatch(PatternConstant.EMAIL, "plainaddress", false, "缺少域名不应匹配邮箱");
            assertMatch(PatternConstant.EMAIL, "ha..mm@airpower.cn", false, "连续两个点不应匹配邮箱");
        }

        @Test
        @DisplayName("LETTER_OR_NUMBER 字母或数字")
        void testLetterOrNumber() {
            assertMatch(PatternConstant.LETTER_OR_NUMBER, "abc123", true, "字母数字组合应匹配");
            assertMatch(PatternConstant.LETTER_OR_NUMBER, "abc-123", false, "包含短横线不应匹配");
            assertMatch(PatternConstant.LETTER_OR_NUMBER, "", false, "空串不应匹配");
        }

        @Test
        @DisplayName("CHINESE 中文（允许空串）")
        void testChinese() {
            assertMatch(PatternConstant.CHINESE, "中文测试", true, "中文应匹配");
            assertMatch(PatternConstant.CHINESE, "", true, "空串应匹配（正则使用 * 量词）");
            assertMatch(PatternConstant.CHINESE, "abc", false, "字母不应匹配中文");
            assertMatch(PatternConstant.CHINESE, "中文abc", false, "中英文混合不应匹配中文");
        }

        @Test
        @DisplayName("MOBILE_PHONE 手机号")
        void testMobilePhone() {
            assertMatch(PatternConstant.MOBILE_PHONE, "13800138000", true, "标准 11 位手机号应匹配");
            assertMatch(PatternConstant.MOBILE_PHONE, "+8613800138000", true, "带国际区号的手机号应匹配");
            assertMatch(PatternConstant.MOBILE_PHONE, "23800138000", false, "第二位非 3-9 不应匹配");
            assertMatch(PatternConstant.MOBILE_PHONE, "1380013800", false, "位数不足 11 位不应匹配");
        }

        @Test
        @DisplayName("TEL_PHONE 座机电话")
        void testTelPhone() {
            assertMatch(PatternConstant.TEL_PHONE, "0755-12345678", true, "带区号的座机应匹配");
            assertMatch(PatternConstant.TEL_PHONE, "12345678", true, "8 位纯数字应匹配");
            assertMatch(PatternConstant.TEL_PHONE, "4001234567", true, "400 客服号应匹配");
            assertMatch(PatternConstant.TEL_PHONE, "8001234567", true, "800 客服号应匹配");
            assertMatch(PatternConstant.TEL_PHONE, "123", false, "位数过少不应匹配");
            assertMatch(PatternConstant.TEL_PHONE, "0755-1234", false, "带区号但位数不足不应匹配");
        }

        @Test
        @DisplayName("NORMAL_CODE 普通字符（单字符）")
        void testNormalCode() {
            assertMatch(PatternConstant.NORMAL_CODE, "a", true, "小写字母应匹配");
            assertMatch(PatternConstant.NORMAL_CODE, "7", true, "数字应匹配");
            assertMatch(PatternConstant.NORMAL_CODE, "@", true, "@ 应匹配");
            assertMatch(PatternConstant.NORMAL_CODE, "中", true, "中文应匹配");
            assertMatch(PatternConstant.NORMAL_CODE, "-", true, "短横线应匹配");
            assertMatch(PatternConstant.NORMAL_CODE, "/", true, "斜杠应匹配");
            assertMatch(PatternConstant.NORMAL_CODE, "\\", true, "反斜杠应匹配");
            assertMatch(PatternConstant.NORMAL_CODE, "", false, "空串不应匹配");
            assertMatch(PatternConstant.NORMAL_CODE, "ab", false, "多字符不应匹配");
            assertMatch(PatternConstant.NORMAL_CODE, "*", false, "* 不在允许的字符集内");
        }

        @Test
        @DisplayName("NUMBER_OR_LETTER 数字或字母")
        void testNumberOrLetter() {
            assertMatch(PatternConstant.NUMBER_OR_LETTER, "abc123", true, "字母数字组合应匹配");
            assertMatch(PatternConstant.NUMBER_OR_LETTER, "abc-1", false, "包含短横线不应匹配");
            assertMatch(PatternConstant.NUMBER_OR_LETTER, "", false, "空串不应匹配");
        }

        @Test
        @DisplayName("NATURAL_NUMBER 自然数（不含负号）")
        void testNaturalNumber() {
            assertMatch(PatternConstant.NATURAL_NUMBER, "0", true, "0 应匹配自然数");
            assertMatch(PatternConstant.NATURAL_NUMBER, "3.14", true, "非负小数应匹配自然数");
            assertMatch(PatternConstant.NATURAL_NUMBER, "-1", false, "负数不应匹配自然数");
            assertMatch(PatternConstant.NATURAL_NUMBER, "1.", false, "缺少小数位不应匹配自然数");
            assertMatch(PatternConstant.NATURAL_NUMBER, "abc", false, "字母不应匹配自然数");
        }

        @Test
        @DisplayName("NATURAL_INTEGER 自然整数（不含负号与小数）")
        void testNaturalInteger() {
            assertMatch(PatternConstant.NATURAL_INTEGER, "0", true, "0 应匹配自然整数");
            assertMatch(PatternConstant.NATURAL_INTEGER, "12345", true, "正整数应匹配自然整数");
            assertMatch(PatternConstant.NATURAL_INTEGER, "-1", false, "负数不应匹配自然整数");
            assertMatch(PatternConstant.NATURAL_INTEGER, "1.5", false, "小数不应匹配自然整数");
            assertMatch(PatternConstant.NATURAL_INTEGER, "", false, "空串不应匹配自然整数");
        }

        @Test
        @DisplayName("全部 12 个正则均已初始化且线程安全")
        void testAllPatterns() {
            Pattern[] patterns = {
                    PatternConstant.NUMBER, PatternConstant.LETTER, PatternConstant.INTEGER, PatternConstant.EMAIL,
                    PatternConstant.LETTER_OR_NUMBER, PatternConstant.CHINESE, PatternConstant.MOBILE_PHONE,
                    PatternConstant.TEL_PHONE, PatternConstant.NORMAL_CODE, PatternConstant.NUMBER_OR_LETTER,
                    PatternConstant.NATURAL_NUMBER, PatternConstant.NATURAL_INTEGER,
            };
            assertEquals(12, patterns.length, "PatternConstant 应定义 12 个正则常量");
            for (Pattern pattern : patterns) {
                assertNotNull(pattern, "正则常量不应为 null");
            }
            // Pattern 本身线程安全：连续多次匹配结果应一致
            assertSame(PatternConstant.NUMBER, PatternConstant.NUMBER, "PatternConstant 应持有同一 Pattern 实例");
            assertTrue(PatternConstant.NUMBER.matcher("123").matches(), "重复匹配结果应保持一致");
            assertTrue(PatternConstant.NUMBER.matcher("123").matches(), "重复匹配结果应保持一致");
            assertFalse(PatternConstant.NUMBER.matcher("abc").matches(), "重复匹配结果应保持一致");
        }
    }
}
