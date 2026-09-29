package cn.hamm.airpower.core.interfaces;

import cn.hamm.airpower.core.fixture.Gender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>枚举字典标准接口单元测试</h1>
 *
 * @author Hamm.cn
 * @see IDictionary
 */
@DisplayName("枚举字典标准接口 IDictionary 测试")
class IDictionaryTest {

    /**
     * 获取接口中指定签名的方法
     *
     * @param name       方法名
     * @param paramTypes 参数类型
     * @return 方法对象
     */
    private static Method requireMethod(String name, Class<?>... paramTypes) {
        try {
            return IDictionary.class.getDeclaredMethod(name, paramTypes);
        } catch (NoSuchMethodException e) {
            throw new AssertionError("IDictionary 接口中应存在方法 " + name, e);
        }
    }

    /**
     * <h2>仅实现两个抽象方法的字典实现</h2>
     */
    private static final class SimpleDictionary implements IDictionary {
        private final int key;

        private final String label;

        /**
         * 构造一个字典实现
         *
         * @param key   字典值
         * @param label 字典描述
         */
        private SimpleDictionary(int key, String label) {
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
     * <h2>字典基础取值</h2>
     */
    @Nested
    @DisplayName("getKey / getLabel 基础取值")
    class BasicAccessor {

        @Test
        @DisplayName("正常:枚举实现应返回声明的 Key 与描述")
        void enumValuesAreCorrect() {
            assertEquals(0, Gender.UNKNOWN.getKey(), "UNKNOWN 的 Key 应为 0");
            assertEquals("未知", Gender.UNKNOWN.getLabel(), "UNKNOWN 的描述应为 未知");
            assertEquals(1, Gender.MALE.getKey(), "MALE 的 Key 应为 1");
            assertEquals("男", Gender.MALE.getLabel(), "MALE 的描述应为 男");
            assertEquals(2, Gender.FEMALE.getKey(), "FEMALE 的 Key 应为 2");
            assertEquals("女", Gender.FEMALE.getLabel(), "FEMALE 的描述应为 女");
        }

        @Test
        @DisplayName("正常:实现类可自由提供 Key 与描述,接口不做任何约束")
        void customImplementationValues() {
            IDictionary custom = new SimpleDictionary(-5, "自定义");
            assertEquals(-5, custom.getKey(), "实现类可以返回负数 Key");
            assertEquals("自定义", custom.getLabel(), "实现类应原样返回描述");
        }

        @Test
        @DisplayName("边界:实现类返回 null 描述时不应抛异常")
        void nullLabelIsAllowed() {
            IDictionary custom = new SimpleDictionary(9, null);
            assertEquals(9, custom.getKey(), "Key 不受空值影响");
            assertNull(custom.getLabel(), "接口未对描述做非空约束,可返回 null");
        }
    }

    /**
     * <h2>默认方法 equalsKey</h2>
     */
    @Nested
    @DisplayName("默认方法 equalsKey")
    class EqualsKey {

        @Test
        @DisplayName("正常:Key 相等时返回 true")
        void equalsKeyReturnsTrue() {
            assertTrue(Gender.MALE.equalsKey(1), "MALE.equalsKey(1) 应为 true");
            assertTrue(Gender.UNKNOWN.equalsKey(0), "UNKNOWN.equalsKey(0) 应为 true");
            assertTrue(Gender.FEMALE.equalsKey(2), "FEMALE.equalsKey(2) 应为 true");
        }

        @Test
        @DisplayName("正常:Key 不等时返回 false")
        void equalsKeyReturnsFalse() {
            assertFalse(Gender.MALE.equalsKey(2), "MALE.equalsKey(2) 应为 false");
            assertFalse(Gender.UNKNOWN.equalsKey(1), "UNKNOWN.equalsKey(1) 应为 false");
            assertFalse(Gender.FEMALE.equalsKey(0), "FEMALE.equalsKey(0) 应为 false");
        }

        @Test
        @DisplayName("边界:int 最小值与最大值均不会误判")
        void intBoundaryValues() {
            assertFalse(Gender.MALE.equalsKey(Integer.MIN_VALUE), "任何枚举项都不应等于 int 最小值");
            assertFalse(Gender.MALE.equalsKey(Integer.MAX_VALUE), "任何枚举项都不应等于 int 最大值");
            assertTrue(new SimpleDictionary(Integer.MIN_VALUE, "最小值").equalsKey(Integer.MIN_VALUE),
                    "Key 为 int 最小值时应判定为相等");
            assertTrue(new SimpleDictionary(Integer.MAX_VALUE, "最大值").equalsKey(Integer.MAX_VALUE),
                    "Key 为 int 最大值时应判定为相等");
        }

        @Test
        @DisplayName("正常:对全部枚举项与全部候选 Key 逐一比对结果正确")
        void equalsKeyAgainstAllKeys() {
            int[] candidates = {0, 1, 2, 3, -1, 100};
            for (Gender gender : Gender.values()) {
                for (int candidate : candidates) {
                    assertEquals(gender.getKey() == candidate, gender.equalsKey(candidate),
                            gender.name() + " 与 " + candidate + " 的比较结果应与直接比较一致");
                }
            }
        }

        @Test
        @DisplayName("正常:仅实现 getKey/getLabel 的类也能使用默认方法")
        void defaultMethodWorksOnCustomImplementation() {
            IDictionary custom = new SimpleDictionary(7, "柒");
            assertTrue(custom.equalsKey(7), "自定义实现的 Key=7 应判定为相等");
            assertFalse(custom.equalsKey(8), "自定义实现的 Key=7 与 8 不应相等");
        }
    }

    /**
     * <h2>测试辅助</h2>
     */

    /**
     * <h2>默认方法 notEqualsKey</h2>
     */
    @Nested
    @DisplayName("默认方法 notEqualsKey")
    class NotEqualsKey {

        @Test
        @DisplayName("正常:Key 不等时返回 true")
        void notEqualsKeyReturnsTrue() {
            assertTrue(Gender.MALE.notEqualsKey(2), "MALE.notEqualsKey(2) 应为 true");
            assertTrue(Gender.UNKNOWN.notEqualsKey(1), "UNKNOWN.notEqualsKey(1) 应为 true");
            assertTrue(Gender.FEMALE.notEqualsKey(0), "FEMALE.notEqualsKey(0) 应为 true");
        }

        @Test
        @DisplayName("正常:Key 相等时返回 false")
        void notEqualsKeyReturnsFalse() {
            assertFalse(Gender.MALE.notEqualsKey(1), "MALE.notEqualsKey(1) 应为 false");
            assertFalse(Gender.UNKNOWN.notEqualsKey(0), "UNKNOWN.notEqualsKey(0) 应为 false");
            assertFalse(Gender.FEMALE.notEqualsKey(2), "FEMALE.notEqualsKey(2) 应为 false");
        }

        @Test
        @DisplayName("边界:int 最小值与最大值均不会误判")
        void intBoundaryValues() {
            assertTrue(Gender.MALE.notEqualsKey(Integer.MIN_VALUE), "MALE 的 Key 不可能是 int 最小值");
            assertTrue(Gender.MALE.notEqualsKey(Integer.MAX_VALUE), "MALE 的 Key 不可能是 int 最大值");
        }

        @Test
        @DisplayName("正常:notEqualsKey 恒为 equalsKey 的取反")
        void notEqualsKeyIsNegationOfEqualsKey() {
            int[] candidates = {0, 1, 2, 3, -1, Integer.MIN_VALUE, Integer.MAX_VALUE};
            for (Gender gender : Gender.values()) {
                for (int candidate : candidates) {
                    assertEquals(!gender.equalsKey(candidate), gender.notEqualsKey(candidate),
                            gender.name() + " 对 " + candidate + " 的两个默认方法结果应互为反义");
                }
            }
        }
    }

    /**
     * <h2>接口结构约定</h2>
     */
    @Nested
    @DisplayName("接口结构与默认方法约定")
    class InterfaceStructure {

        @Test
        @DisplayName("正常:只有 getKey 与 getLabel 是抽象方法")
        void onlyTwoAbstractMethods() {
            List<Method> declared = Arrays.stream(IDictionary.class.getDeclaredMethods()).toList();
            List<Method> abstractMethods = declared.stream()
                    .filter(method -> !method.isDefault())
                    .collect(Collectors.toList());

            assertEquals(2, abstractMethods.size(), "接口应只声明 2 个抽象方法");
            assertEquals(
                    Set.of("getKey", "getLabel"),
                    abstractMethods.stream().map(Method::getName).collect(Collectors.toSet()),
                    "两个抽象方法应为 getKey 与 getLabel"
            );
        }

        @Test
        @DisplayName("正常:equalsKey 与 notEqualsKey 是默认方法")
        void defaultMethodsAreDefault() {
            Method equalsKey = requireMethod("equalsKey", int.class);
            Method notEqualsKey = requireMethod("notEqualsKey", int.class);

            assertTrue(equalsKey.isDefault(), "equalsKey 应为 default 方法,实现类无需覆写");
            assertTrue(notEqualsKey.isDefault(), "notEqualsKey 应为 default 方法,实现类无需覆写");
            assertFalse(Modifier.isAbstract(equalsKey.getModifiers()), "equalsKey 不应是抽象方法");
            assertEquals(boolean.class, equalsKey.getReturnType(), "equalsKey 应返回 boolean");
            assertEquals(boolean.class, notEqualsKey.getReturnType(), "notEqualsKey 应返回 boolean");
        }

        @Test
        @DisplayName("正常:匿名实现只需实现两个抽象方法即可使用默认方法")
        void anonymousImplementationOnlyNeedsAbstractMethods() {
            IDictionary anonymous = new IDictionary() {
                @Override
                public int getKey() {
                    return 42;
                }

                @Override
                public String getLabel() {
                    return "匿名实现";
                }
            };

            assertTrue(anonymous.equalsKey(42), "匿名实现未覆写默认方法,仍应可用 equalsKey");
            assertTrue(anonymous.notEqualsKey(43), "匿名实现未覆写默认方法,仍应可用 notEqualsKey");
        }

        @Test
        @DisplayName("正常:所有枚举项的 Key 在自定义实现中同样可比对")
        void allGenderKeysCanBeQueried() {
            for (int key : new int[]{0, 1, 2}) {
                assertTrue(Arrays.stream(Gender.values()).anyMatch(gender -> gender.equalsKey(key)),
                        "Gender 中应存在 Key 等于 " + key + " 的字典项");
            }
        }
    }
}
