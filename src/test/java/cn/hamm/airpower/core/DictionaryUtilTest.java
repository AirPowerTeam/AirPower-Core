package cn.hamm.airpower.core;

import cn.hamm.airpower.core.exception.ServiceException;
import cn.hamm.airpower.core.fixture.Gender;
import cn.hamm.airpower.core.interfaces.IDictionary;
import cn.hamm.airpower.core.interfaces.IFunction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>枚举字典工具类单元测试</h1>
 *
 * @author Hamm.cn
 * @see DictionaryUtil
 */
@DisplayName("枚举字典工具类 DictionaryUtil 测试")
class DictionaryUtilTest {

    /**
     * <h2>测试用的极简字典枚举</h2>
     */
    private enum SimpleGender implements IDictionary {
        /**
         * 柒
         */
        SEVEN(7, "柒");

        private final int key;

        private final String label;

        SimpleGender(int key, String label) {
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
    }

    /**
     * <h2>用于验证方法名解析规则的字典枚举</h2>
     */
    private enum WeirdDictionary implements IDictionary {
        /**
         * 一
         */
        ONE(1, "一");

        private final int key;

        private final String label;

        WeirdDictionary(int key, String label) {
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
         * 故意把 get 放在方法名中间,用于验证 getLambdaFunctionName 的实现
         *
         * @return 描述
         */
        String forgetLabel() {
            return "已遗忘";
        }
    }

    /**
     * <h2>按 key 查字典</h2>
     */
    @Nested
    @DisplayName("getDictionary(Class, int) 按 Key 查字典")
    class GetDictionaryByKey {

        @Test
        @DisplayName("边界:Key=0 命中第一个枚举项 UNKNOWN")
        void getDictionaryWithZeroKey() {
            Gender gender = DictionaryUtil.getDictionary(Gender.class, 0);
            assertEquals(Gender.UNKNOWN, gender, "Key=0 应命中 UNKNOWN");
            assertEquals("未知", gender.getLabel(), "UNKNOWN 的描述应为未知");
        }

        @Test
        @DisplayName("正常:Key=1 命中 MALE")
        void getDictionaryWithMaleKey() {
            Gender gender = DictionaryUtil.getDictionary(Gender.class, 1);
            assertSame(Gender.MALE, gender, "Key=1 应返回同一个 MALE 枚举实例");
            assertEquals("男", gender.getLabel(), "MALE 的描述应为男");
        }

        @Test
        @DisplayName("正常:Key=2 命中 FEMALE")
        void getDictionaryWithFemaleKey() {
            Gender gender = DictionaryUtil.getDictionary(Gender.class, 2);
            assertSame(Gender.FEMALE, gender, "Key=2 应返回同一个 FEMALE 枚举实例");
            assertEquals("女", gender.getLabel(), "FEMALE 的描述应为女");
        }

        @Test
        @DisplayName("边界:int 最小值不在字典范围内,应抛 ServiceException")
        void getDictionaryWithMinIntKey() {
            ServiceException exception = assertThrows(
                    ServiceException.class,
                    () -> DictionaryUtil.getDictionary(Gender.class, Integer.MIN_VALUE),
                    "超出字典范围的最小 int 应抛出 ServiceException"
            );
            assertEquals("传入的值(Gender=" + Integer.MIN_VALUE + ")不在字典可选范围内",
                    exception.getMessage(), "异常信息应包含类名与传入值");
        }

        @Test
        @DisplayName("边界:int 最大值不在字典范围内,应抛 ServiceException")
        void getDictionaryWithMaxIntKey() {
            ServiceException exception = assertThrows(
                    ServiceException.class,
                    () -> DictionaryUtil.getDictionary(Gender.class, Integer.MAX_VALUE),
                    "超出字典范围的最大 int 应抛出 ServiceException"
            );
            assertEquals("传入的值(Gender=" + Integer.MAX_VALUE + ")不在字典可选范围内",
                    exception.getMessage(), "异常信息应包含类名与传入值");
        }

        @Test
        @DisplayName("异常:Key=99 不在字典范围内,异常应携带可选项列表")
        void getDictionaryWithUnknownKey() {
            ServiceException exception = assertThrows(
                    ServiceException.class,
                    () -> DictionaryUtil.getDictionary(Gender.class, 99),
                    "Key=99 不在字典范围内应抛出 ServiceException"
            );
            assertEquals("传入的值(Gender=99)不在字典可选范围内",
                    exception.getMessage(), "异常信息应为传入的值不在字典可选范围内");
            assertEquals(Json.SERVICE_ERROR, exception.getCode(), "异常错误码应为 500");

            Object data = exception.getData();
            List<?> options = assertInstanceOf(List.class, data, "异常的 getData() 应为字典可选项列表");
            assertEquals(3, options.size(), "可选项列表应有 Gender 的 3 个枚举项");

            Map<?, ?> first = assertInstanceOf(Map.class, options.get(0), "可选项的每一项都应是 Map");
            assertNotNull(first.get("key"), "可选项 Map 应包含 key 字段");
            assertNotNull(first.get("label"), "可选项 Map 应包含 label 字段");
            assertEquals(0, first.get("key"), "第一个可选项的 key 应为 0");
            assertEquals("未知", first.get("label"), "第一个可选项的 label 应为 未知");
        }

        @Test
        @DisplayName("异常:枚举类为 null 时,应抛出 NullPointerException")
        void getDictionaryWithNullClass() {
            assertThrows(
                    NullPointerException.class,
                    () -> DictionaryUtil.getDictionary(null, 1),
                    "枚举类为 null 时源码会在 getEnumConstants() 处抛出 NullPointerException"
            );
        }
    }

    /**
     * <h2>按自定义取值函数查字典</h2>
     */
    @Nested
    @DisplayName("getDictionary(Class, Function, Object) 自定义取值函数查字典")
    class GetDictionaryByFunction {

        @Test
        @DisplayName("正常:按 getLabel 传入 男,应命中 MALE")
        void getDictionaryByLabel() {
            Gender gender = DictionaryUtil.getDictionary(Gender.class, IDictionary::getLabel, "男");
            assertSame(Gender.MALE, gender, "描述为 男 的字典项应是 MALE");
            assertEquals(1, gender.getKey(), "MALE 的 Key 应为 1");
        }

        @Test
        @DisplayName("正常:按 getLabel 传入 女,应命中 FEMALE")
        void getDictionaryByLabelFemale() {
            Gender gender = DictionaryUtil.getDictionary(Gender.class, IDictionary::getLabel, "女");
            assertSame(Gender.FEMALE, gender, "描述为 女 的字典项应是 FEMALE");
        }

        @Test
        @DisplayName("正常:使用自定义 Lambda 取值函数也能查到字典")
        void getDictionaryByCustomLambda() {
            Gender gender = DictionaryUtil.getDictionary(
                    Gender.class, (Gender item) -> "标签-" + item.getKey(), "标签-2"
            );
            assertSame(Gender.FEMALE, gender, "自定义取值函数计算结果为 标签-2 的应是 FEMALE");
        }

        @Test
        @DisplayName("异常:按 getLabel 传入不存在的描述,应抛 ServiceException")
        void getDictionaryByLabelNotFound() {
            ServiceException exception = assertThrows(
                    ServiceException.class,
                    () -> DictionaryUtil.getDictionary(Gender.class, IDictionary::getLabel, "不存在的描述"),
                    "描述不存在时不应返回字典项");
            assertEquals("传入的值(Gender=不存在的描述)不在字典可选范围内",
                    exception.getMessage(), "异常信息应包含类名与传入值");
            assertEquals(3, ((List<?>) exception.getData()).size(), "异常应附带 3 个可选项");
        }

        @Test
        @DisplayName("异常:比较值为 null 时,应抛 ServiceException 且信息中为 null")
        void getDictionaryByNullValue() {
            ServiceException exception = assertThrows(
                    ServiceException.class,
                    () -> DictionaryUtil.getDictionary(Gender.class, IDictionary::getLabel, null),
                    "比较值为 null 时不应命中任何字典项"
            );
            assertEquals("传入的值(Gender=null)不在字典可选范围内",
                    exception.getMessage(), "异常信息中应原样输出 null");
        }

        @Test
        @DisplayName("异常:取值函数为 null 时,应抛出 NullPointerException")
        void getDictionaryByNullFunction() {
            assertThrows(
                    NullPointerException.class,
                    () -> DictionaryUtil.getDictionary(Gender.class, null, 1),
                    "取值函数为 null 时源码会在 function.apply 处抛出 NullPointerException"
            );
        }
    }

    /**
     * <h2>获取字典可选项列表</h2>
     */
    @Nested
    @DisplayName("getDictionaryList(Class) 获取默认字典可选项列表")
    class GetDictionaryList {

        @Test
        @DisplayName("正常:应返回 3 个可选项,键名为 key 与 label")
        void getDictionaryListDefault() {
            List<Map<String, Object>> list = DictionaryUtil.getDictionaryList(Gender.class);
            assertEquals(3, list.size(), "Gender 应有 3 个可选项");

            Map<String, Object> unknown = list.get(0);
            assertEquals(2, unknown.size(), "默认可选项的 Map 应有 2 个键");
            assertEquals(0, unknown.get("key"), "第 1 项的 key 应为 0");
            assertEquals("未知", unknown.get("label"), "第 1 项的 label 应为 未知");

            Map<String, Object> male = list.get(1);
            assertEquals(1, male.get("key"), "第 2 项的 key 应为 1");
            assertEquals("男", male.get("label"), "第 2 项的 label 应为 男");

            Map<String, Object> female = list.get(2);
            assertEquals(2, female.get("key"), "第 3 项的 key 应为 2");
            assertEquals("女", female.get("label"), "第 3 项的 label 应为 女");
        }

        @Test
        @DisplayName("正常:枚举顺序与 values() 声明顺序一致")
        void getDictionaryListKeepsDeclarationOrder() {
            List<Map<String, Object>> list = DictionaryUtil.getDictionaryList(Gender.class);
            List<Gender> values = List.of(Gender.values());
            for (int i = 0; i < values.size(); i++) {
                assertEquals(values.get(i).getKey(), list.get(i).get("key"),
                        "第 " + (i + 1) + " 项的 key 应与枚举声明顺序一致");
                assertEquals(values.get(i).getLabel(), list.get(i).get("label"),
                        "第 " + (i + 1) + " 项的 label 应与枚举声明顺序一致");
            }
        }

        @Test
        @DisplayName("正常:key 的值类型为 Integer,label 的值类型为 String")
        void getDictionaryListValueTypes() {
            Map<String, Object> item = DictionaryUtil.getDictionaryList(Gender.class).get(0);
            assertInstanceOf(Integer.class, item.get("key"), "key 的值应为 Integer");
            assertInstanceOf(String.class, item.get("label"), "label 的值应为 String");
        }

        @Test
        @DisplayName("边界:可选项列表与 Map 均为可变对象(与 TreeUtil 的不可变返回不同)")
        void getDictionaryListIsMutable() {
            List<Map<String, Object>> list = DictionaryUtil.getDictionaryList(Gender.class);
            assertDoesNotThrow(() -> list.add(new HashMap<>()),
                    "getDictionaryList 返回的是 ArrayList,允许新增元素");
            assertEquals(4, list.size(), "新增后列表长度应为 4");
            assertDoesNotThrow(() -> list.get(0).put("额外", "值"),
                    "可选项 Map 是 HashMap,允许写入新的键");
            assertEquals(3, list.get(0).size(), "写入后可选项 Map 应有 3 个键");
        }

        @Test
        @DisplayName("异常:枚举类为 null 时,应抛出 NullPointerException")
        void getDictionaryListWithNullClass() {
            assertThrows(
                    NullPointerException.class,
                    () -> DictionaryUtil.getDictionaryList(null),
                    "枚举类为 null 时源码会在 getEnumConstants() 处抛出 NullPointerException"
            );
        }
    }

    /**
     * <h2>按指定方法表达式获取字典可选项列表</h2>
     */
    @Nested
    @DisplayName("getDictionaryList(Class, IFunction...) 指定方法表达式取字典")
    class GetDictionaryListByLambdas {

        @Test
        @DisplayName("正常:只传 getKey 时,Map 中只有 key 一个键")
        void getDictionaryListWithKeyOnly() {
            List<Map<String, Object>> list = DictionaryUtil.getDictionaryList(Gender.class, IDictionary::getKey);
            assertEquals(3, list.size(), "应返回 3 个可选项");
            assertEquals(1, list.get(0).size(), "只传一个方法表达式时每项只有 1 个键");
            assertEquals(0, list.get(0).get("key"), "第 1 项的 key 应为 0");
            assertNull(list.get(0).get("label"), "未传 getLabel 时不应存在 label 键");
        }

        @Test
        @DisplayName("正常:只传 getLabel 时,Map 中只有 label 一个键")
        void getDictionaryListWithLabelOnly() {
            List<Map<String, Object>> list = DictionaryUtil.getDictionaryList(Gender.class, IDictionary::getLabel);
            assertEquals(1, list.get(0).size(), "只传一个方法表达式时每项只有 1 个键");
            assertEquals("未知", list.get(0).get("label"), "第 1 项的 label 应为 未知");
            assertNull(list.get(0).get("key"), "未传 getKey 时不应存在 key 键");
        }

        @Test
        @DisplayName("正常:传入顺序不影响最终 Map 的内容")
        void getDictionaryListOrderIndependent() {
            List<Map<String, Object>> list = DictionaryUtil.getDictionaryList(
                    Gender.class, IDictionary::getKey, IDictionary::getLabel
            );
            Map<String, Object> male = list.get(1);
            assertEquals(2, male.size(), "传入两个方法表达式时每项应有 2 个键");
            assertEquals(1, male.get("key"), "MALE 的 key 应为 1");
            assertEquals("男", male.get("label"), "MALE 的 label 应为 男");
        }

        @Test
        @DisplayName("边界:传入零个方法表达式时,每项为空 Map")
        @SuppressWarnings({"unchecked", "rawtypes"})
        void getDictionaryListWithEmptyLambdas() {
            List<Map<String, Object>> list = DictionaryUtil.getDictionaryList(Gender.class, new IFunction[0]);
            assertEquals(3, list.size(), "零个方法表达式时仍应返回 3 个可选项");
            for (Map<String, Object> item : list) {
                assertTrue(item.isEmpty(), "零个方法表达式时每项应为空 Map");
            }
        }

        @Test
        @DisplayName("边界:重复传入同一方法,Map 中只会保留一个键")
        void getDictionaryListWithDuplicatedLambdas() {
            List<Map<String, Object>> list = DictionaryUtil.getDictionaryList(
                    Gender.class, IDictionary::getKey, IDictionary::getKey
            );
            assertEquals(1, list.get(0).size(), "重复传入同一方法后每项只剩 1 个键");
            assertEquals(0, list.get(0).get("key"), "重复写入后 key 的值仍为 0");
        }

        @Test
        @DisplayName("边界:传入普通 Lambda 表达式时,键名退化为编译器生成的 lambda 方法名")
        void lambdaExpressionKeyFallsBackToSyntheticName() {
            List<Map<String, Object>> list = DictionaryUtil.getDictionaryList(
                    Gender.class, (Gender item) -> item.getLabel()
            );
            Map<String, Object> item = list.get(1);
            assertEquals(1, item.size(), "Lambda 表达式同样只产生 1 个键");
            String key = item.keySet().iterator().next();
            assertTrue(key.startsWith("lambda$"), "Lambda 表达式的键名应为编译器生成的 lambda$ 前缀,实际为 " + key);
            assertEquals("男", item.get(key), "Lambda 表达式取到的值仍应是 MALE 的描述");
        }

        @Test
        @DisplayName("边界:方法名中含 get 字样的非前缀方法,所有 get 都会被移除(源码使用 replace 而非去除前缀)")
        void methodNameWithGetInMiddleLosesEveryGet() {
            List<Map<String, Object>> list = DictionaryUtil.getDictionaryList(
                    WeirdDictionary.class, WeirdDictionary::forgetLabel
            );
            Map<String, Object> item = list.get(0);
            assertEquals(1, item.size(), "该方法表达式应只产生 1 个键");
            assertEquals("forLabel", item.keySet().iterator().next(),
                    "方法名 forgetLabel 中的 get 被整体移除后应为 forLabel");
            assertEquals("已遗忘", item.get("forLabel"), "取到的值应为 ONE 的 forgetLabel() 结果");
        }

        @Test
        @DisplayName("正常:实现类的方法引用同样以小写首字母作为键名")
        void concreteClassMethodReference() {
            List<Map<String, Object>> list = DictionaryUtil.getDictionaryList(
                    WeirdDictionary.class, WeirdDictionary::getLabel
            );
            assertEquals("一", list.get(0).get("label"), "具体类方法引用 getLabel 应生成 label 键");
        }
    }

    /**
     * <h2>异常数据回填一致性</h2>
     */
    @Nested
    @DisplayName("查字典失败时的异常数据一致性")
    class ExceptionDataConsistency {

        @Test
        @DisplayName("正常:异常携带的可选项应与 getDictionaryList 的结果完全一致")
        void exceptionDataEqualsDictionaryList() {
            ServiceException exception = assertThrows(
                    ServiceException.class,
                    () -> DictionaryUtil.getDictionary(Gender.class, 66),
                    "Key=66 不在字典范围内"
            );
            assertEquals(DictionaryUtil.getDictionaryList(Gender.class), exception.getData(),
                    "异常携带的可选项应与 getDictionaryList(Gender.class) 的结果一致");
        }

        @Test
        @DisplayName("正常:仅实现 getKey/getLabel 的枚举也能正常参与字典处理")
        void simpleDictionaryEnum() {
            IDictionary simple = DictionaryUtil.getDictionary(SimpleGender.class, 7);
            assertEquals(SimpleGender.SEVEN, simple, "Key=7 应命中 SimpleGender.SEVEN");
            assertEquals("柒", simple.getLabel(), "SimpleGender.SEVEN 的描述应为 柒");
        }
    }
}
