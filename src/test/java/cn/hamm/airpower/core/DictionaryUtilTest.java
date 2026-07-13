package cn.hamm.airpower.core;

import cn.hamm.airpower.core.exception.ServiceException;
import cn.hamm.airpower.core.interfaces.IDictionary;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>DictionaryUtil 单元测试</h1>
 *
 * @author Hamm.cn
 */
class DictionaryUtilTest {

    /**
     * 测试用的字典枚举
     */
    enum TestDictionary implements IDictionary {
        /**
         * 选项一
         */
        OPTION_ONE(1, "选项一"),

        /**
         * 选项二
         */
        OPTION_TWO(2, "选项二"),

        /**
         * 选项三
         */
        OPTION_THREE(3, "选项三");

        private final int key;
        private final String label;

        TestDictionary(int key, String label) {
            this.key = key;
            this.label = label;
        }

        @Override
        public int getKey() {
            return key;
        }

        @Override
        public String getLabel() {
            return label;
        }

        /**
         * 获取扩展属性（用于测试 lambda 方法引用）
         */
        public String getExtra() {
            return "extra_" + key;
        }
    }

    /**
     * 空枚举（用于测试边界情况）
     */
    enum EmptyDictionary implements IDictionary {
        ;

        @Override
        public int getKey() {
            return 0;
        }

        @Override
        public String getLabel() {
            return "";
        }
    }

    // ==================== getDictionary 方法测试（按 key 查找） ====================

    @Test
    void testGetDictionaryByKeyWithExistingValue() {
        TestDictionary result = DictionaryUtil.getDictionary(TestDictionary.class, 1);
        assertNotNull(result);
        assertEquals(TestDictionary.OPTION_ONE, result);
    }

    @Test
    void testGetDictionaryByKeyWithAnotherExistingValue() {
        TestDictionary result = DictionaryUtil.getDictionary(TestDictionary.class, 2);
        assertNotNull(result);
        assertEquals(TestDictionary.OPTION_TWO, result);
    }

    @Test
    void testGetDictionaryByKeyWithLastOption() {
        TestDictionary result = DictionaryUtil.getDictionary(TestDictionary.class, 3);
        assertNotNull(result);
        assertEquals(TestDictionary.OPTION_THREE, result);
    }

    @Test
    void testGetDictionaryByKeyWithNonExistingValue() {
        ServiceException exception = assertThrows(ServiceException.class, () ->
                DictionaryUtil.getDictionary(TestDictionary.class, 999)
        );
        assertTrue(exception.getMessage().contains("不在字典可选范围内"));
    }

    @Test
    void testGetDictionaryByKeyWithNegativeValue() {
        assertThrows(ServiceException.class, () ->
                DictionaryUtil.getDictionary(TestDictionary.class, -1)
        );
    }

    @Test
    void testGetDictionaryByKeyWithZero() {
        assertThrows(ServiceException.class, () ->
                DictionaryUtil.getDictionary(TestDictionary.class, 0)
        );
    }

    // ==================== getDictionary 方法测试（按自定义 function 查找） ====================

    @Test
    void testGetDictionaryByFunctionWithExistingLabel() {
        TestDictionary result = DictionaryUtil.getDictionary(
                TestDictionary.class, IDictionary::getLabel, "选项二"
        );
        assertNotNull(result);
        assertEquals(TestDictionary.OPTION_TWO, result);
    }

    @Test
    void testGetDictionaryByFunctionWithNonExistingLabel() {
        assertThrows(ServiceException.class, () ->
                DictionaryUtil.getDictionary(TestDictionary.class, IDictionary::getLabel, "不存在的选项")
        );
    }

    @Test
    void testGetDictionaryByFunctionWithNullValue() {
        assertThrows(ServiceException.class, () ->
                DictionaryUtil.getDictionary(TestDictionary.class, IDictionary::getLabel, null)
        );
    }

    @Test
    void testGetDictionaryByFunctionWithExtraProperty() {
        TestDictionary result = DictionaryUtil.getDictionary(
                TestDictionary.class, TestDictionary::getExtra, "extra_2"
        );
        assertNotNull(result);
        assertEquals(TestDictionary.OPTION_TWO, result);
    }

    // ==================== getDictionaryList 方法测试（默认 key + label） ====================

    @Test
    void testGetDictionaryListWithDefaultParams() {
        List<Map<String, Object>> result = DictionaryUtil.getDictionaryList(TestDictionary.class);
        assertNotNull(result);
        assertEquals(3, result.size());

        Map<String, Object> firstItem = result.get(0);
        assertEquals(1, firstItem.get("key"));
        assertEquals("选项一", firstItem.get("label"));

        Map<String, Object> secondItem = result.get(1);
        assertEquals(2, secondItem.get("key"));
        assertEquals("选项二", secondItem.get("label"));
    }

    @Test
    void testGetDictionaryListWithEmptyEnum() {
        List<Map<String, Object>> result = DictionaryUtil.getDictionaryList(EmptyDictionary.class);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== getDictionaryList 方法测试（自定义 lambda 参数） ====================

    @Test
    void testGetDictionaryListWithCustomLambdas() {
        List<Map<String, Object>> result = DictionaryUtil.getDictionaryList(
                TestDictionary.class,
                TestDictionary::getKey,
                TestDictionary::getLabel,
                TestDictionary::getExtra
        );
        assertNotNull(result);
        assertEquals(3, result.size());

        Map<String, Object> firstItem = result.get(0);
        assertEquals(1, firstItem.get("key"));
        assertEquals("选项一", firstItem.get("label"));
        assertEquals("extra_1", firstItem.get("extra"));
    }

    @Test
    void testGetDictionaryListWithSingleLambda() {
        List<Map<String, Object>> result = DictionaryUtil.getDictionaryList(
                TestDictionary.class,
                TestDictionary::getKey
        );
        assertNotNull(result);
        assertEquals(3, result.size());

        Map<String, Object> firstItem = result.get(0);
        assertEquals(1, firstItem.get("key"));
        assertNull(firstItem.get("label"));
    }

    @Test
    void testGetDictionaryListWithNoLambdas() {
        // 传入空的 lambda 数组，应该返回空 map 的列表
        List<Map<String, Object>> result = DictionaryUtil.getDictionaryList(TestDictionary.class);
        assertNotNull(result);
        assertEquals(3, result.size());
    }

    // ==================== IDictionary 接口默认方法测试 ====================

    @Test
    void testEqualsKeyWithSameKey() {
        assertTrue(TestDictionary.OPTION_ONE.equalsKey(1));
    }

    @Test
    void testEqualsKeyWithDifferentKey() {
        assertFalse(TestDictionary.OPTION_ONE.equalsKey(2));
    }

    @Test
    void testNotEqualsKeyWithSameKey() {
        assertFalse(TestDictionary.OPTION_ONE.notEqualsKey(1));
    }

    @Test
    void testNotEqualsKeyWithDifferentKey() {
        assertTrue(TestDictionary.OPTION_ONE.notEqualsKey(2));
    }
}
