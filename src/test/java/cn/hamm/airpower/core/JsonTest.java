package cn.hamm.airpower.core;

import cn.hamm.airpower.core.exception.ServiceException;
import cn.hamm.airpower.core.fixture.DemoError;
import cn.hamm.airpower.core.fixture.DemoModel;
import cn.hamm.airpower.core.fixture.TimeModel;
import cn.hamm.airpower.core.interfaces.IException;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Data;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>{@link Json} 的单元测试</h1>
 *
 * <p>覆盖：常量、{@code create}、{@code success}、{@code data}、{@code error} 的全部重载、
 * {@code show}、序列化 {@code toString}、反序列化 {@code parse}/{@code parseList}/
 * {@code parse2Map}/{@code parse2MapList}，以及 Lombok 生成的 {@code equals}/{@code hashCode}/{@code toString}。</p>
 *
 * @author Hamm.cn
 */
@DisplayName("Json 简单 JSON 对象")
class JsonTest {

    /**
     * 简单的 POJO，用于对象序列化与反序列化
     */
    @Data
    public static class UserModel {
        /**
         * 姓名
         */
        private String name;
        /**
         * 年龄
         */
        private Integer age;
        /**
         * 好友列表
         */
        private List<UserModel> friends;
        /**
         * 扩展属性，类型声明为 {@code Map}，用于验证全局 {@code NON_NULL} 规则
         */
        private Map<String, Object> attrs = new HashMap<>();
    }

    /**
     * 没有 Getter 的空 Bean，用于验证 {@code FAIL_ON_EMPTY_BEANS=false}
     */
    public static class EmptyBean {
    }

    /**
     * 自我引用的对象，用于验证序列化异常
     */
    public static class SelfRef {
        /**
         * 返回自身
         *
         * @return 自身
         */
        public SelfRef getSelf() {
            return this;
        }
    }

    @Nested
    @DisplayName("常量与构造器")
    class ConstantsTest {

        @Test
        @DisplayName("三个状态码常量取值正确")
        void constantsAreCorrect() {
            assertEquals(200, Json.SUCCESS_CODE, "成功代码常量应为 200");
            assertEquals(500, Json.SERVICE_ERROR, "错误代码常量应为 500");
            assertEquals(401, Json.UNAUTHORIZED_CODE, "未授权代码常量应为 401");
        }

        @Test
        @DisplayName("构造器为私有，禁止外部实例化")
        void constructorIsPrivate() throws NoSuchMethodException {
            Constructor<?> constructor = Json.class.getDeclaredConstructor();
            assertTrue(Modifier.isPrivate(constructor.getModifiers()), "Json 的构造器应该是 private，防止外部实例化");
        }
    }

    @Nested
    @DisplayName("create 与默认值")
    class CreateTest {

        @Test
        @DisplayName("create 返回新的实例，默认 code=200、message 为空串、data 为 null")
        void createReturnsDefaults() {
            Json json = Json.create();
            assertNotNull(json, "create 不应返回 null");
            assertEquals(200, json.getCode(), "默认响应码应为 200");
            assertEquals("", json.getMessage(), "默认提示信息应为空字符串");
            assertNull(json.getData(), "默认返回数据应为 null");
            assertNull(json.getTraceId(), "默认 TraceID 应为 null");
        }

        @Test
        @DisplayName("每次 create 返回的都是不同实例")
        void createReturnsDistinctInstances() {
            assertNotSame(Json.create(), Json.create(), "每次 create 都应返回新实例");
        }

        @Test
        @DisplayName("链式 Setter 返回自身且按顺序生效")
        void settersAreChainable() {
            Json json = Json.create().setCode(1).setMessage("m").setData("d").setTraceId("t");
            assertEquals(1, json.getCode(), "链式设置的响应码应为 1");
            assertEquals("m", json.getMessage(), "链式设置的提示信息应为 m");
            assertEquals("d", json.getData(), "链式设置的返回数据应为 d");
            assertEquals("t", json.getTraceId(), "链式设置的 TraceID 应为 t");
        }
    }

    @Nested
    @DisplayName("success 输出提示信息")
    class SuccessTest {

        @Test
        @DisplayName("success 默认响应码为 200，data 为 null")
        void successKeepsSuccessCode() {
            Json json = Json.success("操作成功");
            assertEquals(200, json.getCode(), "success 的响应码应为 200");
            assertEquals("操作成功", json.getMessage(), "success 的提示信息应等于入参");
            assertNull(json.getData(), "success 不应设置返回数据");
        }

        @Test
        @DisplayName("success 传入 null 提示信息时不报错")
        void successAcceptsNullMessage() {
            Json json = Json.success(null);
            assertNull(json.getMessage(), "success 传入 null 时提示信息应为 null");
            assertEquals(200, json.getCode(), "success 传入 null 时响应码仍为 200");
        }
    }

    @Nested
    @DisplayName("data 输出数据")
    class DataTest {

        @Test
        @DisplayName("data(data) 使用默认提示信息“获取成功”")
        void dataUsesDefaultMessage() {
            Json json = Json.data("payload");
            assertEquals(200, json.getCode(), "data 的响应码应为 200");
            assertEquals("获取成功", json.getMessage(), "单参 data 的提示信息应为「获取成功」");
            assertEquals("payload", json.getData(), "data 应原样保存入参");
        }

        @Test
        @DisplayName("data(data, message) 使用自定义提示信息")
        void dataWithMessage() {
            Json json = Json.data(123, "自定义");
            assertEquals(123, json.getData(), "data 应保存传入的数据");
            assertEquals("自定义", json.getMessage(), "双参 data 的提示信息应为自定义值");
        }

        @Test
        @DisplayName("data 传入 null 数据不抛异常")
        void dataAcceptsNull() {
            Json json = Json.data(null);
            assertNull(json.getData(), "data 传入 null 时返回数据应为 null");
            assertEquals("获取成功", json.getMessage(), "data 传入 null 时提示信息仍为「获取成功」");
        }

        @Test
        @DisplayName("data 支持集合与嵌套对象")
        void dataSupportsCollectionAndNested() {
            assertEquals(List.of(1, 2), Json.data(List.of(1, 2)).getData(), "data 应支持集合类型");
            Json inner = Json.data("inner");
            assertSame(inner, Json.data(inner).getData(), "data 应支持嵌套的 Json 对象");
        }
    }

    @Nested
    @DisplayName("error 输出错误")
    class ErrorTest {

        @Test
        @DisplayName("error(IException) 使用枚举的错误码与错误信息")
        void errorFromEnum() {
            Json json = Json.error(DemoError.UNAUTHORIZED);
            assertEquals(401, json.getCode(), "error(IException) 应使用枚举的错误码 401");
            assertEquals("未授权，请先登录", json.getMessage(), "error(IException) 应使用枚举的错误信息");
            assertNull(json.getData(), "error(IException) 不应设置返回数据");
        }

        @Test
        @DisplayName("error(IException, message) 覆盖错误信息但保留错误码")
        void errorFromEnumWithMessage() {
            Json json = Json.error(DemoError.PARAM_ERROR, "参数缺失");
            assertEquals(400, json.getCode(), "error(IException, message) 应保留枚举错误码 400");
            assertEquals("参数缺失", json.getMessage(), "error(IException, message) 应使用自定义错误信息");
            assertNull(json.getData(), "error(IException, message) 不应设置返回数据");
        }

        @Test
        @DisplayName("error(IException, message, data) 同时设置错误码、信息与数据")
        void errorFromEnumWithMessageAndData() {
            Json json = Json.error(DemoError.UNKNOWN, "未知", "detail");
            assertEquals(500, json.getCode(), "error(IException, message, data) 应使用枚举错误码 500");
            assertEquals("未知", json.getMessage(), "error(IException, message, data) 应使用自定义错误信息");
            assertEquals("detail", json.getData(), "error(IException, message, data) 应设置返回数据");
        }

        @Test
        @DisplayName("error(IException, null, data) 允许错误信息为 null")
        void errorFromEnumWithNullMessage() {
            Json json = Json.error(DemoError.PARAM_ERROR, null, "d");
            assertEquals(400, json.getCode(), "错误信息为 null 时错误码仍应取枚举值");
            assertNull(json.getMessage(), "错误信息为 null 时提示信息应为 null");
            assertEquals("d", json.getData(), "错误信息为 null 时返回数据仍应被保存");
        }

        @Test
        @DisplayName("error(IException) 传入 null 抛空指针（源码未做非空校验）")
        void errorFromNullEnumThrowsNpe() {
            IException<?> nullError = null;
            assertThrows(NullPointerException.class, () -> Json.error(nullError), "error(IException) 传入 null 时源码未做判空，应抛空指针");
        }

        @Test
        @DisplayName("error(String) 使用默认错误码 500")
        void errorWithMessageOnly() {
            Json json = Json.error("出错了");
            assertEquals(500, json.getCode(), "单参 error 的错误码应为 500");
            assertEquals("出错了", json.getMessage(), "单参 error 应使用传入的错误信息");
            assertNull(json.getData(), "单参 error 不应设置返回数据");
        }

        @Test
        @DisplayName("error(String, code) 使用自定义错误码")
        void errorWithMessageAndCode() {
            Json json = Json.error("未找到", 404);
            assertEquals(404, json.getCode(), "error(message, code) 应使用自定义错误码 404");
            assertEquals("未找到", json.getMessage(), "error(message, code) 应使用传入的错误信息");
            assertNull(json.getData(), "error(message, code) 不应设置返回数据");
        }

        @Test
        @DisplayName("error(String, code, data) 三个参数全部生效")
        void errorWithMessageCodeAndData() {
            Json json = Json.error("未找到", 404, "detail");
            assertEquals(404, json.getCode(), "error(message, code, data) 应使用自定义错误码");
            assertEquals("未找到", json.getMessage(), "error(message, code, data) 应使用传入的错误信息");
            assertEquals("detail", json.getData(), "error(message, code, data) 应保存传入的数据");
        }

        @Test
        @DisplayName("error 允许错误码边界值 0 与 Integer.MAX_VALUE")
        void errorAcceptsBoundaryCode() {
            assertEquals(0, Json.error("m", 0).getCode(), "错误码边界值 0 应被原样接受");
            assertEquals(Integer.MAX_VALUE, Json.error("m", Integer.MAX_VALUE).getCode(), "错误码边界值 Integer.MAX_VALUE 应被原样接受");
            assertEquals(Integer.MIN_VALUE, Json.error("m", Integer.MIN_VALUE).getCode(), "错误码边界值 Integer.MIN_VALUE 应被原样接受");
        }
    }

    @Nested
    @DisplayName("show 组装 JSON")
    class ShowTest {

        @Test
        @DisplayName("show 设置全部三个字段")
        void showSetsAllFields() {
            Json json = Json.show(1, "a", null);
            assertEquals(1, json.getCode(), "show 应设置响应码");
            assertEquals("a", json.getMessage(), "show 应设置提示信息");
            assertNull(json.getData(), "show 传入 null 时返回数据应为 null");
        }

        @Test
        @DisplayName("show 可直接嵌套为 data")
        void showCanBeNestedAsData() {
            Json json = Json.data(Json.show(9, "inner", "d"));
            assertEquals(9, ((Json) json.getData()).getCode(), "嵌套的 Json 应保留其响应码");
        }
    }

    @Nested
    @DisplayName("toString 序列化")
    class ToStringTest {

        @Test
        @DisplayName("默认 Json 序列化结果固定为 code/message，且 data 为 null 时不输出")
        void defaultJsonFieldOrder() {
            // 全局改为 NON_NULL 后，data 为 null 不再出现在 JSON 文本中
            assertEquals("{\"code\":200,\"message\":\"\"}",
                    Json.toString(Json.create()), "默认 Json 的 JSON 文本应为 code/message，data 为 null 时被过滤");
        }

        @Test
        @DisplayName("traceId 为 null 时不输出该字段")
        void traceIdIsOmittedWhenNull() {
            assertFalse(Json.toString(Json.create()).contains("traceId"), "traceId 为 null 时不应出现在 JSON 文本中");
            assertTrue(Json.toString(Json.create().setTraceId("tid")).contains("\"traceId\":\"tid\""),
                    "traceId 非 null 时应出现在 JSON 文本中");
        }

        @Test
        @DisplayName("序列化 POJO 时输出非空字段，null 字段被全局 NON_NULL 规则过滤")
        void serializePojo() {
            UserModel user = new UserModel();
            user.setName("hamm");
            user.setAge(18);
            String json = Json.toString(user);
            assertTrue(json.contains("\"name\":\"hamm\""), "POJO 序列化后应包含姓名字段");
            assertTrue(json.contains("\"age\":18"), "POJO 序列化后应包含年龄字段");
            assertFalse(json.contains("\"friends\""), "未赋值的引用类型字段应被 NON_NULL 规则过滤");
        }

        @Test
        @DisplayName("空 Map 属性照常输出，只有 null 会被过滤")
        void emptyMapPropertyIsOmitted() {
            // 修复前的 configOverride(Map.class).setInclude(NON_EMPTY) 对直接序列化的 Map
            // 完全不生效。现在统一为 ObjectMapper.setSerializationInclusion(NON_NULL)：
            // 只过滤 null，空集合/空 Map 照常输出（API 响应里 data:[] 需要保留）
            assertTrue(Json.toString(new UserModel()).contains("\"attrs\":{}"),
                    "空 Map 不是 null，应照常序列化：" + Json.toString(new UserModel()));
            UserModel user = new UserModel();
            user.getAttrs().put("k", "v");
            assertTrue(Json.toString(user).contains("\"attrs\":{\"k\":\"v\"}"),
                    "非空的 Map 属性应被正常序列化");
        }

        @Test
        @DisplayName("null 的 Map 属性会被全局 NON_NULL 规则过滤")
        void nullMapPropertyIsOmitted() {
            UserModel user = new UserModel();
            user.setAttrs(null);
            assertFalse(Json.toString(user).contains("\"attrs\""),
                    "值为 null 的属性应被过滤：" + Json.toString(user));
        }

        @Test
        @DisplayName("顶层 Map 内部的 null 值同样被 NON_NULL 规则过滤")
        void mapContentIsNotFiltered() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("a", null);
            map.put("b", "");
            map.put("c", 1);
            assertEquals("{\"b\":\"\",\"c\":1}", Json.toString(map),
                    "全局 NON_NULL 规则同样作用于 Map 内部的 null 值；空串不受影响");
        }

        @Test
        @DisplayName("空集合与空字符串原样输出")
        void emptyCollectionAndString() {
            assertEquals("{\"code\":200,\"message\":\"获取成功\",\"data\":[]}", Json.toString(Json.data(List.of())),
                    "空集合应序列化为 []");
            assertEquals("{\"code\":200,\"message\":\"获取成功\",\"data\":\"\"}", Json.toString(Json.data("")),
                    "空字符串应序列化为 \"\"");
        }

        @Test
        @DisplayName("序列化 null 返回字符串 null")
        void serializeNullReturnsNullLiteral() {
            assertEquals("null", Json.toString(null), "Jackson 对 null 序列化结果为字符串 null");
        }

        @Test
        @DisplayName("无 Getter 的空 Bean 序列化为空对象")
        void serializeEmptyBean() {
            assertEquals("{}", Json.toString(new EmptyBean()), "FAIL_ON_EMPTY_BEANS=false 时空 Bean 应序列化为 {}");
        }

        @Test
        @DisplayName("循环引用序列化抛出 ServiceException")
        void serializeSelfReferenceThrows() {
            ServiceException exception = assertThrows(ServiceException.class, () -> Json.toString(new SelfRef()),
                    "自我引用的对象序列化应抛出 ServiceException");
            assertTrue(exception.getMessage().startsWith("JSON 序列化失败，"),
                    "序列化失败异常信息应以「JSON 序列化失败，」开头");
        }
    }

    @Nested
    @DisplayName("parse 反序列化")
    class ParseTest {

        @Test
        @DisplayName("parse(json, Class) 反序列化为普通 POJO")
        void parsePojo() {
            UserModel user = Json.parse("{\"name\":\"hamm\",\"age\":18}", UserModel.class);
            assertEquals("hamm", user.getName(), "反序列化后的姓名应正确");
            assertEquals(18, user.getAge(), "反序列化后的年龄应正确");
        }

        @Test
        @DisplayName("parse 忽略未声明的属性")
        void parseIgnoresUnknownProperties() {
            UserModel user = Json.parse("{\"name\":\"hamm\",\"unknown\":\"x\"}", UserModel.class);
            assertEquals("hamm", user.getName(), "未声明的属性应被忽略而不是报错");
        }

        @Test
        @DisplayName("parse 支持嵌套对象与集合")
        void parseNested() {
            UserModel user = Json.parse("{\"name\":\"a\",\"friends\":[{\"name\":\"b\"}]}", UserModel.class);
            assertNotNull(user.getFriends(), "嵌套集合应被解析");
            assertEquals(1, user.getFriends().size(), "嵌套集合元素数量应为 1");
            assertEquals("b", user.getFriends().get(0).getName(), "嵌套集合元素内容应正确");
        }

        @Test
        @DisplayName("parse 可反序列化到 Json 自身")
        void parseIntoJsonItself() {
            Json json = Json.parse("{\"code\":401,\"message\":\"m\",\"data\":{\"a\":1},\"traceId\":\"t\"}", Json.class);
            assertEquals(401, json.getCode(), "反序列化到 Json 自身时响应码应正确");
            assertEquals("m", json.getMessage(), "反序列化到 Json 自身时提示信息应正确");
            assertInstanceOf(Map.class, json.getData(), "Json 的 data 字段应被解析为 Map");
            assertEquals("t", json.getTraceId(), "反序列化到 Json 自身时 TraceID 应正确");
        }

        @Test
        @DisplayName("parse 到 Json 时缺省字段回落到默认值")
        void parseIntoJsonWithDefaults() {
            Json json = Json.parse("{\"code\":1,\"unknown\":2}", Json.class);
            assertEquals(1, json.getCode(), "显式给出的响应码应生效");
            assertEquals("", json.getMessage(), "缺失的 message 应保持默认空串");
            assertNull(json.getData(), "缺失的 data 应保持 null");
        }

        @Test
        @DisplayName("parse 文本 null 返回 null 对象")
        void parseJsonNullLiteral() {
            assertNull(Json.parse("null", Json.class), "JSON 字面量 null 应反序列化为 Java null");
        }

        @Test
        @DisplayName("parse 支持基本类型")
        void parseScalar() {
            assertEquals("abc", Json.parse("\"abc\"", String.class), "字符串字面量应能反序列化");
            assertEquals(1, Json.parse("1", Integer.class), "数字字面量应能反序列化");
        }

        @Test
        @DisplayName("parse 非法 JSON 抛 ServiceException")
        void parseInvalidJsonThrows() {
            ServiceException exception = assertThrows(ServiceException.class, () -> Json.parse("{bad}", UserModel.class),
                    "非法 JSON 文本应抛出 ServiceException");
            assertTrue(exception.getMessage().startsWith("JSON 反序列化失败，"),
                    "反序列化失败异常信息应以「JSON 反序列化失败，」开头");
        }

        @Test
        @DisplayName("parse 空字符串抛 ServiceException")
        void parseEmptyStringThrows() {
            assertThrows(ServiceException.class, () -> Json.parse("", UserModel.class),
                    "空字符串不是合法 JSON，应抛出 ServiceException");
        }

        @Test
        @DisplayName("parse 类型不匹配抛 ServiceException")
        void parseTypeMismatchThrows() {
            assertThrows(ServiceException.class, () -> Json.parse("[1,2]", UserModel.class),
                    "数组反序列化成对象应抛出 ServiceException");
            assertThrows(ServiceException.class, () -> Json.parse("{\"code\":\"abc\"}", Json.class),
                    "字符串反序列化成 int 应抛出 ServiceException");
        }

        @Test
        @DisplayName("parse 传 null 应抛业务异常")
        void parseNullStringThrowsServiceException() {
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> Json.parse(null, UserModel.class),
                    "parse(null, Class) 的 null 文本应被统一包装为 ServiceException，不能泄漏 IllegalArgumentException");
            assertAll("异常应为业务异常，且信息以「JSON 反序列化失败，」开头",
                    () -> assertTrue(exception.getMessage().startsWith("JSON 反序列化失败，"),
                            "反序列化失败异常信息应以「JSON 反序列化失败，」开头，实际为：" + exception.getMessage()),
                    () -> assertEquals(Json.SERVICE_ERROR, exception.getCode(),
                            "业务异常的错误码应为 500，实际为：" + exception.getCode()));
        }

        @Test
        @DisplayName("parse(json, TypeReference) 支持泛型反序列化")
        void parseWithTypeReference() {
            Map<String, Object> map = Json.parse("{\"a\":1,\"b\":\"x\"}", new TypeReference<Map<String, Object>>() {
            });
            assertEquals(1, map.get("a"), "TypeReference 反序列化后的数字应正确");
            assertEquals("x", map.get("b"), "TypeReference 反序列化后的字符串应正确");

            List<UserModel> list = Json.parse("[{\"name\":\"a\"}]", new TypeReference<List<UserModel>>() {
            });
            assertEquals(1, list.size(), "TypeReference 反序列化的集合长度应为 1");
            assertEquals("a", list.get(0).getName(), "TypeReference 反序列化的集合元素应正确");
        }

        @Test
        @DisplayName("parse(json, TypeReference) 非法 JSON 抛 ServiceException")
        void parseWithTypeReferenceInvalidThrows() {
            assertThrows(ServiceException.class,
                    () -> Json.parse("{oops}", new TypeReference<Map<String, Object>>() {
                    }), "TypeReference 方式遇到非法 JSON 同样应抛出 ServiceException");
        }
    }

    @Nested
    @DisplayName("parseList 反序列化为数组")
    class ParseListTest {

        @Test
        @DisplayName("parseList 解析对象数组")
        void parseListOfPojo() {
            DemoModel[] models = Json.parseList("[{\"id\":1,\"name\":\"a\"},{\"id\":2,\"name\":\"b\"}]", DemoModel[].class);
            assertEquals(2, models.length, "数组长度应为 2");
            assertEquals("a", models[0].getName(), "数组第一个元素应正确");
            assertEquals(2L, models[1].getId(), "数组第二个元素应正确");
        }

        @Test
        @DisplayName("parseList 解析空数组")
        void parseEmptyList() {
            assertEquals(0, Json.parseList("[]", DemoModel[].class).length, "空数组应解析为长度为 0 的数组");
        }

        @Test
        @DisplayName("parseList 支持 null 元素")
        void parseListWithNullElement() {
            DemoModel[] models = Json.parseList("[null]", DemoModel[].class);
            assertEquals(1, models.length, "含 null 元素的数组长度应为 1");
            assertNull(models[0], "数组中的 null 元素应保留为 null");
        }

        @Test
        @DisplayName("parseList 类型不匹配抛 ServiceException")
        void parseListTypeMismatchThrows() {
            ServiceException exception = assertThrows(ServiceException.class, () -> Json.parseList("{}", DemoModel[].class),
                    "对象文本无法解析为数组，应抛出 ServiceException");
            assertTrue(exception.getMessage().startsWith("JSON 反序列化失败，"),
                    "数组反序列化失败异常信息应以「JSON 反序列化失败，」开头");
        }

        @Test
        @DisplayName("parseList 传 null 应抛业务异常")
        void parseListNullStringThrowsServiceException() {
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> Json.parseList(null, DemoModel[].class),
                    "parseList(null, ...) 的 null 文本应被统一包装为 ServiceException，不能泄漏 IllegalArgumentException");
            assertAll("异常应为业务异常，且信息以「JSON 反序列化失败，」开头",
                    () -> assertTrue(exception.getMessage().startsWith("JSON 反序列化失败，"),
                            "反序列化失败异常信息应以「JSON 反序列化失败，」开头，实际为：" + exception.getMessage()),
                    () -> assertEquals(Json.SERVICE_ERROR, exception.getCode(),
                            "业务异常的错误码应为 500，实际为：" + exception.getCode()));
        }
    }

    @Nested
    @DisplayName("parse2Map 反序列化为 Map")
    class Parse2MapTest {

        @Test
        @DisplayName("parse2Map 解析扁平结构")
        void parseFlatMap() {
            Map<String, Object> map = Json.parse2Map("{\"a\":1,\"b\":null}");
            assertEquals(1, map.get("a"), "Map 中的数字应正确");
            assertTrue(map.containsKey("b"), "Map 中显式的 null 值应保留键");
            assertNull(map.get("b"), "Map 中显式的 null 值应保留为 null");
        }

        @Test
        @DisplayName("parse2Map 解析嵌套结构")
        void parseNestedMap() {
            Map<String, Object> map = Json.parse2Map("{\"a\":{\"b\":[1,2]},\"c\":true}");
            assertInstanceOf(Map.class, map.get("a"), "嵌套对象应被解析为 Map");
            assertEquals(List.of(1, 2), ((Map<?, ?>) map.get("a")).get("b"),
                    "嵌套对象中的数组应被正确解析");
            assertEquals(Boolean.TRUE, map.get("c"), "布尔值应被正确解析");
        }

        @Test
        @DisplayName("parse2Map 解析空对象")
        void parseEmptyMap() {
            assertTrue(Json.parse2Map("{}").isEmpty(), "空对象应解析为空的 Map");
        }

        @Test
        @DisplayName("parse2Map 非法 JSON 抛 ServiceException")
        void parse2MapInvalidThrows() {
            ServiceException exception = assertThrows(ServiceException.class, () -> Json.parse2Map("[1]"),
                    "数组文本无法解析为 Map，应抛出 ServiceException");
            assertTrue(exception.getMessage().startsWith("JSON 反序列化失败，"),
                    "Map 反序列化失败异常信息应以「JSON 反序列化失败，」开头");
        }

        @Test
        @DisplayName("parse2Map 空字符串与 null 均抛 ServiceException")
        void parse2MapBlankThrows() {
            assertThrows(ServiceException.class, () -> Json.parse2Map(""),
                    "空字符串应被统一包装成 ServiceException");
            assertThrows(ServiceException.class, () -> Json.parse2Map(null),
                    "null 文本应被统一包装成 ServiceException（该方法捕获的是 Exception）");
        }
    }

    @Nested
    @DisplayName("parse2MapList 反序列化为 List<Map>")
    class Parse2MapListTest {

        @Test
        @DisplayName("parse2MapList 解析对象数组")
        void parseListOfMap() {
            List<Map<String, Object>> list = Json.parse2MapList("[{\"a\":1},{\"b\":2}]");
            assertEquals(2, list.size(), "列表长度应为 2");
            assertEquals(1, list.get(0).get("a"), "第一个 Map 的内容应正确");
            assertEquals(2, list.get(1).get("b"), "第二个 Map 的内容应正确");
        }

        @Test
        @DisplayName("parse2MapList 解析空数组")
        void parseEmptyList() {
            assertTrue(Json.parse2MapList("[]").isEmpty(), "空数组应解析为空的 List");
        }

        @Test
        @DisplayName("parse2MapList 元素类型不匹配抛 ServiceException")
        void parseListTypeMismatchThrows() {
            assertThrows(ServiceException.class, () -> Json.parse2MapList("[1,2]"),
                    "元素是数字的数组无法解析为 List<Map>，应抛出 ServiceException");
            assertThrows(ServiceException.class, () -> Json.parse2MapList("{}"),
                    "对象文本无法解析为 List<Map>，应抛出 ServiceException");
        }

        @Test
        @DisplayName("parse2MapList 传入 null 抛 ServiceException")
        void parseListNullStringThrows() {
            ServiceException exception = assertThrows(ServiceException.class, () -> Json.parse2MapList(null),
                    "null 文本应被统一包装成 ServiceException");
            assertTrue(exception.getMessage().startsWith("JSON 反序列化失败，"),
                    "List 反序列化失败异常信息应以「JSON 反序列化失败，」开头");
        }
    }

    @Nested
    @DisplayName("Lombok 生成的实例方法")
    class LombokMethods {

        @Test
        @DisplayName("内容相同的两个 Json 判定相等且哈希值一致")
        void equalsAndHashCode() {
            Json left = Json.create().setCode(1).setMessage("m");
            Json right = Json.create().setCode(1).setMessage("m");
            assertEquals(left, right, "内容相同的两个 Json 应相等");
            assertEquals(left.hashCode(), right.hashCode(), "内容相同的两个 Json 哈希值应一致");
            assertNotSame(left, right, "两次构造应得到不同实例");
        }

        @Test
        @DisplayName("响应码不同的两个 Json 判定不相等")
        void notEquals() {
            assertFalse(Json.create().setCode(1).equals(Json.create().setCode(2)), "响应码不同的两个 Json 不应相等");
        }

        @Test
        @DisplayName("toString 输出 Lombok 风格的调试文本")
        void lombokToString() {
            assertEquals("Json(code=1, message=, data=null, traceId=null)", Json.create().setCode(1).toString(),
                    "Lombok 生成的 toString 应输出全部四个字段");
        }
    }

    @Nested
    @DisplayName("JSR-310 时间类型")
    class JavaTimeTest {

        @Test
        @DisplayName("LocalDateTime 输出 ISO 字符串而非 bean 结构")
        void localDateTimeIsIsoString() {
            TimeModel model = new TimeModel().setDateTime(LocalDateTime.of(2026, 10, 2, 13, 45, 30));
            String json = Json.toString(model);
            assertTrue(json.contains("\"2026-10-02T13:45:30\""),
                    "LocalDateTime 应序列化为 ISO 字符串，实际为：" + json);
            assertFalse(json.contains("monthValue"), "不应出现 bean 序列化产物 monthValue：" + json);
            assertFalse(json.contains("\"year\""), "不应出现 bean 序列化产物 year：" + json);
        }

        @Test
        @DisplayName("LocalDate 输出 ISO 字符串")
        void localDateIsIsoString() {
            String json = Json.toString(new TimeModel().setDate(LocalDate.of(2026, 10, 2)));
            assertTrue(json.contains("\"2026-10-02\""), "LocalDate 应序列化为 ISO 字符串：" + json);
        }

        @Test
        @DisplayName("Instant 输出 ISO 字符串而非时间戳数字")
        void instantIsIsoString() {
            Instant instant = Instant.ofEpochSecond(1_700_000_000L);
            String json = Json.toString(new TimeModel().setInstant(instant));
            assertTrue(json.contains("T"), "Instant 应为 ISO 字符串：" + json);
            assertFalse(json.matches(".*\\\"instant\\\":\\d+.*"),
                    "Instant 不应输出为时间戳数字：" + json);
        }

        @Test
        @DisplayName("含时间字段的实体可往返（Redis 缓存读回的关键）")
        void roundTrip() {
            TimeModel origin = new TimeModel()
                    .setDateTime(LocalDateTime.of(2026, 10, 2, 13, 45, 30))
                    .setDate(LocalDate.of(2026, 10, 2))
                    .setInstant(Instant.ofEpochSecond(1_700_000_000L));
            TimeModel back = Json.parse(Json.toString(origin), TimeModel.class);
            assertNotNull(back, "反序列化不应返回 null");
            assertEquals(origin.getDateTime(), back.getDateTime(), "LocalDateTime 往返应一致");
            assertEquals(origin.getDate(), back.getDate(), "LocalDate 往返应一致");
            assertEquals(origin.getInstant(), back.getInstant(), "Instant 往返应一致");
        }

        @Test
        @DisplayName("Collection 里的时间字段同样正确")
        void insideCollection() {
            List<TimeModel> list = List.of(new TimeModel().setDate(LocalDate.of(2026, 10, 2)));
            String json = Json.toString(list);
            assertTrue(json.contains("2026-10-02"), "集合内的 LocalDate 也应为 ISO 字符串：" + json);
        }
    }
}
