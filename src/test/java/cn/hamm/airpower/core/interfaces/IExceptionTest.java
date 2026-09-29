package cn.hamm.airpower.core.interfaces;

import cn.hamm.airpower.core.exception.ServiceException;
import cn.hamm.airpower.core.fixture.DemoError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>IException 单元测试</h1>
 *
 * <p>使用测试夹具 {@link DemoError} 枚举，覆盖 {@code IException} 的全部默认方法与重载。</p>
 *
 * @author Hamm.cn
 */
@DisplayName("IException 单元测试")
class IExceptionTest {
    /**
     * 被测的错误枚举：参数错误
     */
    private static final DemoError ERROR = DemoError.PARAM_ERROR;

    /**
     * 断言会抛出 code 为 400 的业务异常
     *
     * @param message      预期异常消息
     * @param expectedData 预期异常数据
     * @param executable   被测代码块
     * @return 捕获到的异常
     */
    private static ServiceException assertShow(String message, Object expectedData, Executable executable) {
        ServiceException exception = assertThrows(ServiceException.class, executable, "应抛出业务异常");
        assertEquals(400, exception.getCode(), "异常码应取自 IException 的 400");
        assertEquals(message, exception.getMessage(), "异常消息应符合预期");
        assertEquals(expectedData, exception.getData(), "异常数据应符合预期");
        return exception;
    }

    @Nested
    @DisplayName("getCode / getMessage / get")
    class BasicTest {
        @Test
        @DisplayName("getCode 与 getMessage 应返回枚举定义的值")
        void testCodeAndMessage() {
            assertEquals(400, DemoError.PARAM_ERROR.getCode(), "PARAM_ERROR 的错误码应为 400");
            assertEquals("参数错误", DemoError.PARAM_ERROR.getMessage(), "PARAM_ERROR 的信息应为参数错误");
            assertEquals(401, DemoError.UNAUTHORIZED.getCode(), "UNAUTHORIZED 的错误码应为 401");
            assertEquals("未授权，请先登录", DemoError.UNAUTHORIZED.getMessage(), "UNAUTHORIZED 的信息应为未授权，请先登录");
            assertEquals(500, DemoError.UNKNOWN.getCode(), "UNKNOWN 的错误码应为 500");
            assertEquals("未知错误", DemoError.UNKNOWN.getMessage(), "UNKNOWN 的信息应为未知错误");
        }

        @Test
        @DisplayName("枚举共 3 个成员")
        void testValues() {
            assertEquals(3, DemoError.values().length, "DemoError 应包含 3 个枚举值");
            assertEquals(DemoError.PARAM_ERROR, DemoError.valueOf("PARAM_ERROR"), "valueOf 应按名称取到枚举值");
        }

        @Test
        @DisplayName("get() 应返回自身")
        void testGet() {
            assertSame(DemoError.PARAM_ERROR, DemoError.PARAM_ERROR.get(), "get() 应返回枚举自身");
        }

        @Test
        @DisplayName("IException 可作为 Supplier 使用")
        void testAsSupplier() {
            Supplier<DemoError> supplier = DemoError.UNKNOWN;
            assertSame(DemoError.UNKNOWN, supplier.get(), "作为 Supplier 时 get() 应返回枚举自身");
        }
    }

    @Nested
    @DisplayName("show 系列方法")
    class ShowTest {
        @Test
        @DisplayName("show() 应使用自身的 code 与 message 抛出业务异常")
        void testShow() {
            assertShow("参数错误", null, ERROR::show);
        }

        @Test
        @DisplayName("show(message) 应使用自定义消息并保留自身错误码")
        void testShowWithMessage() {
            assertShow("用户名不能为空", null, () -> ERROR.show("用户名不能为空"));
        }

        @Test
        @DisplayName("show(message, data) 应携带自定义数据")
        void testShowWithMessageAndData() {
            assertShow("用户名不能为空", List.of("username"), () -> ERROR.show("用户名不能为空", List.of("username")));
        }

        @Test
        @DisplayName("不同枚举抛出的异常码应各不相同")
        void testShowCodeDiffersByEnum() {
            ServiceException unauthorized = assertThrows(ServiceException.class, DemoError.UNAUTHORIZED::show,
                    "UNAUTHORIZED 应抛出业务异常");
            assertEquals(401, unauthorized.getCode(), "UNAUTHORIZED 的异常码应为 401");
            ServiceException unknown = assertThrows(ServiceException.class, DemoError.UNKNOWN::show,
                    "UNKNOWN 应抛出业务异常");
            assertEquals(500, unknown.getCode(), "UNKNOWN 的异常码应为 500");
        }
    }

    @Nested
    @DisplayName("when 条件断言")
    class WhenTest {
        @Test
        @DisplayName("when(false) 不应抛异常")
        void testWhenFalse() {
            assertDoesNotThrow(() -> ERROR.when(false), "条件为 false 时不应抛异常");
        }

        @Test
        @DisplayName("when(true) 应使用默认消息抛出异常")
        void testWhenTrue() {
            assertShow("参数错误", null, () -> ERROR.when(true));
        }

        @Test
        @DisplayName("when(false, message) 不应抛异常")
        void testWhenFalseWithMessage() {
            assertDoesNotThrow(() -> ERROR.when(false, "自定义消息"), "条件为 false 时不应抛异常");
        }

        @Test
        @DisplayName("when(true, message) 应使用自定义消息")
        void testWhenTrueWithMessage() {
            assertShow("自定义消息", null, () -> ERROR.when(true, "自定义消息"));
        }

        @Test
        @DisplayName("when(false, message, data) 不应抛异常")
        void testWhenFalseWithMessageAndData() {
            assertDoesNotThrow(() -> ERROR.when(false, "自定义消息", "数据"), "条件为 false 时不应抛异常");
        }

        @Test
        @DisplayName("when(true, message, data) 应携带自定义数据")
        void testWhenTrueWithMessageAndData() {
            assertShow("自定义消息", "数据", () -> ERROR.when(true, "自定义消息", "数据"));
        }
    }

    @Nested
    @DisplayName("whenNull 空值断言")
    class WhenNullTest {
        @Test
        @DisplayName("whenNull(null) 应抛出默认消息的业务异常")
        void testWhenNull() {
            assertShow("参数错误", null, () -> ERROR.whenNull(null));
        }

        @Test
        @DisplayName("whenNull(非空对象) 不应抛异常")
        void testWhenNullWithNotNull() {
            assertDoesNotThrow(() -> ERROR.whenNull("abc"), "非空对象不应触发异常");
            assertDoesNotThrow(() -> ERROR.whenNull(0), "数字 0 不应触发异常");
        }

        @Test
        @DisplayName("whenNull(null, message) 应使用自定义消息")
        void testWhenNullWithMessage() {
            assertShow("用户编号不能为空", null, () -> ERROR.whenNull(null, "用户编号不能为空"));
        }

        @Test
        @DisplayName("whenNull(非空对象, message) 不应抛异常")
        void testWhenNullWithMessageAndNotNull() {
            assertDoesNotThrow(() -> ERROR.whenNull(new Object(), "用户编号不能为空"), "非空对象不应触发异常");
        }
    }

    @Nested
    @DisplayName("whenEquals 相等断言")
    class WhenEqualsTest {
        @Test
        @DisplayName("whenEquals(Object, Object) 两值相同应抛异常")
        void testObjectEqualsSame() {
            assertShow("参数错误", null, () -> ERROR.whenEquals(1, 1));
        }

        @Test
        @DisplayName("whenEquals(Object, Object) 两值不同不应抛异常")
        void testObjectEqualsDifferent() {
            assertDoesNotThrow(() -> ERROR.whenEquals(1, 2), "两值不同不应触发异常");
            assertDoesNotThrow(() -> ERROR.whenEquals("abc", 1), "类型不同不应触发异常");
        }

        @Test
        @DisplayName("whenEquals(Object, Object) 两个 null 也视为相同")
        void testObjectEqualsBothNull() {
            assertShow("参数错误", null, () -> ERROR.whenEquals((Object) null, (Object) null));
        }

        @Test
        @DisplayName("whenEquals(Object, Object, message) 应使用自定义消息")
        void testObjectEqualsWithMessage() {
            assertShow("新旧密码不能相同", null, () -> ERROR.whenEquals("123", "123", "新旧密码不能相同"));
        }

        @Test
        @DisplayName("whenEquals(String, String) 两个字符串字面量应走 String 重载")
        void testStringEqualsSame() {
            assertShow("参数错误", null, () -> ERROR.whenEquals("abc", "abc"));
        }

        @Test
        @DisplayName("whenEquals(String, String) 大小写不同不应抛异常")
        void testStringEqualsDifferent() {
            assertDoesNotThrow(() -> ERROR.whenEquals("abc", "ABC"), "大小写不同不应触发异常");
        }

        @Test
        @DisplayName("whenEquals(String, String, message) 应使用自定义消息")
        void testStringEqualsWithMessage() {
            assertShow("两次输入不一致", null, () -> ERROR.whenEquals("abc", "abc", "两次输入不一致"));
        }
    }

    @Nested
    @DisplayName("whenEqualsIgnoreCase 忽略大小写相等断言")
    class WhenEqualsIgnoreCaseTest {
        @Test
        @DisplayName("忽略大小写后相同应抛异常")
        void testIgnoreCaseEquals() {
            assertShow("参数错误", null, () -> ERROR.whenEqualsIgnoreCase("AbC", "aBc"));
        }

        @Test
        @DisplayName("忽略大小写后不同不应抛异常")
        void testIgnoreCaseNotEquals() {
            assertDoesNotThrow(() -> ERROR.whenEqualsIgnoreCase("abc", "xyz"), "内容不同不应触发异常");
        }

        @Test
        @DisplayName("带自定义消息的重载应使用自定义消息")
        void testIgnoreCaseEqualsWithMessage() {
            assertShow("标识不能重复", null, () -> ERROR.whenEqualsIgnoreCase("AbC", "aBc", "标识不能重复"));
        }

        @Test
        @DisplayName("任一参数为 null 时直接抛出异常（源码先判空）")
        void testIgnoreCaseEqualsWithNull() {
            assertShow("参数错误", null, () -> ERROR.whenEqualsIgnoreCase(null, "abc"));
            assertShow("参数错误", null, () -> ERROR.whenEqualsIgnoreCase("abc", null));
            assertShow("自定义消息", null, () -> ERROR.whenEqualsIgnoreCase(null, null, "自定义消息"));
        }
    }

    @Nested
    @DisplayName("whenNotEquals 不相等断言")
    class WhenNotEqualsTest {
        @Test
        @DisplayName("whenNotEquals(Object, Object) 两值不同应抛异常")
        void testObjectNotEquals() {
            assertShow("参数错误", null, () -> ERROR.whenNotEquals(1, 2));
        }

        @Test
        @DisplayName("whenNotEquals(Object, Object) 两值相同不应抛异常")
        void testObjectNotEqualsSame() {
            assertDoesNotThrow(() -> ERROR.whenNotEquals(1, 1), "两值相同不应触发异常");
            assertDoesNotThrow(() -> ERROR.whenNotEquals((Object) null, (Object) null), "两个 null 不应触发异常");
        }

        @Test
        @DisplayName("whenNotEquals(Object, Object, message) 应使用自定义消息")
        void testObjectNotEqualsWithMessage() {
            assertShow("旧密码不正确", null, () -> ERROR.whenNotEquals("abc", "123", "旧密码不正确"));
        }

        @Test
        @DisplayName("whenNotEquals(String, String) 两个字符串字面量应走 String 重载")
        void testStringNotEquals() {
            assertShow("参数错误", null, () -> ERROR.whenNotEquals("abc", "xyz"));
        }

        @Test
        @DisplayName("whenNotEquals(String, String) 两值相同不应抛异常")
        void testStringNotEqualsSame() {
            assertDoesNotThrow(() -> ERROR.whenNotEquals("abc", "abc"), "两值相同不应触发异常");
        }

        @Test
        @DisplayName("whenNotEquals(String, String, message) 应使用自定义消息")
        void testStringNotEqualsWithMessage() {
            assertShow("两次输入不一致", null, () -> ERROR.whenNotEquals("abc", "xyz", "两次输入不一致"));
        }
    }

    @Nested
    @DisplayName("whenNotEqualsIgnoreCase 忽略大小写不相等断言")
    class WhenNotEqualsIgnoreCaseTest {
        @Test
        @DisplayName("忽略大小写后不同应抛异常")
        void testIgnoreCaseNotEquals() {
            assertShow("参数错误", null, () -> ERROR.whenNotEqualsIgnoreCase("abc", "xyz"));
        }

        @Test
        @DisplayName("忽略大小写后相同不应抛异常")
        void testIgnoreCaseEquals() {
            assertDoesNotThrow(() -> ERROR.whenNotEqualsIgnoreCase("AbC", "aBc"), "忽略大小写后相同不应触发异常");
        }

        @Test
        @DisplayName("带自定义消息的重载应使用自定义消息")
        void testIgnoreCaseNotEqualsWithMessage() {
            assertShow("标识不能重复", null, () -> ERROR.whenNotEqualsIgnoreCase("AbC", "xyz", "标识不能重复"));
        }

        @Test
        @DisplayName("任一参数为 null 时直接抛出异常（源码先判空）")
        void testIgnoreCaseNotEqualsWithNull() {
            assertShow("参数错误", null, () -> ERROR.whenNotEqualsIgnoreCase(null, "abc"));
            assertShow("参数错误", null, () -> ERROR.whenNotEqualsIgnoreCase("abc", null));
            assertShow("自定义消息", null, () -> ERROR.whenNotEqualsIgnoreCase(null, null, "自定义消息"));
        }
    }

    @Nested
    @DisplayName("whenEmpty 空白断言")
    class WhenEmptyTest {
        @Test
        @DisplayName("whenEmpty(null) 应抛异常")
        void testWhenEmptyNull() {
            assertShow("参数错误", null, () -> ERROR.whenEmpty(null));
        }

        @Test
        @DisplayName("whenEmpty(空字符串与纯空白字符串) 应抛异常")
        void testWhenEmptyBlankString() {
            assertShow("参数错误", null, () -> ERROR.whenEmpty(""));
            assertShow("参数错误", null, () -> ERROR.whenEmpty("   "));
        }

        @Test
        @DisplayName("whenEmpty(有内容字符串) 不应抛异常")
        void testWhenEmptyHasText() {
            assertDoesNotThrow(() -> ERROR.whenEmpty("abc"), "有内容的字符串不应触发异常");
        }

        @Test
        @DisplayName("whenEmpty(非字符串对象) 按 toString 判断，不应抛异常")
        void testWhenEmptyOtherObject() {
            assertDoesNotThrow(() -> ERROR.whenEmpty(0), "数字 0 的 toString 非空白，不应触发异常");
            assertDoesNotThrow(() -> ERROR.whenEmpty(List.of()), "空集合的 toString 为 []，不应触发异常");
        }

        @Test
        @DisplayName("whenEmpty(null, message) 应使用自定义消息")
        void testWhenEmptyWithMessage() {
            assertShow("用户姓名不能为空", null, () -> ERROR.whenEmpty(null, "用户姓名不能为空"));
            assertShow("用户姓名不能为空", null, () -> ERROR.whenEmpty("", "用户姓名不能为空"));
        }

        @Test
        @DisplayName("whenEmpty(有内容, message) 不应抛异常")
        void testWhenEmptyWithMessageAndNotEmpty() {
            assertDoesNotThrow(() -> ERROR.whenEmpty("Hamm", "用户姓名不能为空"), "有内容时不应触发异常");
        }
    }

    @Nested
    @DisplayName("whenNotNull 非空断言")
    class WhenNotNullTest {
        @Test
        @DisplayName("whenNotNull(非空对象) 应抛异常")
        void testWhenNotNull() {
            assertShow("参数错误", null, () -> ERROR.whenNotNull("abc"));
        }

        @Test
        @DisplayName("whenNotNull(null) 不应抛异常")
        void testWhenNotNullNull() {
            assertDoesNotThrow(() -> ERROR.whenNotNull(null), "null 不应触发异常");
        }

        @Test
        @DisplayName("whenNotNull(非空对象, message) 应使用自定义消息")
        void testWhenNotNullWithMessage() {
            assertShow("该字段不允许填写", null, () -> ERROR.whenNotNull(0, "该字段不允许填写"));
        }

        @Test
        @DisplayName("whenNotNull(null, message) 不应抛异常")
        void testWhenNotNullWithMessageAndNull() {
            assertDoesNotThrow(() -> ERROR.whenNotNull(null, "该字段不允许填写"), "null 不应触发异常");
        }
    }

    @Nested
    @DisplayName("show(message) 传递 null 数据时的行为")
    class NullDataTest {
        @Test
        @DisplayName("show(message) 内部以 data 为 null 调用三参重载")
        void testShowWithNullData() {
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> ERROR.show("仅有消息"), "应抛出业务异常");
            assertNull(exception.getData(), "两参 show() 抛出的异常数据应为 null");
        }
    }
}
