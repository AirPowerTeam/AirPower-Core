package cn.hamm.airpower.core;

import cn.hamm.airpower.core.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>RandomUtil 单元测试</h1>
 *
 * <p>覆盖 {@code RandomUtil} 的 8 个 public 方法。随机类只断言
 * <b>长度 / 取值范围 / 字符集</b> 这类确定性性质，不比较具体随机值。</p>
 *
 * @author Hamm.cn
 */
@DisplayName("RandomUtil 随机生成工具类测试")
public class RandomUtilTest {

    /**
     * 默认长度常量（与源码 DEFAULT_LENGTH 保持一致）
     */
    private static final int DEFAULT_LENGTH = 32;

    /**
     * 覆盖范围足够大，使“必然出现某类字符”的断言在概率上不会出现抖动
     */
    private static final int SAMPLES = 2000;

    /**
     * {@code randomString(int)} 使用的字符集：大写 + 小写字母 + 数字
     */
    private static final Pattern ALPHANUMERIC = Pattern.compile("^[0-9A-Za-z]+$");

    @Nested
    @DisplayName("randomBytes 随机字节数组")
    class RandomBytes {

        @Test
        @DisplayName("指定长度时返回该长度的数组")
        void withLength() {
            assertEquals(0, RandomUtil.randomBytes(0).length, "长度为 0 时应返回空数组");
            assertEquals(1, RandomUtil.randomBytes(1).length, "长度 1 应返回 1 个字节");
            assertEquals(16, RandomUtil.randomBytes(16).length, "长度 16 应返回 16 个字节");
            assertEquals(DEFAULT_LENGTH, RandomUtil.randomBytes(DEFAULT_LENGTH).length, "长度 32 应返回 32 个字节");
        }

        @Test
        @DisplayName("不传长度时默认为 32 字节")
        void defaultLength() {
            byte[] bytes = RandomUtil.randomBytes();
            assertNotNull(bytes, "返回值不应为 null");
            assertEquals(DEFAULT_LENGTH, bytes.length, "默认长度应为 32 字节");
        }

        @Test
        @DisplayName("负长度抛 ServiceException：随机字节数组长度不能小于0")
        void negativeLength() {
            ServiceException e1 = assertThrows(ServiceException.class, () -> RandomUtil.randomBytes(-1),
                    "负长度应被参数校验拦截，抛出业务异常而不是 NegativeArraySizeException");
            assertEquals("随机字节数组长度不能小于0", e1.getMessage(), "异常消息应与源码一致");
            assertEquals(Json.SERVICE_ERROR, e1.getCode(), "应携带默认业务错误码 500");

            ServiceException e2 = assertThrows(ServiceException.class, () -> RandomUtil.randomBytes(-100),
                    "任意负长度都应被同一处校验拦截");
            assertEquals("随机字节数组长度不能小于0", e2.getMessage(), "异常消息应一致");
        }

        @Test
        @DisplayName("多次调用结果不相同（内容为随机）")
        void valuesDiffer() {
            Set<String> seen = new HashSet<>();
            for (int i = 0; i < 50; i++) {
                seen.add(java.util.Arrays.toString(RandomUtil.randomBytes(16)));
            }
            assertTrue(seen.size() > 1, "多次调用应产生不同的随机字节内容");
        }
    }

    @Nested
    @DisplayName("randomString 随机字符串（默认字符集）")
    class RandomString {

        @Test
        @DisplayName("不传长度时默认为 32 位")
        void defaultLength() {
            String value = RandomUtil.randomString();
            assertNotNull(value, "返回值不应为 null");
            assertEquals(DEFAULT_LENGTH, value.length(), "默认长度应为 32 位");
        }

        @Test
        @DisplayName("指定长度时返回该长度的字符串")
        void withLength() {
            assertEquals(1, RandomUtil.randomString(1).length(), "长度 1 应返回 1 位");
            assertEquals(8, RandomUtil.randomString(8).length(), "长度 8 应返回 8 位");
            assertEquals(64, RandomUtil.randomString(64).length(), "长度 64 应返回 64 位");
        }

        @Test
        @DisplayName("长度小于等于 0 时被拉回为 1")
        void nonPositiveLength() {
            assertEquals(1, RandomUtil.randomString(0).length(), "长度 0 被 Math.max 拉回为 1");
            assertEquals(1, RandomUtil.randomString(-1).length(), "长度 -1 被 Math.max 拉回为 1");
            assertEquals(1, RandomUtil.randomString(Integer.MIN_VALUE).length(), "极小长度同样被拉回为 1");
        }

        @Test
        @DisplayName("字符集为大小写字母与数字")
        void charset() {
            for (int i = 0; i < 50; i++) {
                assertTrue(ALPHANUMERIC.matcher(RandomUtil.randomString(32)).matches(),
                        "随机字符串只能由大小写字母和数字组成");
            }
        }

        @Test
        @DisplayName("大样本中大小写字母与数字都会出现")
        void allKindsAppear() {
            StringBuilder all = new StringBuilder();
            for (int i = 0; i < SAMPLES; i++) {
                all.append(RandomUtil.randomString(16));
            }
            String sample = all.toString();
            assertTrue(sample.chars().anyMatch(Character::isUpperCase), "大样本中应出现大写字母");
            assertTrue(sample.chars().anyMatch(Character::isLowerCase), "大样本中应出现小写字母");
            assertTrue(sample.chars().anyMatch(Character::isDigit), "大样本中应出现数字");
        }
    }

    @Nested
    @DisplayName("randomNumbers 随机数字字符串")
    class RandomNumbers {

        @Test
        @DisplayName("指定长度时返回该长度的数字串")
        void withLength() {
            assertEquals(1, RandomUtil.randomNumbers(1).length(), "长度 1 应返回 1 位");
            assertEquals(6, RandomUtil.randomNumbers(6).length(), "长度 6 应返回 6 位");
            assertEquals(32, RandomUtil.randomNumbers(32).length(), "长度 32 应返回 32 位");
        }

        @Test
        @DisplayName("长度小于等于 0 时被拉回为 1")
        void nonPositiveLength() {
            assertEquals(1, RandomUtil.randomNumbers(0).length(), "长度 0 被 Math.max 拉回为 1");
            assertEquals(1, RandomUtil.randomNumbers(-5).length(), "负长度同样被拉回为 1");
        }

        @Test
        @DisplayName("结果全部由 0-9 组成")
        void onlyDigits() {
            for (int i = 0; i < 100; i++) {
                String value = RandomUtil.randomNumbers(32);
                for (int j = 0; j < value.length(); j++) {
                    assertTrue(Character.isDigit(value.charAt(j)),
                            "第 " + j + " 个字符 " + value.charAt(j) + " 不是数字");
                }
            }
        }
    }

    @Nested
    @DisplayName("randomString 指定样本的随机字符串")
    class RandomStringFromBase {

        @Test
        @DisplayName("baseString 为 null 抛 ServiceException：随机字符样本不能为空")
        void nullBaseString() {
            ServiceException e = assertThrows(ServiceException.class, () -> RandomUtil.randomString(null, 8),
                    "baseString 为 null 应抛出 ServiceException");
            assertEquals("随机字符样本不能为空", e.getMessage(), "异常消息应与源码一致");
            assertEquals(Json.SERVICE_ERROR, e.getCode(), "应携带默认业务错误码 500");
        }

        @Test
        @DisplayName("baseString 为空串抛 ServiceException：随机字符样本不能为空")
        void emptyBaseString() {
            ServiceException e = assertThrows(ServiceException.class, () -> RandomUtil.randomString("", 8),
                    "baseString 为空串应抛出 ServiceException");
            assertEquals("随机字符样本不能为空", e.getMessage(), "异常消息应与源码一致");
            assertEquals(Json.SERVICE_ERROR, e.getCode(), "应携带默认业务错误码 500");
        }

        @Test
        @DisplayName("baseString 为 null 时不受长度参数影响，始终抛同一异常")
        void nullBaseStringBeforeLengthCheck() {
            ServiceException e = assertThrows(ServiceException.class, () -> RandomUtil.randomString(null, 0),
                    "样本校验先于长度处理，长度非法也先抛 ServiceException");
            assertEquals("随机字符样本不能为空", e.getMessage(), "异常消息应与 null 样本时保持一致");
        }

        @Test
        @DisplayName("结果字符全部取自 baseString")
        void charsetFromBase() {
            String base = "abc";
            for (int i = 0; i < 100; i++) {
                String value = RandomUtil.randomString(base, 16);
                assertEquals(16, value.length(), "长度参数应生效");
                for (int j = 0; j < value.length(); j++) {
                    assertTrue(base.indexOf(value.charAt(j)) >= 0,
                            "第 " + j + " 个字符 " + value.charAt(j) + " 不在 baseString 中");
                }
            }
        }

        @Test
        @DisplayName("单字符样本集只会产生该字符")
        void singleCharBase() {
            assertEquals("########", RandomUtil.randomString("#", 8), "样本只有一个字符时结果必然全是该字符");
        }

        @Test
        @DisplayName("长度小于等于 0 时被拉回为 1")
        void nonPositiveLength() {
            assertEquals(1, RandomUtil.randomString("abc", 0).length(), "长度 0 被 Math.max 拉回为 1");
            assertEquals(1, RandomUtil.randomString("abc", -3).length(), "负长度同样被拉回为 1");
        }

        @Test
        @DisplayName("大样本中 baseString 的所有字符都会出现")
        void allBaseCharsAppear() {
            String base = "abcdef";
            StringBuilder all = new StringBuilder();
            for (int i = 0; i < SAMPLES; i++) {
                all.append(RandomUtil.randomString(base, 16));
            }
            String sample = all.toString();
            for (int i = 0; i < base.length(); i++) {
                assertTrue(sample.indexOf(base.charAt(i)) >= 0,
                        "大样本中应出现 baseString 的字符 " + base.charAt(i));
            }
        }
    }

    @Nested
    @DisplayName("randomInt 随机整数")
    class RandomInt {

        @Test
        @DisplayName("randomInt() 返回任意 int，多次调用不应恒定")
        void anyInt() {
            Set<Integer> seen = new HashSet<>();
            for (int i = 0; i < 200; i++) {
                seen.add(RandomUtil.randomInt());
            }
            assertTrue(seen.size() > 1, "randomInt() 应返回随机的 int，不应恒定");
        }

        @Test
        @DisplayName("randomInt(exclude) 返回 [0, exclude) 区间")
        void withExclude() {
            for (int i = 0; i < SAMPLES; i++) {
                int value = RandomUtil.randomInt(10);
                assertTrue(value >= 0 && value < 10, "结果 " + value + " 应落在 [0, 10) 区间");
            }
            assertTrue(RandomUtil.randomInt(1) == 0, "exclude 为 1 时只能返回 0");
        }

        @Test
        @DisplayName("randomInt(exclude) 在 exclude 小于等于 0 时抛 IllegalArgumentException")
        void withNonPositiveExclude() {
            assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomInt(0),
                    "exclude 为 0 时 ThreadLocalRandom.nextInt 要求 bound 为正");
            assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomInt(-1),
                    "exclude 为负数时同样抛 IllegalArgumentException");
        }

        @Test
        @DisplayName("randomInt(min, max) 为左闭右开区间")
        void minAndMax() {
            for (int i = 0; i < SAMPLES; i++) {
                int value = RandomUtil.randomInt(1, 11);
                assertTrue(value >= 1 && value < 11, "结果 " + value + " 应落在 [1, 11) 区间");
            }
        }

        @Test
        @DisplayName("randomInt(min, max) 支持负数区间")
        void negativeRange() {
            for (int i = 0; i < SAMPLES; i++) {
                int value = RandomUtil.randomInt(-10, 0);
                assertTrue(value >= -10 && value < 0, "结果 " + value + " 应落在 [-10, 0) 区间");
            }
        }

        @Test
        @DisplayName("randomInt(min, max) 在 min 大于等于 max 时抛 IllegalArgumentException")
        void minNotLessThanMax() {
            assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomInt(5, 5),
                    "min 等于 max 时区间为空，抛 IllegalArgumentException");
            assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomInt(5, 3),
                    "min 大于 max 时抛 IllegalArgumentException");
        }

        @Test
        @DisplayName("四参重载：min == max 且两端都包含时返回该值")
        void fourArgsSingleValue() {
            for (int i = 0; i < 10; i++) {
                assertEquals(5, RandomUtil.randomInt(5, 5, true, true),
                        "min=max=5 且两端都包含时，max 自增为 6，结果恒为 5");
            }
        }

        @Test
        @DisplayName("四参重载：includeMin=false 且 min == max 时抛 IllegalArgumentException")
        void fourArgsOriginReachesBound() {
            // min 由 5 自增为 6，max 由 5 自增为 6，调整后 origin == bound
            assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomInt(5, 5, false, true),
                    "调整后 origin 等于 bound，抛 IllegalArgumentException");
        }

        @Test
        @DisplayName("四参重载：min 大于 max 时抛 IllegalArgumentException")
        void fourArgsInvalidRange() {
            assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomInt(5, 3, true, true),
                    "min 大于 max 时抛 IllegalArgumentException");
            assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomInt(5, 3, false, false),
                    "即使不包含端点，min 大于 max 仍抛 IllegalArgumentException");
        }

        @Test
        @DisplayName("四参重载：includeMin / includeMax 决定区间边界")
        void fourArgsBoundaries() {
            for (int i = 0; i < SAMPLES; i++) {
                int closed = RandomUtil.randomInt(1, 10, true, true);
                assertTrue(closed >= 1 && closed <= 10, "两端都包含时结果应落在 [1, 10]，实际 " + closed);

                int halfOpen = RandomUtil.randomInt(1, 10, true, false);
                assertTrue(halfOpen >= 1 && halfOpen < 10, "仅含左端时结果应落在 [1, 10)，实际 " + halfOpen);

                int halfOpenRight = RandomUtil.randomInt(1, 10, false, false);
                assertTrue(halfOpenRight >= 2 && halfOpenRight < 10,
                        "两端都不含时结果应落在 (1, 10)，实际 " + halfOpenRight);

                int rightClosed = RandomUtil.randomInt(1, 10, false, true);
                assertTrue(rightClosed >= 2 && rightClosed <= 10,
                        "仅含右端时结果应落在 (1, 10]，实际 " + rightClosed);
            }
        }

        @Test
        @DisplayName("四参重载：右端点确实可取到")
        void fourArgsMaxReachable() {
            boolean hitMax = false;
            for (int i = 0; i < SAMPLES && !hitMax; i++) {
                hitMax = RandomUtil.randomInt(1, 10, false, true) == 10;
            }
            assertTrue(hitMax, "includeMax=true 时大样本中应能取到上界 10");
        }

        @Test
        @DisplayName("四参重载：includeMax=true 且 max 为 Integer.MAX_VALUE 时整数溢出抛异常")
        void fourArgsIntegerOverflowOnMax() {
            // max 自增后从 Integer.MAX_VALUE 回绕为 Integer.MIN_VALUE，导致 bound <= origin
            assertThrows(IllegalArgumentException.class,
                    () -> RandomUtil.randomInt(1, Integer.MAX_VALUE, true, true),
                    "includeMax=true 时 max++ 溢出，抛 IllegalArgumentException（源码未防溢出）");
        }
    }

    @Nested
    @DisplayName("构造器与常量")
    class Misc {

        @Test
        @DisplayName("工具类为不可实例化的静态方法集合")
        void privateConstructor() throws Exception {
            var constructors = RandomUtil.class.getDeclaredConstructors();
            assertEquals(1, constructors.length, "RandomUtil 只应有一个构造器");
            assertTrue(java.lang.reflect.Modifier.isPrivate(constructors[0].getModifiers()),
                    "构造器应为 private，禁止外部实例化");
        }

        @Test
        @DisplayName("连续两次调用不会返回相同的固定值")
        void notConstant() {
            assertFalse(RandomUtil.randomString(32).equals(RandomUtil.randomString(32)),
                    "随机字符串不应是固定值");
        }
    }
}
