package cn.hamm.airpower.core.exception;

import cn.hamm.airpower.core.Json;
import cn.hamm.airpower.core.fixture.DemoError;
import cn.hamm.airpower.core.interfaces.IException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.crypto.BadPaddingException;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>ServiceException 单元测试</h1>
 *
 * <p>覆盖全部构造器、{@code getCode} / {@code getData} / {@code getMessage} 读取，
 * 以及 {@code IException} 默认方法 {@code get} / {@code show} 的实现行为。</p>
 *
 * @author Hamm.cn
 */
@DisplayName("ServiceException 单元测试")
class ServiceExceptionTest {
    @Nested
    @DisplayName("构造器")
    class ConstructorTest {
        @Test
        @DisplayName("无参构造器应使用默认错误码 500 且数据为 null")
        void testNoArgsConstructor() {
            ServiceException exception = new ServiceException();
            assertEquals(Json.SERVICE_ERROR, exception.getCode(), "无参构造的错误码应为 500");
            assertNull(exception.getData(), "无参构造的错误数据应为 null");
            assertNull(exception.getMessage(), "无参构造的错误信息应为 null");
        }

        @Test
        @DisplayName("单参构造器应设置消息，错误码为 500、数据为 null")
        void testMessageConstructor() {
            ServiceException exception = new ServiceException("操作失败");
            assertEquals("操作失败", exception.getMessage(), "错误信息应与传入一致");
            assertEquals(Json.SERVICE_ERROR, exception.getCode(), "错误码应为默认的服务错误码 500");
            assertNull(exception.getData(), "错误数据应为 null");
        }

        @Test
        @DisplayName("消息为 null 的单参构造器不应抛异常")
        void testMessageConstructorWithNull() {
            ServiceException exception = new ServiceException((String) null);
            assertNull(exception.getMessage(), "传入 null 消息时错误信息应为 null");
            assertEquals(Json.SERVICE_ERROR, exception.getCode(), "错误码应为 500");
        }

        @Test
        @DisplayName("消息 + 数据构造器应同时设置消息与数据")
        void testMessageAndDataConstructor() {
            List<String> data = List.of("a", "b");
            ServiceException exception = new ServiceException("参数缺失", data);
            assertEquals("参数缺失", exception.getMessage(), "错误信息应与传入一致");
            assertEquals(data, exception.getData(), "错误数据应与传入一致");
            assertEquals(Json.SERVICE_ERROR, exception.getCode(), "错误码应为 500");
        }

        @Test
        @DisplayName("数据为 null 的消息 + 数据构造器应保持数据为 null")
        void testMessageAndDataConstructorWithNullData() {
            ServiceException exception = new ServiceException("参数缺失", null);
            assertEquals("参数缺失", exception.getMessage(), "错误信息应与传入一致");
            assertNull(exception.getData(), "错误数据应为 null");
        }

        @Test
        @DisplayName("错误码 + 消息构造器应同时设置错误码与消息")
        void testCodeAndMessageConstructor() {
            ServiceException exception = new ServiceException(400, "参数错误");
            assertEquals(400, exception.getCode(), "错误码应与传入一致");
            assertEquals("参数错误", exception.getMessage(), "错误信息应与传入一致");
            assertNull(exception.getData(), "错误数据应为 null");
        }

        @Test
        @DisplayName("错误码 + 消息 + 数据构造器应同时设置三个字段")
        void testCodeMessageAndDataConstructor() {
            ServiceException exception = new ServiceException(401, "未授权", "token");
            assertEquals(401, exception.getCode(), "错误码应与传入一致");
            assertEquals("未授权", exception.getMessage(), "错误信息应与传入一致");
            assertEquals("token", exception.getData(), "错误数据应与传入一致");
        }

        @Test
        @DisplayName("异常 + 消息构造器应继承 IException 的错误码")
        void testExceptionAndMessageConstructor() {
            ServiceException exception = new ServiceException(DemoError.PARAM_ERROR, "自定义消息");
            assertEquals(400, exception.getCode(), "错误码应取自 IException 的 400");
            assertEquals("自定义消息", exception.getMessage(), "错误信息应使用传入的自定义消息");
            assertNull(exception.getData(), "错误数据应为 null");
        }

        @Test
        @DisplayName("仅异常构造器应继承 IException 的错误码与信息")
        void testExceptionOnlyConstructor() {
            ServiceException exception = new ServiceException(DemoError.UNAUTHORIZED);
            assertEquals(401, exception.getCode(), "错误码应取自 IException 的 401");
            assertEquals("未授权，请先登录", exception.getMessage(), "错误信息应取自 IException 的 message");
        }
    }

    @Nested
    @DisplayName("异常类型与继承关系")
    class HierarchyTest {
        @Test
        @DisplayName("ServiceException 应是 RuntimeException 的子类")
        void testIsRuntimeException() {
            ServiceException exception = new ServiceException("运行期异常");
            assertInstanceOf(RuntimeException.class, exception, "ServiceException 应继承 RuntimeException");
        }

        @Test
        @DisplayName("ServiceException 应实现 IException 接口")
        void testImplementsIException() {
            IException<ServiceException> exception = new ServiceException("实现接口");
            assertEquals(500, exception.getCode(), "通过接口读取的错误码应为 500");
            assertEquals("实现接口", exception.getMessage(), "通过接口读取的消息应与构造器一致");
        }

        @Test
        @DisplayName("get() 应返回异常自身")
        void testGet() {
            ServiceException exception = new ServiceException("自身");
            assertSame(exception, exception.get(), "get() 应返回异常自身");
        }

        @Test
        @DisplayName("ServiceException 可作为 Supplier 使用")
        void testAsSupplier() {
            Supplier<ServiceException> supplier = new ServiceException("供给者");
            assertEquals("供给者", supplier.get().getMessage(), "作为 Supplier 时 get() 应返回异常自身");
        }

        @Test
        @DisplayName("toString 应包含类名与错误信息")
        void testToString() {
            ServiceException exception = new ServiceException(500, "系统繁忙");
            String text = exception.toString();
            assertTrue(text.contains("ServiceException"), "toString 应包含类名");
            assertTrue(text.contains("系统繁忙"), "toString 应包含错误信息");
        }

        @Test
        @DisplayName("getLocalizedMessage 应返回错误信息")
        void testGetLocalizedMessage() {
            ServiceException exception = new ServiceException("中文消息");
            assertEquals("中文消息", exception.getLocalizedMessage(), "getLocalizedMessage 应返回错误信息");
        }

        @Test
        @DisplayName("未设置原因时 getCause 应为 null")
        void testGetCause() {
            assertNull(new ServiceException("无原因").getCause(), "默认构造的异常不应携带原因");
        }

        @Test
        @DisplayName("异常应可被 assertThrows 捕获并携带完整信息")
        void testCaughtByAssertThrows() {
            ServiceException exception = assertThrows(ServiceException.class, () -> {
                throw new ServiceException(400, "参数错误", "字段 userId");
            }, "应抛出业务异常");
            assertEquals(400, exception.getCode(), "被捕获异常的错误码应为 400");
            assertEquals("参数错误", exception.getMessage(), "被捕获异常的错误信息应为参数错误");
            assertEquals("字段 userId", exception.getData(), "被捕获异常的错误数据应为字段 userId");
        }
    }

    @Nested
    @DisplayName("show 系列方法（抛出新的业务异常）")
    class ShowTest {
        @Test
        @DisplayName("show() 应抛出 code 与 message 均为自身的业务异常")
        void testShow() {
            ServiceException origin = new ServiceException(DemoError.UNKNOWN);
            ServiceException thrown = assertThrows(ServiceException.class, origin::show, "show() 应抛出业务异常");
            assertNotSame(origin, thrown, "show() 应抛出新的异常实例");
            assertEquals(500, thrown.getCode(), "抛出的异常码应取自自身");
            assertEquals("未知错误", thrown.getMessage(), "抛出的异常信息应取自自身");
            assertNull(thrown.getData(), "抛出的异常数据应为 null");
        }

        @Test
        @DisplayName("show(message) 应使用自定义消息并保留错误码")
        void testShowWithMessage() {
            ServiceException origin = new ServiceException(DemoError.PARAM_ERROR);
            ServiceException thrown = assertThrows(ServiceException.class,
                    () -> origin.show("字段不能为空"), "show(message) 应抛出业务异常");
            assertEquals(400, thrown.getCode(), "抛出的异常码应取自自身");
            assertEquals("字段不能为空", thrown.getMessage(), "抛出的异常应使用自定义消息");
            assertNull(thrown.getData(), "抛出的异常数据应为 null");
        }

        @Test
        @DisplayName("show(message, data) 应同时携带自定义消息与数据")
        void testShowWithMessageAndData() {
            ServiceException origin = new ServiceException(DemoError.UNAUTHORIZED);
            ServiceException thrown = assertThrows(ServiceException.class,
                    () -> origin.show("登录已过期", 3600), "show(message, data) 应抛出业务异常");
            assertEquals(401, thrown.getCode(), "抛出的异常码应取自自身");
            assertEquals("登录已过期", thrown.getMessage(), "抛出的异常应使用自定义消息");
            assertEquals(3600, thrown.getData(), "抛出的异常应携带自定义数据");
        }

        @Test
        @DisplayName("show 在异常枚举上调用时应使用枚举的错误码与信息")
        void testShowOnEnum() {
            ServiceException thrown = assertThrows(ServiceException.class, DemoError.PARAM_ERROR::show,
                    "枚举调用 show() 应抛出业务异常");
            assertEquals(400, thrown.getCode(), "异常码应取自枚举");
            assertEquals("参数错误", thrown.getMessage(), "异常信息应取自枚举");
        }
    }

    @Nested
    @DisplayName("原始异常（cause）构造器")
    class CauseTest {

        @Test
        @DisplayName("消息 + Throwable 构造器应把异常放进 cause 而不是 data")
        void causeIsNotData() {
            IllegalStateException root = new IllegalStateException("root cause");

            ServiceException exception = new ServiceException("加密失败", root);

            assertEquals("加密失败", exception.getMessage(), "消息应原样保留");
            assertSame(root, exception.getCause(),
                    "原始异常必须落在 cause 上。没有 (String, Throwable) 重载时，"
                            + "new ServiceException(msg, throwable) 会静默匹配到 (String, Object)，"
                            + "异常被当成 data，getCause() 返回 null、堆栈彻底丢失");
            assertNull(exception.getData(),
                    "data 必须为 null：ExceptionInterceptor 会把 getData() 直接放进响应体，"
                            + "异常一旦落在 data 里，其类型、message 与 stackTrace 都会泄露给前端");
        }

        @Test
        @DisplayName("cause 为 null 时不应退化成 data 重载")
        void nullCauseIsNotData() {
            ServiceException exception = new ServiceException("加密失败", (Throwable) null);

            assertNull(exception.getCause(), "cause 为 null 时 getCause() 应为 null");
            assertNull(exception.getData(), "cause 为 null 时 data 也必须是 null");
        }

        @Test
        @DisplayName("两个重载在传 Throwable 时必须走不同分支")
        void overloadsDoNotCollide() {
            Object businessData = List.of("a", "b");
            Throwable cause = new IllegalStateException("boom");

            ServiceException withData = new ServiceException("消息", businessData);
            ServiceException withCause = new ServiceException("消息", cause);

            assertSame(businessData, withData.getData(), "显式传业务数据时必须落在 data 上");
            assertNull(withData.getCause(), "业务数据不应变成 cause");
            assertSame(cause, withCause.getCause(), "显式传 Throwable 时必须落在 cause 上");
            assertNull(withCause.getData(), "cause 不应变成 data");
        }
    }

    @Nested
    @DisplayName("铁律：data 里绝不出现异常或堆栈")
    class DataNeverHoldsThrowableTest {

        @Test
        @DisplayName("硬把异常塞进 data 时必须被拦下并改挂到 cause")
        void throwablePassedAsDataIsRejected() {
            BadPaddingException root = new BadPaddingException("Given final block not properly padded");

            // 显式转成 Object，模拟「调用方没看 javadoc」的违规写法
            ServiceException exception = new ServiceException("RSA 加密失败", (Object) root);

            assertNull(exception.getData(),
                    "data 会被 Jackson 序列化进响应体，异常一旦落在里面，"
                            + "其类型、message 与完整 stackTrace（类名/文件名/行号）都会泄露给前端");
            assertSame(root, exception.getCause(),
                    "堆栈不能丢：应改挂到 cause 上，排障时仍能看到");
        }

        @Test
        @DisplayName("三参构造器同样受保护")
        void threeArgConstructorAlsoProtected() {
            ServiceException exception = new ServiceException(500, "加密失败", (Object) new IllegalStateException("boom"));

            assertEquals(500, exception.getCode(), "错误码不应受影响");
            assertNull(exception.getData(), "data 同样不能是异常");
            assertNotNull(exception.getCause(), "cause 应保留原始异常");
        }

        @Test
        @DisplayName("响应体里不含 stackTrace 与异常类型名")
        void responseBodyHasNoStackTrace() {
            ServiceException exception = new ServiceException(
                    "RSA 加密失败", (Object) new BadPaddingException("Given final block not properly padded"));

            // 复刻 ExceptionInterceptor:240-243 的响应构造
            String body = Json.toString(
                    Json.create().setCode(exception.getCode())
                            .setMessage(exception.getMessage())
                            .setData(exception.getData()));

            assertFalse(body.contains("stackTrace"), "响应体不得包含 stackTrace：" + body);
            assertFalse(body.contains("BadPadding"), "响应体不得包含异常类型名：" + body);
        }

        @Test
        @DisplayName("正常业务数据不受影响")
        void businessDataStillWorks() {
            List<String> data = List.of("a", "b");

            assertEquals(data, new ServiceException("参数缺失", data).getData(),
                    "Map/List/String 这类业务数据必须照常放进 data");
            assertEquals(data, new ServiceException(400, "参数缺失", data).getData(),
                    "三参重载同样如此");
        }
    }
}
