package cn.hamm.airpower.core;

import cn.hamm.airpower.core.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>AES 加解密工具类测试</h1>
 * <p>
 * 覆盖：链式工厂、密钥/偏移向量/填充模式配置、加解密往返、{@code null} 入参、
 * 密钥与偏移向量长度边界、非法 Base64、异常分支。
 * </p>
 *
 * @author Hamm
 */
@DisplayName("AesUtil AES 加解密工具类测试")
class AesUtilTest {

    /**
     * 16 字节密钥（AES-128）
     */
    private static final byte[] KEY_128 = "0123456789abcdef".getBytes(UTF_8);

    /**
     * 24 字节密钥（AES-192）
     */
    private static final byte[] KEY_192 = "0123456789abcdef01234567".getBytes(UTF_8);

    /**
     * 32 字节密钥（AES-256）
     */
    private static final byte[] KEY_256 = "0123456789abcdef0123456789abcdef".getBytes(UTF_8);

    /**
     * 源码中的默认偏移向量
     */
    private static final byte[] DEFAULT_IV = "0000000000000000".getBytes(UTF_8);

    /**
     * 自定义偏移向量
     */
    private static final byte[] CUSTOM_IV = "abcdef9876543210".getBytes(UTF_8);

    /**
     * 待测实例，每个测试用例独立创建，避免相互干扰
     */
    private AesUtil aesUtil;

    /**
     * 组装一个配置好密钥的实例
     *
     * @param key 密钥
     * @return 实例
     */
    private static AesUtil withKey(byte[] key) {
        return AesUtil.create().setKey(key);
    }

    /**
     * 断言两个引用指向同一个对象
     *
     * @param expected 期望对象
     * @param actual   实际对象
     */
    private static void assertSameInstance(Object expected, Object actual) {
        org.junit.jupiter.api.Assertions.assertSame(expected, actual, "链式调用应返回同一个实例");
    }

    /**
     * 每个用例前创建一个干净的实例
     */
    @BeforeEach
    void setUp() {
        aesUtil = AesUtil.create();
    }

    @Nested
    @DisplayName("1. 实例创建与链式配置")
    class CreateAndChain {

        @Test
        @DisplayName("create() 每次返回全新的实例")
        void testCreateReturnsNewInstance() {
            AesUtil one = AesUtil.create();
            AesUtil two = AesUtil.create();
            assertNotSame(one, two, "create() 每次都应返回一个新的实例");
            assertTrue(one instanceof AesUtil, "create() 的返回值类型应为 AesUtil");
        }

        @Test
        @DisplayName("setKey(byte[])、setIv(byte[])、setPadding(String) 均返回 this")
        void testChainReturnSelf() {
            AesUtil instance = AesUtil.create();
            AesUtil afterKey = instance.setKey(KEY_128);
            AesUtil afterIv = afterKey.setIv(CUSTOM_IV);
            AesUtil afterPadding = afterIv.setPadding("PKCS7Padding");
            assertAll("链式调用的每一步都应返回同一个实例",
                    () -> assertSameInstance(instance, afterKey),
                    () -> assertSameInstance(instance, afterIv),
                    () -> assertSameInstance(instance, afterPadding));
        }

        @Test
        @DisplayName("setKey(String) 同样返回 this")
        void testSetKeyStringChain() {
            AesUtil instance = AesUtil.create();
            String base64Key = Base64.getEncoder().encodeToString(KEY_128);
            assertSame(instance, instance.setKey(base64Key), "setKey(String) 应返回 this");
        }

        @Test
        @DisplayName("默认偏移向量为 0000000000000000")
        void testDefaultIv() {
            String source = "默认偏移向量测试";
            String encrypted = withKey(KEY_128).encrypt(source);
            // 不设置 iv 的实例与显式设置为默认值的实例应能互相解密
            String decrypted = withKey(KEY_128).setIv(DEFAULT_IV).decrypt(encrypted);
            assertEquals(source, decrypted, "默认偏移向量应等价于 0000000000000000");
        }

        @Test
        @DisplayName("algorithm 与 mode 不可修改，Lombok 未生成对应 setter")
        void testAlgorithmAndModeImmutable() {
            assertAll("algorithm 与 mode 声明了 @Setter(AccessLevel.NONE)，不应存在 setter",
                    () -> assertThrows(NoSuchMethodException.class,
                            () -> AesUtil.class.getMethod("setAlgorithm", String.class),
                            "不应存在 setAlgorithm 方法"),
                    () -> assertThrows(NoSuchMethodException.class,
                            () -> AesUtil.class.getMethod("setMode", String.class),
                            "不应存在 setMode 方法"));
        }

        @Test
        @DisplayName("未设置 key 时加解密均抛出 ServiceException")
        void testCryptWithoutKey() {
            ServiceException decryptException = assertThrows(ServiceException.class,
                    () -> AesUtil.create().decrypt("dGVzdA=="),
                    "未设置密钥时解密应抛出 ServiceException");
            assertEquals("加密密钥未设置", decryptException.getMessage(), "异常信息应说明密钥未设置");
            assertThrows(ServiceException.class, () -> AesUtil.create().encrypt("内容"),
                    "未设置密钥时加密应抛出 ServiceException");
        }
    }

    @Nested
    @DisplayName("2. 正常加解密往返")
    class RoundTrip {

        @Test
        @DisplayName("16 / 24 / 32 字节密钥均可正常往返")
        void testRoundTripWithAllKeyLengths() {
            String source = "AirPower-Core AES 往返测试";
            assertAll("三种合法密钥长度都应能正确往返",
                    () -> assertEquals(source, withKey(KEY_128).decrypt(withKey(KEY_128).encrypt(source)),
                            "AES-128 解密结果应与原文一致"),
                    () -> assertEquals(source, withKey(KEY_192).decrypt(withKey(KEY_192).encrypt(source)),
                            "AES-192 解密结果应与原文一致"),
                    () -> assertEquals(source, withKey(KEY_256).decrypt(withKey(KEY_256).encrypt(source)),
                            "AES-256 解密结果应与原文一致"));
        }

        @Test
        @DisplayName("密钥长度边界：15 / 17 / 31 / 33 字节均非法")
        void testInvalidKeyLengths() {
            int[] invalidLengths = {15, 17, 31, 33};
            for (int length : invalidLengths) {
                byte[] key = new byte[length];
                java.util.Arrays.fill(key, (byte) 'a');
                ServiceException exception = assertThrows(ServiceException.class,
                        () -> AesUtil.create().setKey(key),
                        "长度为 " + length + " 字节的密钥应在设置时就抛出 ServiceException");
                assertTrue(exception.getMessage().contains("16、24 或 32 字节"),
                        "异常信息应明确指出合法长度，实际为：" + exception.getMessage());
            }
        }

        @Test
        @DisplayName("空字符串往返一致，密文为一个填充分组")
        void testEmptyStringRoundTrip() {
            AesUtil instance = withKey(KEY_128);
            String encrypted = instance.encrypt("");
            assertFalse(encrypted.isEmpty(), "空字符串加密后不应为空（PKCS5 填充会补满一个分组）");
            assertEquals(16, Base64.getDecoder().decode(encrypted).length,
                    "空字符串的密文长度应为 16 字节");
            assertEquals("", instance.decrypt(encrypted), "空字符串解密结果应为空字符串");
        }

        @Test
        @DisplayName("单字符往返一致")
        void testSingleCharacter() {
            String source = "A";
            assertEquals(source, withKey(KEY_128).decrypt(withKey(KEY_128).encrypt(source)),
                    "单字符往返后应与原文一致");
        }

        @Test
        @DisplayName("ASCII 文本往返一致")
        void testAsciiText() {
            String source = "The quick brown fox jumps over the lazy dog. 0123456789 !@#$%^&*()";
            assertEquals(source, withKey(KEY_128).decrypt(withKey(KEY_128).encrypt(source)),
                    "ASCII 文本往返后应与原文一致");
        }

        @Test
        @DisplayName("中文文本往返一致")
        void testChineseText() {
            String source = "空气动力核心 AirPower，测试中文加密！";
            assertEquals(source, withKey(KEY_128).decrypt(withKey(KEY_128).encrypt(source)),
                    "中文文本往返后应与原文一致");
        }

        @Test
        @DisplayName("Emoji 文本往返一致")
        void testEmojiText() {
            String source = "😀😃😄😁🚀 表情符号";
            assertEquals(source, withKey(KEY_128).decrypt(withKey(KEY_128).encrypt(source)),
                    "Emoji 文本往返后应与原文一致");
        }

        @Test
        @DisplayName("恰好 16 字节的文本（一个完整分组）往返一致")
        void testExactlyOneBlock() {
            String source = "0123456789abcdef";
            AesUtil instance = withKey(KEY_128);
            String encrypted = instance.encrypt(source);
            assertEquals(32, Base64.getDecoder().decode(encrypted).length,
                    "16 字节原文 + PKCS5 填充后应为 2 个分组共 32 字节");
            assertEquals(source, instance.decrypt(encrypted), "单分组文本往返后应与原文一致");
        }

        @Test
        @DisplayName("超过单分组的文本自动填充并可正确解密")
        void testMultiBlockText() {
            String source = "0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz";
            AesUtil instance = withKey(KEY_128);
            byte[] cipherBytes = Base64.getDecoder().decode(instance.encrypt(source));
            assertEquals(0, cipherBytes.length % 16, "密文长度应为 16 的整数倍");
            assertEquals(source, instance.decrypt(instance.encrypt(source)), "多分组文本往返后应与原文一致");
        }

        @Test
        @DisplayName("超长文本（10000 字符）往返一致")
        void testVeryLongText() {
            String source = "超长文本加密测试".repeat(2000);
            assertTrue(source.length() > 10000, "测试文本长度应超过 10000 字符");
            AesUtil instance = withKey(KEY_128);
            assertEquals(source, instance.decrypt(instance.encrypt(source)),
                    "超长文本往返后应与原文一致");
        }

        @Test
        @DisplayName("相同明文使用相同密钥加密结果稳定（确定性）")
        void testDeterministicCipher() {
            String source = "确定性加密测试";
            String first = withKey(KEY_128).encrypt(source);
            String second = withKey(KEY_128).encrypt(source);
            assertEquals(first, second, "ECB 之外的确定性依赖 IV，相同 key + iv 应得到相同密文");
        }

        @Test
        @DisplayName("不同密钥加密同一明文得到不同密文")
        void testDifferentKeyDifferentCipher() {
            String source = "不同密钥应产生不同密文";
            byte[] other = "fedcba9876543210".getBytes(UTF_8);
            assertNotEquals(withKey(KEY_128).encrypt(source), withKey(other).encrypt(source),
                    "不同密钥加密同一明文不应得到相同密文");
        }
    }

    @Nested
    @DisplayName("3. 密钥设置：setKey 的两个重载")
    class SetKey {

        @Test
        @DisplayName("setKey(String) 传入合法 Base64 密钥可正常加解密")
        void testSetKeyWithBase64() {
            String source = "Base64 密钥测试";
            AesUtil instance = AesUtil.create()
                    .setKey(Base64.getEncoder().encodeToString(KEY_128));
            assertEquals(source, instance.decrypt(instance.encrypt(source)),
                    "Base64 字符串密钥应与字节数组密钥等价");
        }

        @Test
        @DisplayName("setKey(String) 传入非法 Base64 抛出 ServiceException")
        void testSetKeyWithIllegalBase64() {
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> AesUtil.create().setKey("这不是Base64!!!"),
                    "非法 Base64 密钥字符串应抛出 ServiceException");
            assertEquals("加密密钥不是合法的 Base64 字符串", exception.getMessage(), "异常信息应说明是 Base64 非法");
        }

        @Test
        @DisplayName("setKey(String) 传入 null 抛出 ServiceException")
        void testSetKeyStringWithNull() {
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> AesUtil.create().setKey((String) null),
                    "null 的 Base64 密钥字符串应抛出 ServiceException");
            assertEquals("加密密钥不能为null", exception.getMessage(), "异常信息应说明密钥不能为 null");
        }

        @Test
        @DisplayName("setKey(byte[]) 传入 null 立即抛出 ServiceException")
        void testSetKeyBytesWithNull() {
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> AesUtil.create().setKey((byte[]) null),
                    "设置 null 密钥应立即报错，而不是延后到加密时");
            assertEquals("加密密钥不能为null", exception.getMessage(), "异常信息应说明密钥不能为 null");
        }

        @Test
        @DisplayName("setKey(byte[]) 传入空数组立即抛出 ServiceException")
        void testSetKeyBytesWithEmpty() {
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> AesUtil.create().setKey(new byte[0]),
                    "空密钥应立即报错");
            assertTrue(exception.getMessage().contains("16、24 或 32 字节"),
                    "异常信息应说明合法长度，实际为：" + exception.getMessage());
        }

        @Test
        @DisplayName("setKey(String) 传入空串抛出 ServiceException")
        void testSetKeyStringWithBlank() {
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> AesUtil.create().setKey(""),
                    "空串密钥解码后是 0 长度数组，应立即报错");
            assertEquals("加密密钥不能为空字符串", exception.getMessage(), "异常信息应说明密钥不能为空串");
        }

        @Test
        @DisplayName("setIv 传入非 16 字节长度时加密抛出明确的 ServiceException")
        void testInvalidIvLength() {
            AesUtil instance = AesUtil.create()
                    .setKey(new byte[16])
                    .setIv(new byte[3]);
            ServiceException exception = assertThrows(ServiceException.class, () -> instance.encrypt("内容"),
                    "IV 长度非法应给出明确提示");
            assertTrue(exception.getMessage().contains("偏移向量长度必须为 16 字节"),
                    "异常信息应说明 IV 长度要求，实际为：" + exception.getMessage());
        }
    }

    @Nested
    @DisplayName("4. 偏移向量与填充模式")
    class IvAndPadding {

        @Test
        @DisplayName("自定义 16 字节偏移向量可正常往返")
        void testCustomIvRoundTrip() {
            String source = "自定义偏移向量测试";
            AesUtil instance = AesUtil.create().setKey(KEY_128).setIv(CUSTOM_IV);
            assertEquals(source, instance.decrypt(instance.encrypt(source)),
                    "自定义 IV 往返后应与原文一致");
        }

        @Test
        @DisplayName("使用不同偏移向量解密无法得到原文（CBC 特性）")
        void testDecryptWithWrongIv() {
            String source = "偏移向量不匹配时的解密结果";
            String encrypted = AesUtil.create().setKey(KEY_128).setIv(CUSTOM_IV).encrypt(source);
            String decrypted = AesUtil.create().setKey(KEY_128).setIv(DEFAULT_IV).decrypt(encrypted);
            assertNotEquals(source, decrypted, "IV 不一致时不应得到原文（首分组会被破坏）");
        }

        @Test
        @DisplayName("偏移向量长度不是 16 字节时抛出 ServiceException")
        void testInvalidIvLength() {
            AesUtil instance = AesUtil.create().setKey(KEY_128).setIv("12345678".getBytes(UTF_8));
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> instance.encrypt("任意内容"),
                    "IV 长度非法时加密应抛出 ServiceException");
            assertFalse(exception.getMessage() == null || exception.getMessage().isBlank(),
                    "异常信息不应为空（JDK 底层异常信息会透传）");
        }

        @Test
        @DisplayName("偏移向量为 null 时抛出 ServiceException")
        void testNullIv() {
            AesUtil instance = AesUtil.create().setKey(KEY_128).setIv(null);
            assertThrows(ServiceException.class, () -> instance.encrypt("任意内容"),
                    "IV 为 null 时加密应抛出 ServiceException");
        }

        @Test
        @DisplayName("偏移向量为空数组时抛出 ServiceException")
        void testEmptyIv() {
            AesUtil instance = AesUtil.create().setKey(KEY_128).setIv(new byte[0]);
            assertThrows(ServiceException.class, () -> instance.encrypt("任意内容"),
                    "IV 为空数组时加密应抛出 ServiceException");
        }

        @Test
        @DisplayName("setPadding 切换为 NoPadding 后，16 字节整分组的文本仍可正常往返")
        void testPaddingNoPadding() {
            String source = "0123456789abcdef";
            AesUtil instance = AesUtil.create().setKey(KEY_128).setPadding("NoPadding");
            String encrypted = instance.encrypt(source);
            assertEquals(16, Base64.getDecoder().decode(encrypted).length,
                    "NoPadding 模式下 16 字节原文的密文长度应为 16 字节");
            assertEquals(source, instance.decrypt(encrypted), "NoPadding 模式下往返后应与原文一致");
        }

        @Test
        @DisplayName("setPadding 为 NoPadding 时，非 16 倍数的明文抛出 ServiceException")
        void testPaddingNoPaddingWithInvalidLength() {
            AesUtil instance = AesUtil.create().setKey(KEY_128).setPadding("NoPadding");
            assertThrows(ServiceException.class, () -> instance.encrypt("0123456789"),
                    "NoPadding 模式下非 16 倍数的明文应抛出 ServiceException");
        }

        @Test
        @DisplayName("非法填充模式抛出 ServiceException")
        void testIllegalPadding() {
            AesUtil instance = AesUtil.create().setKey(KEY_128).setPadding("NoSuchPadding");
            assertThrows(ServiceException.class, () -> instance.encrypt("任意内容"),
                    "不存在的填充模式应抛出 ServiceException");
        }
    }

    @Nested
    @DisplayName("5. null 入参与异常分支")
    class ExceptionBranch {

        @Test
        @DisplayName("encrypt(null) 抛出 ServiceException：加密内容不能为null")
        void testEncryptNull() {
            AesUtil instance = withKey(KEY_128);
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> instance.encrypt(null),
                    "加密 null 内容应抛出 ServiceException");
            assertEquals("加密内容不能为null", exception.getMessage(), "异常信息应为：加密内容不能为null");
        }

        @Test
        @DisplayName("decrypt(null) 抛出 ServiceException：解密内容不能为null")
        void testDecryptNull() {
            AesUtil instance = withKey(KEY_128);
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> instance.decrypt(null),
                    "解密 null 内容应抛出 ServiceException");
            assertEquals("解密内容不能为null", exception.getMessage(), "异常信息应为：解密内容不能为null");
        }

        @Test
        @DisplayName("decrypt 传入非法 Base64 抛出 ServiceException")
        void testDecryptIllegalBase64() {
            AesUtil instance = withKey(KEY_128);
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> instance.decrypt("这不是Base64!!!"),
                    "非法 Base64 密文应抛出 ServiceException");
            assertFalse(exception.getMessage() == null || exception.getMessage().isBlank(),
                    "异常信息来自 JDK，不应为空");
        }

        @Test
        @DisplayName("decrypt 传入空字符串返回空字符串（JDK 对 0 长度输入不校验填充）")
        void testDecryptEmptyString() {
            AesUtil instance = withKey(KEY_128);
            assertEquals("", instance.decrypt(""),
                    "空密文解密后返回空字符串（JDK 未对 0 长度输入做填充校验）");
        }

        @Test
        @DisplayName("decrypt 传入长度非 16 倍数的 Base64 密文抛出 ServiceException")
        void testDecryptInvalidBlockLength() {
            AesUtil instance = withKey(KEY_128);
            String notCipherText = Base64.getEncoder().encodeToString(new byte[20]);
            assertThrows(ServiceException.class, () -> instance.decrypt(notCipherText),
                    "长度非 16 倍数的密文应抛出 ServiceException");
        }

        @Test
        @DisplayName("使用其他密钥解密无法得到原文（抛异常或结果不一致）")
        void testDecryptWithOtherKey() {
            String source = "使用其他密钥解密的测试内容";
            byte[] otherKey = "fedcba9876543210".getBytes(UTF_8);
            String encrypted = withKey(KEY_128).encrypt(source);
            String result = null;
            try {
                result = withKey(otherKey).decrypt(encrypted);
            } catch (ServiceException ignored) {
                // 填充校验失败属于正常分支，此处忽略
            }
            assertTrue(result == null || !source.equals(result),
                    "使用其他密钥不应得到原文（可能抛出填充异常，也可能得到乱码）");
        }
    }
}
