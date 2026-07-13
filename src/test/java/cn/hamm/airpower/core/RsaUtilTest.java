package cn.hamm.airpower.core;

import cn.hamm.airpower.core.exception.ServiceException;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>RSA 工具类单元测试</h1>
 *
 * @author Hamm.cn
 */
class RsaUtilTest {

    // ==================== 密钥生成与转换测试 ====================

    @Test
    void testGenerateKeyPair() throws Exception {
        RsaUtil rsaUtil = RsaUtil.create();
        KeyPair keyPair = rsaUtil.generateKeyPair();

        assertNotNull(keyPair);
        assertNotNull(keyPair.getPublic());
        assertNotNull(keyPair.getPrivate());
    }

    @Test
    void testGenerateKeyPairWithCustomKeySize() throws Exception {
        RsaUtil rsaUtil = RsaUtil.create();
        rsaUtil.setKeySize(1024);
        KeyPair keyPair = rsaUtil.generateKeyPair();

        assertNotNull(keyPair);
        assertNotNull(keyPair.getPublic());
        assertNotNull(keyPair.getPrivate());
    }

    @Test
    void testConvertPublicKeyToPem() throws Exception {
        RsaUtil rsaUtil = RsaUtil.create();
        KeyPair keyPair = rsaUtil.generateKeyPair();
        String pem = rsaUtil.convertPublicKeyToPem(keyPair.getPublic());

        assertNotNull(pem);
        assertTrue(pem.contains("-----BEGIN PUBLIC KEY-----"));
        assertTrue(pem.contains("-----END PUBLIC KEY-----"));
    }

    @Test
    void testConvertPrivateKeyToPem() throws Exception {
        RsaUtil rsaUtil = RsaUtil.create();
        KeyPair keyPair = rsaUtil.generateKeyPair();
        String pem = rsaUtil.convertPrivateKeyToPem(keyPair.getPrivate());

        assertNotNull(pem);
        assertTrue(pem.contains("-----BEGIN RSA PRIVATE KEY-----"));
        assertTrue(pem.contains("-----END RSA PRIVATE KEY-----"));
    }

    @Test
    void testGetPemPublicKey() throws Exception {
        RsaUtil rsaUtil = RsaUtil.create();
        KeyPair keyPair = rsaUtil.generateKeyPair();
        String pem = rsaUtil.getPemPublicKey(keyPair);

        assertNotNull(pem);
        assertTrue(pem.contains("-----BEGIN PUBLIC KEY-----"));
    }

    @Test
    void testGetPemPrivateKey() throws Exception {
        RsaUtil rsaUtil = RsaUtil.create();
        KeyPair keyPair = rsaUtil.generateKeyPair();
        String pem = rsaUtil.getPemPrivateKey(keyPair);

        assertNotNull(pem);
        assertTrue(pem.contains("-----BEGIN RSA PRIVATE KEY-----"));
    }

    // ==================== Base64 文本换行测试 ====================

    @Test
    void testWrapBase64Text() {
        RsaUtil rsaUtil = RsaUtil.create();
        String shortText = "short";
        String result = rsaUtil.wrapBase64Text(shortText);

        assertEquals("short\n", result);
    }

    @Test
    void testWrapBase64TextWithLongText() {
        RsaUtil rsaUtil = RsaUtil.create();
        String longText = "a".repeat(100);
        String result = rsaUtil.wrapBase64Text(longText);

        // 应该包含换行符
        assertTrue(result.contains("\n"));
        // 每行最多64个字符
        String[] lines = result.split("\n");
        for (String line : lines) {
            assertTrue(line.length() <= 64);
        }
    }

    @Test
    void testWrapBase64TextWithExact64Chars() {
        RsaUtil rsaUtil = RsaUtil.create();
        String text64 = "a".repeat(64);
        String result = rsaUtil.wrapBase64Text(text64);

        // 64个字符应该正好一行
        assertEquals(text64 + "\n", result);
    }

    @Test
    void testWrapBase64TextWithEmptyString() {
        RsaUtil rsaUtil = RsaUtil.create();
        String result = rsaUtil.wrapBase64Text("");
        // 空字符串直接返回空
        assertEquals("", result);
    }

    // ==================== 公钥私钥解析测试 ====================

    @Test
    void testGetPublicKey() throws Exception {
        RsaUtil rsaUtil = RsaUtil.create();
        KeyPair keyPair = rsaUtil.generateKeyPair();
        String publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

        rsaUtil.setPublicKey(publicKeyBase64);
        PublicKey publicKey = rsaUtil.getPublicKey(publicKeyBase64);

        assertNotNull(publicKey);
    }

    @Test
    void testGetPrivateKey() throws Exception {
        RsaUtil rsaUtil = RsaUtil.create();
        KeyPair keyPair = rsaUtil.generateKeyPair();
        String privateKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());

        rsaUtil.setPrivateKey(privateKeyBase64);
        PrivateKey privateKey = rsaUtil.getPrivateKey(privateKeyBase64);

        assertNotNull(privateKey);
    }

    @Test
    void testPublicKeyCache() throws Exception {
        RsaUtil rsaUtil = RsaUtil.create();
        KeyPair keyPair = rsaUtil.generateKeyPair();
        String publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

        // 第一次获取
        PublicKey publicKey1 = rsaUtil.getPublicKey(publicKeyBase64);
        // 第二次获取（应该使用缓存）
        PublicKey publicKey2 = rsaUtil.getPublicKey(publicKeyBase64);

        assertSame(publicKey1, publicKey2);
    }

    @Test
    void testPrivateKeyCache() throws Exception {
        RsaUtil rsaUtil = RsaUtil.create();
        KeyPair keyPair = rsaUtil.generateKeyPair();
        String privateKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());

        // 第一次获取
        PrivateKey privateKey1 = rsaUtil.getPrivateKey(privateKeyBase64);
        // 第二次获取（应该使用缓存）
        PrivateKey privateKey2 = rsaUtil.getPrivateKey(privateKeyBase64);

        assertSame(privateKey1, privateKey2);
    }

    // ==================== 公钥加密/私钥解密测试 ====================

    @Test
    void testPublicKeyEncryptAndPrivateKeyDecrypt() throws Exception {
        RsaUtil rsaUtil = RsaUtil.create();
        KeyPair keyPair = rsaUtil.generateKeyPair();
        String publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        String privateKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());

        rsaUtil.setPublicKey(publicKeyBase64);
        rsaUtil.setPrivateKey(privateKeyBase64);

        String originalText = "Hello, RSA!";
        String encrypted = rsaUtil.publicKeyEncrypt(originalText);
        String decrypted = rsaUtil.privateKeyDecrypt(encrypted);

        assertNotNull(encrypted);
        assertNotEquals(originalText, encrypted);
        assertEquals(originalText, decrypted);
    }

    @Test
    void testPublicKeyEncryptAndPrivateKeyDecryptWithLongText() throws Exception {
        RsaUtil rsaUtil = RsaUtil.create();
        KeyPair keyPair = rsaUtil.generateKeyPair();
        String publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        String privateKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());

        rsaUtil.setPublicKey(publicKeyBase64);
        rsaUtil.setPrivateKey(privateKeyBase64);

        // 超过一个block的文本
        String originalText = "a".repeat(300);
        String encrypted = rsaUtil.publicKeyEncrypt(originalText);
        String decrypted = rsaUtil.privateKeyDecrypt(encrypted);

        assertEquals(originalText, decrypted);
    }

    @Test
    void testPublicKeyEncryptWithEmptyString() throws Exception {
        RsaUtil rsaUtil = RsaUtil.create();
        KeyPair keyPair = rsaUtil.generateKeyPair();
        String publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        String privateKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());

        rsaUtil.setPublicKey(publicKeyBase64);
        rsaUtil.setPrivateKey(privateKeyBase64);

        String encrypted = rsaUtil.publicKeyEncrypt("");
        String decrypted = rsaUtil.privateKeyDecrypt(encrypted);

        assertEquals("", decrypted);
    }

    // ==================== 私钥加密/公钥解密测试 ====================

    @Test
    void testPrivateKeyEncryptAndPublicKeyDecrypt() throws Exception {
        RsaUtil rsaUtil = RsaUtil.create();
        KeyPair keyPair = rsaUtil.generateKeyPair();
        String publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        String privateKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());

        rsaUtil.setPublicKey(publicKeyBase64);
        rsaUtil.setPrivateKey(privateKeyBase64);

        String originalText = "Hello, RSA!";
        String encrypted = rsaUtil.privateKeyEncrypt(originalText);
        String decrypted = rsaUtil.publicKeyDecrypt(encrypted);

        assertNotNull(encrypted);
        assertNotEquals(originalText, encrypted);
        assertEquals(originalText, decrypted);
    }

    @Test
    void testPrivateKeyEncryptAndPublicKeyDecryptWithLongText() throws Exception {
        RsaUtil rsaUtil = RsaUtil.create();
        KeyPair keyPair = rsaUtil.generateKeyPair();
        String publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        String privateKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());

        rsaUtil.setPublicKey(publicKeyBase64);
        rsaUtil.setPrivateKey(privateKeyBase64);

        // 超过一个block的文本
        String originalText = "b".repeat(300);
        String encrypted = rsaUtil.privateKeyEncrypt(originalText);
        String decrypted = rsaUtil.publicKeyDecrypt(encrypted);

        assertEquals(originalText, decrypted);
    }

    // ==================== 签名和验签测试 ====================

    @Test
    void testPrivateKeySignatureAndPublicKeyVerify() throws Exception {
        RsaUtil rsaUtil = RsaUtil.create();
        KeyPair keyPair = rsaUtil.generateKeyPair();
        String publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        String privateKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());

        rsaUtil.setPublicKey(publicKeyBase64);
        rsaUtil.setPrivateKey(privateKeyBase64);

        String originalText = "Hello, RSA Signature!";
        String signature = rsaUtil.privateKeySignature(originalText);

        assertNotNull(signature);

        boolean verified = rsaUtil.publicKeyVerifySignature(originalText, signature);
        assertTrue(verified);
    }

    @Test
    void testPublicKeyVerifyWithWrongSignature() throws Exception {
        RsaUtil rsaUtil = RsaUtil.create();
        KeyPair keyPair = rsaUtil.generateKeyPair();
        String publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        String privateKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());

        rsaUtil.setPublicKey(publicKeyBase64);
        rsaUtil.setPrivateKey(privateKeyBase64);

        String originalText = "Hello, RSA Signature!";
        // 使用一个有效的Base64编码但签名内容错误的签名
        String wrongSignature = rsaUtil.privateKeySignature("different content");

        boolean verified = rsaUtil.publicKeyVerifySignature(originalText, wrongSignature);
        assertFalse(verified);
    }

    @Test
    void testPublicKeyVerifyWithWrongContent() throws Exception {
        RsaUtil rsaUtil = RsaUtil.create();
        KeyPair keyPair = rsaUtil.generateKeyPair();
        String publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        String privateKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());

        rsaUtil.setPublicKey(publicKeyBase64);
        rsaUtil.setPrivateKey(privateKeyBase64);

        String originalText = "Hello, RSA Signature!";
        String signature = rsaUtil.privateKeySignature(originalText);

        boolean verified = rsaUtil.publicKeyVerifySignature("Wrong content", signature);
        assertFalse(verified);
    }

    // ==================== 异常测试 ====================

    @Test
    void testPublicKeyEncryptWithoutPublicKey() {
        RsaUtil rsaUtil = RsaUtil.create();
        // 没有设置公钥
        assertThrows(ServiceException.class, () -> rsaUtil.publicKeyEncrypt("test"));
    }

    @Test
    void testPrivateKeyDecryptWithoutPrivateKey() {
        RsaUtil rsaUtil = RsaUtil.create();
        // 没有设置私钥
        assertThrows(ServiceException.class, () -> rsaUtil.privateKeyDecrypt("test"));
    }

    @Test
    void testPrivateKeyEncryptWithoutPrivateKey() {
        RsaUtil rsaUtil = RsaUtil.create();
        // 没有设置私钥
        assertThrows(ServiceException.class, () -> rsaUtil.privateKeyEncrypt("test"));
    }

    @Test
    void testPublicKeyDecryptWithoutPublicKey() {
        RsaUtil rsaUtil = RsaUtil.create();
        // 没有设置公钥
        assertThrows(ServiceException.class, () -> rsaUtil.publicKeyDecrypt("test"));
    }

    @Test
    void testPrivateKeySignatureWithoutPrivateKey() {
        RsaUtil rsaUtil = RsaUtil.create();
        // 没有设置私钥
        assertThrows(ServiceException.class, () -> rsaUtil.privateKeySignature("test"));
    }

    @Test
    void testPublicKeyVerifyWithoutPublicKey() {
        RsaUtil rsaUtil = RsaUtil.create();
        // 没有设置公钥
        assertThrows(ServiceException.class, () -> rsaUtil.publicKeyVerifySignature("test", "signature"));
    }

    @Test
    void testRsaDoFinalWithInvalidBlockSize() throws Exception {
        RsaUtil rsaUtil = RsaUtil.create();
        KeyPair keyPair = rsaUtil.generateKeyPair();
        String publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        String privateKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());

        rsaUtil.setPublicKey(publicKeyBase64);
        rsaUtil.setPrivateKey(privateKeyBase64);

        // 使用无效的blockSize（通过设置keySize为0）
        rsaUtil.setKeySize(0);
        assertThrows(ServiceException.class, () -> rsaUtil.publicKeyEncrypt("test"));
    }

    // ==================== 链式调用测试 ====================

    @Test
    void testFluentApi() throws Exception {
        RsaUtil rsaUtil = RsaUtil.create()
                .setKeySize(2048)
                .setCryptAlgorithm("RSA")
                .setSignAlgorithm("SHA256withRSA");

        assertNotNull(rsaUtil);
        KeyPair keyPair = rsaUtil.generateKeyPair();
        assertNotNull(keyPair);
    }

    // ==================== 自定义算法测试 ====================

    @Test
    void testCustomSignAlgorithm() throws Exception {
        RsaUtil rsaUtil = RsaUtil.create();
        rsaUtil.setSignAlgorithm("SHA1withRSA");
        KeyPair keyPair = rsaUtil.generateKeyPair();
        String publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        String privateKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());

        rsaUtil.setPublicKey(publicKeyBase64);
        rsaUtil.setPrivateKey(privateKeyBase64);

        String originalText = "Test with SHA1";
        String signature = rsaUtil.privateKeySignature(originalText);
        boolean verified = rsaUtil.publicKeyVerifySignature(originalText, signature);

        assertTrue(verified);
    }
}
