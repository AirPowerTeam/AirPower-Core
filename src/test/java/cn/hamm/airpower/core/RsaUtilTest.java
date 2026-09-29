package cn.hamm.airpower.core;

import cn.hamm.airpower.core.exception.ServiceException;
import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.security.KeyPair;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>RSA 加解密与签名工具类测试</h1>
 * <p>
 * 覆盖：链式工厂、密钥对生成、公私钥解析、PEM 转换、Base64 换行、
 * 公私钥互相加解密、分段加解密、签名验签、各分支异常。
 * 使用 1024 位密钥以加速测试。
 * </p>
 *
 * @author Hamm
 */
@DisplayName("RsaUtil RSA 加解密与签名工具类测试")
class RsaUtilTest {

    /**
     * 测试使用的密钥长度（越小越快）
     */
    private static final int TEST_KEY_SIZE = 1024;

    /**
     * 公钥单块最大明文长度：keySize / 8 - 11
     */
    private static final int MAX_ENCRYPT_BLOCK = TEST_KEY_SIZE / 8 - 11;

    /**
     * 密文单块长度：keySize / 8
     */
    private static final int CIPHER_BLOCK = TEST_KEY_SIZE / 8;

    /**
     * 全局复用的密钥对（只生成一次，避免重复的密钥生成开销）
     */
    private static KeyPair keyPair;

    /**
     * 公钥的 Base64 字符串（X.509 编码）
     */
    private static String publicKeyBase64;

    /**
     * 私钥的 Base64 字符串（PKCS#8 编码）
     */
    private static String privateKeyBase64;

    /**
     * 待测实例，每个用例独立创建
     */
    private RsaUtil rsaUtil;

    /**
     * 类级初始化：生成一次 1024 位密钥对供所有用例复用
     *
     * @throws Exception 异常
     */
    @BeforeAll
    static void initKeyPair() throws Exception {
        keyPair = RsaUtil.create().setKeySize(TEST_KEY_SIZE).generateKeyPair();
        publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        privateKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
    }

    /**
     * 生成指定长度的确定性 ASCII 文本
     *
     * @param length 长度
     * @return 文本
     */
    private static String ascii(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append((char) ('a' + (i % 26)));
        }
        return builder.toString();
    }

    /**
     * 读取私有字段的值
     *
     * @param target    目标对象
     * @param fieldName 字段名
     * @return 字段值
     * @throws Exception 反射异常
     */
    private static Object readField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    /**
     * 去除 PEM 的头尾与换行，得到纯 Base64 内容
     *
     * <p>公钥头尾为 {@code PUBLIC KEY}，私钥头尾为 PKCS#8 的 {@code PRIVATE KEY}，
     * 两者均以 {@code -----} 包裹，顺序替换不影响结果。</p>
     *
     * @param pem PEM 文本
     * @return 纯 Base64 文本
     */
    private static String stripPem(String pem) {
        return pem.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("\n", "");
    }

    /**
     * 断言字节数组内容一致
     *
     * @param expected 期望内容
     * @param actual   实际内容
     * @param message  失败消息
     */
    private static void assertArrayEqualsWithMessage(byte[] expected, byte[] actual, String message) {
        org.junit.jupiter.api.Assertions.assertArrayEquals(expected, actual, message);
    }

    /**
     * 每个用例前创建一个已配置好密钥的干净实例
     */
    @BeforeEach
    void setUp() {
        rsaUtil = RsaUtil.create()
                .setKeySize(TEST_KEY_SIZE)
                .setPublicKey(publicKeyBase64)
                .setPrivateKey(privateKeyBase64);
    }

    @Nested
    @DisplayName("1. 实例创建与链式配置")
    class CreateAndChain {

        @Test
        @DisplayName("create() 每次返回全新的实例")
        void testCreateReturnsNewInstance() {
            assertNotSame(RsaUtil.create(), RsaUtil.create(), "create() 每次都应返回一个新的实例");
        }

        @Test
        @DisplayName("setKeySize / setPublicKey / setPrivateKey 返回 this")
        void testChainReturnSelf() {
            RsaUtil instance = RsaUtil.create();
            assertSame(instance, instance.setKeySize(TEST_KEY_SIZE), "setKeySize 应返回 this");
            assertSame(instance, instance.setPublicKey(publicKeyBase64), "setPublicKey 应返回 this");
            assertSame(instance, instance.setPrivateKey(privateKeyBase64), "setPrivateKey 应返回 this");
        }

        @Test
        @DisplayName("默认配置：keySize=2048、cryptAlgorithm=RSA、signAlgorithm=SHA256withRSA")
        void testDefaultConfiguration() throws Exception {
            RsaUtil instance = RsaUtil.create();
            assertAll("默认配置应符合文档描述",
                    () -> assertEquals(2048, readField(instance, "keySize"), "默认密钥长度应为 2048"),
                    () -> assertEquals("RSA", readField(instance, "cryptAlgorithm"), "默认加密算法应为 RSA"),
                    () -> assertEquals("SHA256withRSA", readField(instance, "signAlgorithm"), "默认签名算法应为 SHA256withRSA"));
        }

        @Test
        @DisplayName("setKeySize 生效，生成对应长度的密钥对")
        void testKeySizeApplied() throws Exception {
            KeyPair generated = RsaUtil.create().setKeySize(TEST_KEY_SIZE).generateKeyPair();
            assertAll("setKeySize 应作用于 generateKeyPair",
                    () -> assertEquals(TEST_KEY_SIZE,
                            ((java.security.interfaces.RSAPublicKey) generated.getPublic()).getModulus().bitLength(),
                            "生成的公钥长度应与配置一致"),
                    () -> assertEquals(TEST_KEY_SIZE,
                            ((java.security.interfaces.RSAPrivateKey) generated.getPrivate()).getModulus().bitLength(),
                            "生成的私钥长度应与配置一致"));
        }

        @Test
        @DisplayName("setCryptAlgorithm 为非法算法时生成密钥对失败")
        void testIllegalCryptAlgorithm() {
            RsaUtil instance = RsaUtil.create().setCryptAlgorithm("NotAnAlgorithm");
            assertThrows(java.security.NoSuchAlgorithmException.class, instance::generateKeyPair,
                    "非法算法名应抛出 NoSuchAlgorithmException");
        }

        @Test
        @DisplayName("KeyFactory 缓存随 cryptAlgorithm 变化失效")
        void testKeyFactoryInvalidatedOnAlgorithmChange() throws Exception {
            RsaUtil instance = RsaUtil.create().setPublicKey(publicKeyBase64);
            assertNotNull(instance.getPublicKey(publicKeyBase64), "首次解析公钥应成功");

            instance.setCryptAlgorithm("NotAnAlgorithm");
            // 原实现缓存不失效，仍复用 RSA 的 KeyFactory，配置变更形同虚设
            assertThrows(java.security.NoSuchAlgorithmException.class,
                    () -> instance.getPublicKey(publicKeyBase64),
                    "切换 cryptAlgorithm 后缓存应失效，按新算法重新创建 KeyFactory 并报错");
        }

        @Test
        @DisplayName("KeyFactory 在算法未变时复用缓存，不重复创建")
        void testKeyFactoryReusedWhenAlgorithmUnchanged() throws Exception {
            RsaUtil instance = RsaUtil.create().setPublicKey(publicKeyBase64);
            assertNotNull(instance.getPublicKey(publicKeyBase64), "首次解析应成功");
            assertNotNull(instance.getPublicKey(publicKeyBase64), "算法未变时应命中缓存并继续成功");
        }
    }

    @Nested
    @DisplayName("2. 密钥对生成与解析")
    class KeyGenerateAndParse {

        @Test
        @DisplayName("generateKeyPair 返回 RSA 密钥对")
        void testGenerateKeyPair() throws Exception {
            KeyPair generated = RsaUtil.create().setKeySize(TEST_KEY_SIZE).generateKeyPair();
            assertAll("生成的密钥对应当有效",
                    () -> assertNotNull(generated.getPublic(), "公钥不应为 null"),
                    () -> assertNotNull(generated.getPrivate(), "私钥不应为 null"),
                    () -> assertEquals("RSA", generated.getPublic().getAlgorithm(), "公钥算法应为 RSA"),
                    () -> assertEquals("RSA", generated.getPrivate().getAlgorithm(), "私钥算法应为 RSA"));
        }

        @Test
        @DisplayName("getPublicKey 解析出的公钥与密钥对一致")
        void testGetPublicKey() throws Exception {
            assertArrayEqualsWithMessage(keyPair.getPublic().getEncoded(),
                    rsaUtil.getPublicKey(publicKeyBase64).getEncoded(),
                    "解析出的公钥应与原始公钥一致");
        }

        @Test
        @DisplayName("getPrivateKey 解析出的私钥与密钥对一致")
        void testGetPrivateKey() throws Exception {
            assertArrayEqualsWithMessage(keyPair.getPrivate().getEncoded(),
                    rsaUtil.getPrivateKey(privateKeyBase64).getEncoded(),
                    "解析出的私钥应与原始私钥一致");
        }

        @Test
        @DisplayName("getPublicKey(null) 抛出 ServiceException：RSA 公钥未设置")
        void testGetPublicKeyNull() {
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> RsaUtil.create().getPublicKey(null),
                    "公钥为 null 时应抛出 ServiceException");
            assertEquals("RSA 公钥未设置", exception.getMessage(), "异常信息应为：RSA 公钥未设置");
        }

        @Test
        @DisplayName("getPrivateKey(null) 抛出 ServiceException：RSA 私钥未设置")
        void testGetPrivateKeyNull() {
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> RsaUtil.create().getPrivateKey(null),
                    "私钥为 null 时应抛出 ServiceException");
            assertEquals("RSA 私钥未设置", exception.getMessage(), "异常信息应为：RSA 私钥未设置");
        }

        @Test
        @DisplayName("getPublicKey 传入非法 Base64 抛出 IllegalArgumentException")
        void testGetPublicKeyIllegalBase64() {
            assertThrows(IllegalArgumentException.class,
                    () -> RsaUtil.create().getPublicKey("这不是Base64!!!"),
                    "非法 Base64 公钥应抛出 IllegalArgumentException");
        }

        @Test
        @DisplayName("getPublicKey 传入合法 Base64 但非密钥内容抛出 InvalidKeySpecException")
        void testGetPublicKeyInvalidContent() {
            String notAKey = Base64.getEncoder().encodeToString("我只是一个普通字符串".getBytes(UTF_8));
            assertThrows(InvalidKeySpecException.class,
                    () -> RsaUtil.create().getPublicKey(notAKey),
                    "非密钥内容应抛出 InvalidKeySpecException");
        }

        @Test
        @DisplayName("getPrivateKey 传入合法 Base64 但非密钥内容抛出 InvalidKeySpecException")
        void testGetPrivateKeyInvalidContent() {
            String notAKey = Base64.getEncoder().encodeToString("我只是一个普通字符串".getBytes(UTF_8));
            assertThrows(InvalidKeySpecException.class,
                    () -> RsaUtil.create().getPrivateKey(notAKey),
                    "非密钥内容应抛出 InvalidKeySpecException");
        }
    }

    @Nested
    @DisplayName("3. PEM 格式转换")
    class Pem {

        @Test
        @DisplayName("getPemPublicKey 头尾正确且内容为公钥的 Base64")
        void testGetPemPublicKey() {
            String pem = rsaUtil.getPemPublicKey(keyPair);
            assertAll("公钥 PEM 结构应当正确",
                    () -> assertTrue(pem.startsWith("-----BEGIN PUBLIC KEY-----\n"), "应以公钥 BEGIN 头开始"),
                    () -> assertTrue(pem.endsWith("\n-----END PUBLIC KEY-----"), "应以公钥 END 尾结束"),
                    () -> assertEquals(publicKeyBase64, stripPem(pem), "去除头尾后的内容应等于公钥的 Base64"));
        }

        @Test
        @DisplayName("getPemPrivateKey 头尾为 PKCS#8 的 PRIVATE KEY 且内容为私钥的 Base64")
        void testGetPemPrivateKey() {
            String pem = rsaUtil.getPemPrivateKey(keyPair);
            assertAll("私钥 PEM 结构应当正确",
                    () -> assertTrue(pem.startsWith("-----BEGIN PRIVATE KEY-----\n"), "应以 PKCS#8 私钥 BEGIN 头开始"),
                    () -> assertTrue(pem.endsWith("\n-----END PRIVATE KEY-----"), "应以 PKCS#8 私钥 END 尾结束"),
                    () -> assertFalse(pem.contains("RSA PRIVATE KEY"),
                            "头尾应与内容一致，不应再使用 PKCS#1 的 RSA PRIVATE KEY 标注"),
                    () -> assertEquals(privateKeyBase64, stripPem(pem), "去除头尾后的内容应等于私钥的 Base64"));
        }

        @Test
        @DisplayName("convertPublicKeyToPem 与 getPemPublicKey 结果一致")
        void testConvertPublicKeyToPem() {
            assertEquals(rsaUtil.getPemPublicKey(keyPair), rsaUtil.convertPublicKeyToPem(keyPair.getPublic()),
                    "两个方法应产出相同的公钥 PEM");
        }

        @Test
        @DisplayName("convertPrivateKeyToPem 与 getPemPrivateKey 结果一致")
        void testConvertPrivateKeyToPem() {
            assertEquals(rsaUtil.getPemPrivateKey(keyPair), rsaUtil.convertPrivateKeyToPem(keyPair.getPrivate()),
                    "两个方法应产出相同的私钥 PEM");
        }

        @Test
        @DisplayName("PEM 文本不能直接 Base64 解码，但可被 getPublicKey 直接解析")
        void testPemIsAcceptedByGetPublicKey() throws Exception {
            String pem = rsaUtil.getPemPublicKey(keyPair);
            assertTrue(pem.contains("\n"), "PEM 内容应包含换行");
            assertThrows(IllegalArgumentException.class, () -> Base64.getDecoder().decode(pem),
                    "原始 PEM 仍不能直接 Base64 解码");
            // 修复点：getPublicKey/getPrivateKey 自行剥离头尾与换行，
            // "生成密钥对 -> 拿 PEM -> 设置回去" 这条链路不再是断的
            assertArrayEqualsWithMessage(keyPair.getPublic().getEncoded(),
                    RsaUtil.create().getPublicKey(pem).getEncoded(),
                    "getPublicKey 应能直接解析带 PEM 头尾与换行的文本");
        }

        @Test
        @DisplayName("PEM 私钥可直接回填使用，无需调用方自行剥离")
        void testPrivateKeyPemRoundTrip() {
            String pem = rsaUtil.getPemPrivateKey(keyPair);
            RsaUtil instance = RsaUtil.create().setPrivateKey(pem).setPublicKey(rsaUtil.getPemPublicKey(keyPair));
            String encrypted = assertDoesNotThrow(() -> instance.privateKeyEncrypt("中文内容"),
                    "PEM 私钥应能直接用于加密");
            assertEquals("中文内容", instance.publicKeyDecrypt(encrypted), "私钥加密后应能用公钥解密还原");
        }

        @Test
        @DisplayName("PEM 公钥可直接回填使用，无需调用方自行剥离")
        void testPublicKeyPemRoundTrip() {
            String pem = rsaUtil.getPemPublicKey(keyPair);
            RsaUtil instance = RsaUtil.create().setPublicKey(pem).setPrivateKey(rsaUtil.getPemPrivateKey(keyPair));
            String encrypted = assertDoesNotThrow(() -> instance.publicKeyEncrypt("中文内容"),
                    "PEM 公钥应能直接用于加密");
            assertEquals("中文内容", instance.privateKeyDecrypt(encrypted), "公钥加密后应能用私钥解密还原");
        }

        @Test
        @DisplayName("按 PKCS#8 头尾剥离后的私钥内容可重新解析为密钥")
        void testStripPemCanBeParsed() throws Exception {
            String pem = rsaUtil.getPemPrivateKey(keyPair);
            String content = stripPem(pem);
            assertFalse(content.contains("-"), "按 PKCS#8 头尾完整剥离后不应残留分隔符 -----");
            assertArrayEqualsWithMessage(keyPair.getPrivate().getEncoded(),
                    RsaUtil.create().getPrivateKey(content).getEncoded(),
                    "剥离后的私钥内容应能重新解析，说明 PEM 头尾与 PKCS#8 内容完全一致");
        }

        @Test
        @DisplayName("convertPrivateKeyToPem 的头尾同样是 PKCS#8 的 PRIVATE KEY")
        void testConvertPrivateKeyToPemHeader() {
            String pem = rsaUtil.convertPrivateKeyToPem(keyPair.getPrivate());
            assertAll("转换私钥的 PEM 头尾应与内容一致",
                    () -> assertTrue(pem.startsWith("-----BEGIN PRIVATE KEY-----\n"), "应以 PKCS#8 私钥 BEGIN 头开始"),
                    () -> assertTrue(pem.endsWith("\n-----END PRIVATE KEY-----"), "应以 PKCS#8 私钥 END 尾结束"),
                    () -> assertFalse(pem.contains("RSA PRIVATE KEY"), "不应再使用 PKCS#1 的 RSA PRIVATE KEY 标注"));
        }

        @Test
        @DisplayName("convertPublicKeyToPem(null) 抛出 NullPointerException")
        void testConvertPublicKeyToPemNull() {
            assertThrows(NullPointerException.class, () -> rsaUtil.convertPublicKeyToPem(null),
                    "公钥为 null 时应抛出 NullPointerException");
        }

        @Test
        @DisplayName("getPemPublicKey(null) 抛出 NullPointerException")
        void testGetPemPublicKeyNull() {
            assertThrows(NullPointerException.class, () -> rsaUtil.getPemPublicKey(null),
                    "密钥对为 null 时应抛出 NullPointerException");
        }

        @Test
        @DisplayName("getPemPrivateKey(null) 抛出 NullPointerException")
        void testGetPemPrivateKeyNull() {
            assertThrows(NullPointerException.class, () -> rsaUtil.getPemPrivateKey(null),
                    "密钥对为 null 时应抛出 NullPointerException");
        }
    }

    @Nested
    @DisplayName("4. wrapBase64Text 换行")
    class WrapBase64Text {

        @Test
        @DisplayName("空字符串返回空字符串")
        void testEmptyText() {
            assertEquals("", rsaUtil.wrapBase64Text(""), "空字符串换行后应仍为空字符串");
        }

        @Test
        @DisplayName("不足 64 字符时保持一行并以换行结尾")
        void testLessThanSixtyFour() {
            String text = "abc";
            String wrapped = rsaUtil.wrapBase64Text(text);
            assertEquals("abc\n", wrapped, "不足 64 字符应输出为一行且以换行结尾");
        }

        @Test
        @DisplayName("恰好 64 字符时输出为一行 64 字符")
        void testExactlySixtyFour() {
            String text = ascii(64);
            String wrapped = rsaUtil.wrapBase64Text(text);
            assertEquals(text + "\n", wrapped, "恰好 64 字符应输出为一行 64 字符并以换行结尾");
        }

        @Test
        @DisplayName("65 字符时拆分为 64 + 1 两行")
        void testSixtyFive() {
            String text = ascii(65);
            String[] lines = rsaUtil.wrapBase64Text(text).split("\n");
            assertAll("65 字符应拆分为两行",
                    () -> assertEquals(2, lines.length, "应拆分为两行"),
                    () -> assertEquals(64, lines[0].length(), "第一行应为 64 字符"),
                    () -> assertEquals(1, lines[1].length(), "第二行应为 1 字符"),
                    () -> assertTrue(rsaUtil.wrapBase64Text(text).endsWith("\n"), "换行结果应以换行结尾"));
        }

        @Test
        @DisplayName("128 字符时拆分为两行 64 字符")
        void testOneHundredTwentyEight() {
            String wrapped = rsaUtil.wrapBase64Text(ascii(128));
            String[] lines = wrapped.split("\n");
            assertAll("128 字符应拆分为两行 64 字符",
                    () -> assertEquals(2, lines.length, "应拆分为两行"),
                    () -> assertEquals(64, lines[0].length(), "第一行应为 64 字符"),
                    () -> assertEquals(64, lines[1].length(), "第二行应为 64 字符"));
        }

        @Test
        @DisplayName("换行后再去掉换行符应还原原文")
        void testRoundTripWithoutNewline() {
            String text = ascii(500);
            String wrapped = rsaUtil.wrapBase64Text(text);
            assertAll("换行不应丢失任何字符",
                    () -> assertEquals(text, wrapped.replace("\n", ""), "去掉换行符后应等于原文"),
                    () -> assertTrue(wrapped.endsWith("\n"), "换行结果应以换行结尾"),
                    () -> {
                        for (String line : wrapped.split("\n")) {
                            assertTrue(line.length() <= 64, "每行长度不应超过 64 字符，实际：" + line.length());
                        }
                    });
        }
    }

    @Nested
    @DisplayName("5. 公钥加密 / 私钥解密")
    class PublicEncryptPrivateDecrypt {

        @Test
        @DisplayName("空字符串可正常往返（分段循环不执行）")
        void testEmptyContent() {
            String encrypted = rsaUtil.publicKeyEncrypt("");
            assertEquals("", encrypted, "空字符串加密结果应为空字符串");
            assertEquals("", rsaUtil.privateKeyDecrypt(encrypted), "空字符串解密结果应为空字符串");
        }

        @Test
        @DisplayName("短文本往返一致")
        void testShortContent() {
            String source = "你好";
            assertEquals(source, rsaUtil.privateKeyDecrypt(rsaUtil.publicKeyEncrypt(source)),
                    "短文本往返后应与原文一致");
        }

        @Test
        @DisplayName("单块边界：116 / 117 / 118 字符往返一致")
        void testBlockBoundary() {
            int[] lengths = {MAX_ENCRYPT_BLOCK - 1, MAX_ENCRYPT_BLOCK, MAX_ENCRYPT_BLOCK + 1};
            for (int length : lengths) {
                String source = ascii(length);
                assertEquals(source, rsaUtil.privateKeyDecrypt(rsaUtil.publicKeyEncrypt(source)),
                        "长度为 " + length + " 字符的文本往返后应与原文一致");
            }
        }

        @Test
        @DisplayName("多段文本往返一致，密文长度为分组的整数倍")
        void testMultiBlockContent() {
            String source = ascii(MAX_ENCRYPT_BLOCK * 3 + 5);
            String encrypted = rsaUtil.publicKeyEncrypt(source);
            byte[] cipherBytes = Base64.getDecoder().decode(encrypted);
            assertAll("多段加解密应正常工作",
                    () -> assertEquals(0, cipherBytes.length % CIPHER_BLOCK, "密文长度应为 " + CIPHER_BLOCK + " 的整数倍"),
                    () -> assertEquals(source, rsaUtil.privateKeyDecrypt(encrypted), "多段文本往返后应与原文一致"));
        }

        @Test
        @DisplayName("超长文本（5000 字符）分段往返一致")
        void testVeryLongContent() {
            String source = ascii(5000);
            assertEquals(source, rsaUtil.privateKeyDecrypt(rsaUtil.publicKeyEncrypt(source)),
                    "超长文本分段往返后应与原文一致");
        }

        @Test
        @DisplayName("密文长度符合预期：单块 172 字符、两块 344 字符")
        void testCipherLength() {
            assertEquals(172, rsaUtil.publicKeyEncrypt(ascii(MAX_ENCRYPT_BLOCK)).length(),
                    "单块密文的 Base64 长度应为 172");
            assertEquals(344, rsaUtil.publicKeyEncrypt(ascii(MAX_ENCRYPT_BLOCK * 2)).length(),
                    "两块密文的 Base64 长度应为 344");
        }

        @Test
        @DisplayName("privateKeyDecrypt 传入长度非分组整数倍的密文抛出 ServiceException")
        void testDecryptInvalidCipher() {
            String notCipher = Base64.getEncoder().encodeToString(new byte[100]);
            assertThrows(ServiceException.class, () -> rsaUtil.privateKeyDecrypt(notCipher),
                    "密文长度非 128 整数倍时解密应抛出 ServiceException");
        }

        @Test
        @DisplayName("privateKeyDecrypt(null) 抛出 ServiceException")
        void testDecryptNull() {
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> rsaUtil.privateKeyDecrypt(null),
                    "密文为 null 时应抛出 ServiceException");
            assertTrue(exception.getMessage().startsWith("RSA 私钥解密失败，"),
                    "异常信息应以 RSA 私钥解密失败， 开头，实际为：" + exception.getMessage());
        }

        @Test
        @DisplayName("publicKeyEncrypt(null) 抛出 ServiceException")
        void testEncryptNull() {
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> rsaUtil.publicKeyEncrypt(null),
                    "明文为 null 时应抛出 ServiceException");
            assertTrue(exception.getMessage().startsWith("RSA 公钥加密失败，"),
                    "异常信息应以 RSA 公钥加密失败， 开头，实际为：" + exception.getMessage());
        }
    }

    @Nested
    @DisplayName("6. 私钥加密 / 公钥解密")
    class PrivateEncryptPublicDecrypt {

        @Test
        @DisplayName("空字符串可正常往返")
        void testEmptyContent() {
            String encrypted = rsaUtil.privateKeyEncrypt("");
            assertEquals("", encrypted, "空字符串加密结果应为空字符串");
            assertEquals("", rsaUtil.publicKeyDecrypt(encrypted), "空字符串解密结果应为空字符串");
        }

        @Test
        @DisplayName("短文本往返一致")
        void testShortContent() {
            String source = "AirPower";
            assertEquals(source, rsaUtil.publicKeyDecrypt(rsaUtil.privateKeyEncrypt(source)),
                    "短文本往返后应与原文一致");
        }

        @Test
        @DisplayName("单块边界与多段文本往返一致")
        void testBlockBoundary() {
            for (int length : new int[]{MAX_ENCRYPT_BLOCK, MAX_ENCRYPT_BLOCK + 1, MAX_ENCRYPT_BLOCK * 4}) {
                String source = ascii(length);
                assertEquals(source, rsaUtil.publicKeyDecrypt(rsaUtil.privateKeyEncrypt(source)),
                        "长度为 " + length + " 字符的文本往返后应与原文一致");
            }
        }

        @Test
        @DisplayName("publicKeyDecrypt(null) 抛出 ServiceException")
        void testDecryptNull() {
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> rsaUtil.publicKeyDecrypt(null),
                    "密文为 null 时应抛出 ServiceException");
            assertTrue(exception.getMessage().startsWith("RSA 公钥解密失败，"),
                    "异常信息应以 RSA 公钥解密失败， 开头，实际为：" + exception.getMessage());
        }

        @Test
        @DisplayName("privateKeyEncrypt(null) 抛出 ServiceException")
        void testEncryptNull() {
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> rsaUtil.privateKeyEncrypt(null),
                    "明文为 null 时应抛出 ServiceException");
            assertTrue(exception.getMessage().startsWith("RSA 私钥加密失败，"),
                    "异常信息应以 RSA 私钥加密失败， 开头，实际为：" + exception.getMessage());
        }
    }

    @Nested
    @DisplayName("7. 私钥签名 / 公钥验签")
    class Signature {

        @Test
        @DisplayName("正确签名验签通过")
        void testSignAndVerify() {
            String source = "待签名的内容";
            String signature = rsaUtil.privateKeySignature(source);
            assertAll("签名与验签应正常工作",
                    () -> assertFalse(signature.isEmpty(), "签名不应为空"),
                    () -> assertTrue(rsaUtil.publicKeyVerifySignature(source, signature), "正确的签名应验签通过"));
        }

        @Test
        @DisplayName("中文内容签名验签通过（内部使用 UTF-8）")
        void testSignAndVerifyChinese() {
            String source = "中文签名内容，测试通过！";
            assertTrue(rsaUtil.publicKeyVerifySignature(source, rsaUtil.privateKeySignature(source)),
                    "中文内容的签名应验签通过");
        }

        @Test
        @DisplayName("超长内容签名验签通过")
        void testSignAndVerifyLongContent() {
            String source = ascii(10000);
            assertTrue(rsaUtil.publicKeyVerifySignature(source, rsaUtil.privateKeySignature(source)),
                    "超长内容的签名应验签通过");
        }

        @Test
        @DisplayName("篡改内容后验签失败")
        void testVerifyTamperedContent() {
            String source = "原始内容";
            String signature = rsaUtil.privateKeySignature(source);
            assertFalse(rsaUtil.publicKeyVerifySignature(source + " 被篡改", signature),
                    "内容被篡改后验签应返回 false");
        }

        @Test
        @DisplayName("篡改签名串后验签失败")
        void testVerifyTamperedSignature() {
            String source = "原始内容";
            byte[] signatureBytes = Base64.getDecoder().decode(rsaUtil.privateKeySignature(source));
            signatureBytes[0] ^= 0x01;
            String tampered = Base64.getEncoder().encodeToString(signatureBytes);
            assertFalse(rsaUtil.publicKeyVerifySignature(source, tampered),
                    "签名被篡改后验签应返回 false");
        }

        @Test
        @DisplayName("其他密钥对产生的签名验签失败")
        void testVerifyWithOtherKeyPair() throws Exception {
            KeyPair otherKeyPair = RsaUtil.create().setKeySize(TEST_KEY_SIZE).generateKeyPair();
            String otherPrivateKey = Base64.getEncoder().encodeToString(otherKeyPair.getPrivate().getEncoded());
            String source = "跨密钥签名";
            String signature = RsaUtil.create().setPrivateKey(otherPrivateKey).privateKeySignature(source);
            assertFalse(rsaUtil.publicKeyVerifySignature(source, signature),
                    "其他密钥产生的签名应验签失败");
        }

        @Test
        @DisplayName("签名串不是合法 Base64 时抛出 ServiceException")
        void testVerifyIllegalSignature() {
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> rsaUtil.publicKeyVerifySignature("内容", "这不是Base64!!!"),
                    "非法 Base64 签名应抛出 ServiceException");
            assertTrue(exception.getMessage().startsWith("RSA 公钥验签失败，"),
                    "异常信息应以 RSA 公钥验签失败， 开头，实际为：" + exception.getMessage());
        }

        @Test
        @DisplayName("setSignAlgorithm 切换为 SHA512withRSA 后仍可验签通过")
        void testCustomSignAlgorithm() {
            RsaUtil instance = RsaUtil.create()
                    .setKeySize(TEST_KEY_SIZE)
                    .setPublicKey(publicKeyBase64)
                    .setPrivateKey(privateKeyBase64)
                    .setSignAlgorithm("SHA512withRSA");
            String source = "自定义签名算法";
            assertTrue(instance.publicKeyVerifySignature(source, instance.privateKeySignature(source)),
                    "切换签名算法后签名验签应正常工作");
        }

        @Test
        @DisplayName("签名算法切换后新旧签名互不兼容")
        void testSignatureBetweenAlgorithms() {
            String source = "签名算法不兼容";
            String sha256Signature = rsaUtil.privateKeySignature(source);
            RsaUtil sha512 = RsaUtil.create()
                    .setKeySize(TEST_KEY_SIZE)
                    .setPublicKey(publicKeyBase64)
                    .setPrivateKey(privateKeyBase64)
                    .setSignAlgorithm("SHA512withRSA");
            assertFalse(sha512.publicKeyVerifySignature(source, sha256Signature),
                    "不同签名算法的签名不应通过验签");
        }

        @Test
        @DisplayName("privateKeySignature(null) 抛出 ServiceException")
        void testSignNull() {
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> rsaUtil.privateKeySignature(null),
                    "签名为 null 的内容时抛出 ServiceException");
            assertTrue(exception.getMessage().startsWith("RSA 私钥签名失败，"),
                    "异常信息应以 RSA 私钥签名失败， 开头，实际为：" + exception.getMessage());
        }

        @Test
        @DisplayName("publicKeyVerifySignature(null, ...) 抛出 ServiceException")
        void testVerifyNullContent() {
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> rsaUtil.publicKeyVerifySignature(null, rsaUtil.privateKeySignature("内容")),
                    "验签内容为 null 时抛出 ServiceException");
            assertTrue(exception.getMessage().startsWith("RSA 公钥验签失败，"),
                    "异常信息应以 RSA 公钥验签失败， 开头，实际为：" + exception.getMessage());
        }
    }

    @Nested
    @DisplayName("8. 未设置密钥时的异常分支")
    class WithoutKey {

        @Test
        @DisplayName("publicKeyEncrypt 未设置公钥")
        void testPublicKeyEncrypt() {
            RsaUtil instance = RsaUtil.create().setKeySize(TEST_KEY_SIZE);
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> instance.publicKeyEncrypt("内容"),
                    "未设置公钥时加密应抛出 ServiceException");
            assertEquals("RSA 公钥加密失败，RSA 公钥未设置", exception.getMessage(),
                    "异常信息应为：RSA 公钥加密失败，RSA 公钥未设置");
        }

        @Test
        @DisplayName("publicKeyDecrypt 未设置公钥")
        void testPublicKeyDecrypt() {
            RsaUtil instance = RsaUtil.create().setKeySize(TEST_KEY_SIZE);
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> instance.publicKeyDecrypt("内容"),
                    "未设置公钥时解密应抛出 ServiceException");
            assertEquals("RSA 公钥解密失败，RSA 公钥未设置", exception.getMessage(),
                    "异常信息应为：RSA 公钥解密失败，RSA 公钥未设置");
        }

        @Test
        @DisplayName("privateKeyEncrypt 未设置私钥")
        void testPrivateKeyEncrypt() {
            RsaUtil instance = RsaUtil.create().setKeySize(TEST_KEY_SIZE);
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> instance.privateKeyEncrypt("内容"),
                    "未设置私钥时加密应抛出 ServiceException");
            assertEquals("RSA 私钥加密失败，RSA 私钥未设置", exception.getMessage(),
                    "异常信息应为：RSA 私钥加密失败，RSA 私钥未设置");
        }

        @Test
        @DisplayName("privateKeyDecrypt 未设置私钥")
        void testPrivateKeyDecrypt() {
            RsaUtil instance = RsaUtil.create().setKeySize(TEST_KEY_SIZE);
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> instance.privateKeyDecrypt("内容"),
                    "未设置私钥时解密应抛出 ServiceException");
            assertEquals("RSA 私钥解密失败，RSA 私钥未设置", exception.getMessage(),
                    "异常信息应为：RSA 私钥解密失败，RSA 私钥未设置");
        }

        @Test
        @DisplayName("privateKeySignature 未设置私钥")
        void testPrivateKeySignature() {
            RsaUtil instance = RsaUtil.create().setKeySize(TEST_KEY_SIZE);
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> instance.privateKeySignature("内容"),
                    "未设置私钥时签名应抛出 ServiceException");
            assertEquals("RSA 私钥签名失败，RSA 私钥未设置", exception.getMessage(),
                    "异常信息应为：RSA 私钥签名失败，RSA 私钥未设置");
        }

        @Test
        @DisplayName("publicKeyVerifySignature 未设置公钥")
        void testPublicKeyVerifySignature() {
            RsaUtil instance = RsaUtil.create().setKeySize(TEST_KEY_SIZE);
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> instance.publicKeyVerifySignature("内容", "签名"),
                    "未设置公钥时验签应抛出 ServiceException");
            assertEquals("RSA 公钥验签失败，RSA 公钥未设置", exception.getMessage(),
                    "异常信息应为：RSA 公钥验签失败，RSA 公钥未设置");
        }
    }
}
