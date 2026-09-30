package cn.hamm.airpower.core.exception;

import cn.hamm.airpower.core.Json;
import cn.hamm.airpower.core.fixture.DemoError;
import cn.hamm.airpower.core.interfaces.IException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
}
