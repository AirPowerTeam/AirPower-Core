package cn.hamm.airpower.core;

import cn.hamm.airpower.core.exception.ServiceException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>Json 单元测试</h1>
 *
 * @author Hamm.cn
 */
class JsonTest {

    private static final String TEST_MESSAGE = "测试消息";
    private static final String TEST_DATA = "测试数据";
    private static final int CUSTOM_CODE = 400;

    // ==================== 常量测试 ====================

    @Test
    void testConstants() {
        assertEquals(200, Json.SUCCESS_CODE);
        assertEquals(500, Json.SERVICE_ERROR);
        assertEquals(401, Json.UNAUTHORIZED_CODE);
    }

    // ==================== create 方法测试 ====================

    @Test
    void testCreate() {
        Json json = Json.create();
        assertNotNull(json);
        assertEquals(Json.SUCCESS_CODE, json.getCode());
        assertEquals("", json.getMessage());
        assertNull(json.getData());
        assertNull(json.getTraceId());
    }

    // ==================== success 方法测试 ====================

    @Test
    void testSuccessWithMessage() {
        Json json = Json.success(TEST_MESSAGE);
        assertNotNull(json);
        assertEquals(Json.SUCCESS_CODE, json.getCode());
        assertEquals(TEST_MESSAGE, json.getMessage());
        assertNull(json.getData());
    }

    @Test
    void testSuccessWithEmptyMessage() {
        Json json = Json.success("");
        assertNotNull(json);
        assertEquals(Json.SUCCESS_CODE, json.getCode());
        assertEquals("", json.getMessage());
    }

    // ==================== data 方法测试 ====================

    @Test
    void testDataWithObject() {
        Json json = Json.data(TEST_DATA);
        assertNotNull(json);
        assertEquals(Json.SUCCESS_CODE, json.getCode());
        assertEquals("获取成功", json.getMessage());
        assertEquals(TEST_DATA, json.getData());
    }

    @Test
    void testDataWithObjectAndMessage() {
        Json json = Json.data(TEST_DATA, TEST_MESSAGE);
        assertNotNull(json);
        assertEquals(Json.SUCCESS_CODE, json.getCode());
        assertEquals(TEST_MESSAGE, json.getMessage());
        assertEquals(TEST_DATA, json.getData());
    }

    @Test
    void testDataWithNull() {
        Json json = Json.data(null);
        assertNotNull(json);
        assertEquals(Json.SUCCESS_CODE, json.getCode());
        assertNull(json.getData());
    }

    // ==================== error 方法测试 ====================

    @Test
    void testErrorWithIException() {
        // 使用 ServiceException 作为 IException 实现
        ServiceException exception = new ServiceException(TEST_MESSAGE);
        Json json = Json.error(exception);
        assertNotNull(json);
        assertEquals(exception.getCode(), json.getCode());
        assertEquals(TEST_MESSAGE, json.getMessage());
    }

    @Test
    void testErrorWithIExceptionAndMessage() {
        ServiceException exception = new ServiceException("原始错误");
        Json json = Json.error(exception, TEST_MESSAGE);
        assertNotNull(json);
        assertEquals(exception.getCode(), json.getCode());
        assertEquals(TEST_MESSAGE, json.getMessage());
    }

    @Test
    void testErrorWithIExceptionMessageAndData() {
        ServiceException exception = new ServiceException("原始错误");
        Json json = Json.error(exception, TEST_MESSAGE, TEST_DATA);
        assertNotNull(json);
        assertEquals(exception.getCode(), json.getCode());
        assertEquals(TEST_MESSAGE, json.getMessage());
        assertEquals(TEST_DATA, json.getData());
    }

    @Test
    void testErrorWithMessage() {
        Json json = Json.error(TEST_MESSAGE);
        assertNotNull(json);
        assertEquals(Json.SERVICE_ERROR, json.getCode());
        assertEquals(TEST_MESSAGE, json.getMessage());
    }

    @Test
    void testErrorWithMessageAndCode() {
        Json json = Json.error(TEST_MESSAGE, CUSTOM_CODE);
        assertNotNull(json);
        assertEquals(CUSTOM_CODE, json.getCode());
        assertEquals(TEST_MESSAGE, json.getMessage());
    }

    @Test
    void testErrorWithMessageCodeAndData() {
        Json json = Json.error(TEST_MESSAGE, CUSTOM_CODE, TEST_DATA);
        assertNotNull(json);
        assertEquals(CUSTOM_CODE, json.getCode());
        assertEquals(TEST_MESSAGE, json.getMessage());
        assertEquals(TEST_DATA, json.getData());
    }

    // ==================== show 方法测试 ====================

    @Test
    void testShow() {
        Json json = Json.show(CUSTOM_CODE, TEST_MESSAGE, TEST_DATA);
        assertNotNull(json);
        assertEquals(CUSTOM_CODE, json.getCode());
        assertEquals(TEST_MESSAGE, json.getMessage());
        assertEquals(TEST_DATA, json.getData());
    }

    @Test
    void testShowWithNullData() {
        Json json = Json.show(CUSTOM_CODE, TEST_MESSAGE, null);
        assertNotNull(json);
        assertEquals(CUSTOM_CODE, json.getCode());
        assertEquals(TEST_MESSAGE, json.getMessage());
        assertNull(json.getData());
    }

    // ==================== parse 方法测试 ====================

    @Test
    void testParseWithClass() {
        String jsonStr = "{\"name\":\"test\",\"age\":18}";
        TestObject result = Json.parse(jsonStr, TestObject.class);
        assertNotNull(result);
        assertEquals("test", result.getName());
        assertEquals(18, result.getAge());
    }

    @Test
    void testParseWithTypeReference() {
        String jsonStr = "{\"name\":\"test\",\"age\":18}";
        TestObject result = Json.parse(jsonStr, new TypeReference<TestObject>() {});
        assertNotNull(result);
        assertEquals("test", result.getName());
        assertEquals(18, result.getAge());
    }

    @Test
    void testParseWithInvalidJson() {
        String invalidJson = "invalid json";
        assertThrows(ServiceException.class, () ->
            Json.parse(invalidJson, TestObject.class)
        );
    }

    @Test
    void testParseWithEmptyString() {
        assertThrows(ServiceException.class, () ->
            Json.parse("", TestObject.class)
        );
    }

    @Test
    void testParseWithNull() {
        // Jackson 在传入 null 时会抛出 IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () ->
            Json.parse(null, TestObject.class)
        );
    }

    // ==================== parseList 方法测试 ====================

    @Test
    void testParseList() {
        String jsonStr = "[{\"name\":\"test1\",\"age\":18},{\"name\":\"test2\",\"age\":20}]";
        TestObject[] result = Json.parseList(jsonStr, TestObject[].class);
        assertNotNull(result);
        assertEquals(2, result.length);
        assertEquals("test1", result[0].getName());
        assertEquals(18, result[0].getAge());
        assertEquals("test2", result[1].getName());
        assertEquals(20, result[1].getAge());
    }

    @Test
    void testParseListWithEmptyArray() {
        String jsonStr = "[]";
        TestObject[] result = Json.parseList(jsonStr, TestObject[].class);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    void testParseListWithInvalidJson() {
        assertThrows(ServiceException.class, () ->
            Json.parseList("invalid", TestObject[].class)
        );
    }

    // ==================== parse2Map 方法测试 ====================

    @Test
    void testParse2Map() {
        String jsonStr = "{\"key1\":\"value1\",\"key2\":123}";
        Map<String, Object> result = Json.parse2Map(jsonStr);
        assertNotNull(result);
        assertEquals("value1", result.get("key1"));
        assertEquals(123, result.get("key2"));
    }

    @Test
    void testParse2MapWithNestedObject() {
        String jsonStr = "{\"name\":\"test\",\"data\":{\"age\":18}}";
        Map<String, Object> result = Json.parse2Map(jsonStr);
        assertNotNull(result);
        assertEquals("test", result.get("name"));
        assertNotNull(result.get("data"));
    }

    @Test
    void testParse2MapWithInvalidJson() {
        assertThrows(ServiceException.class, () ->
            Json.parse2Map("invalid")
        );
    }

    @Test
    void testParse2MapWithEmptyString() {
        assertThrows(ServiceException.class, () ->
            Json.parse2Map("")
        );
    }

    // ==================== parse2MapList 方法测试 ====================

    @Test
    void testParse2MapList() {
        String jsonStr = "[{\"key1\":\"value1\"},{\"key2\":\"value2\"}]";
        List<Map<String, Object>> result = Json.parse2MapList(jsonStr);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("value1", result.get(0).get("key1"));
        assertEquals("value2", result.get(1).get("key2"));
    }

    @Test
    void testParse2MapListWithEmptyArray() {
        String jsonStr = "[]";
        List<Map<String, Object>> result = Json.parse2MapList(jsonStr);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testParse2MapListWithInvalidJson() {
        assertThrows(ServiceException.class, () ->
            Json.parse2MapList("invalid")
        );
    }

    // ==================== toString 方法测试 ====================

    @Test
    void testToStringWithObject() {
        TestObject obj = new TestObject();
        obj.setName("test");
        obj.setAge(18);
        String result = Json.toString(obj);
        assertNotNull(result);
        assertTrue(result.contains("test"));
        assertTrue(result.contains("18"));
    }

    @Test
    void testToStringWithNull() {
        // null 对象序列化应该返回 "null"
        String result = Json.toString(null);
        assertEquals("null", result);
    }

    @Test
    void testToStringWithMap() {
        Map<String, Object> map = Map.of("key", "value", "num", 123);
        String result = Json.toString(map);
        assertNotNull(result);
        assertTrue(result.contains("key"));
        assertTrue(result.contains("value"));
    }

    // ==================== 链式调用测试 ====================

    @Test
    void testFluentApi() {
        Json json = Json.create()
                .setCode(CUSTOM_CODE)
                .setMessage(TEST_MESSAGE)
                .setData(TEST_DATA)
                .setTraceId("trace-123");

        assertEquals(CUSTOM_CODE, json.getCode());
        assertEquals(TEST_MESSAGE, json.getMessage());
        assertEquals(TEST_DATA, json.getData());
        assertEquals("trace-123", json.getTraceId());
    }

    // ==================== 内部测试类 ====================

    /**
     * 用于测试的简单对象
     */
    public static class TestObject {
        private String name;
        private int age;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }
}
