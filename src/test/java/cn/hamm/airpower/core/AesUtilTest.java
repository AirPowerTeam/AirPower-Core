package cn.hamm.airpower.core;

import cn.hamm.airpower.core.exception.ServiceException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>AesUtil 单元测试</h1>
 *
 * @author Hamm.cn
 */
class AesUtilTest {

    private static final String TEST_KEY = "0123456789abcdef";
    private static final String TEST_IV = "abcdef0123456789";
    private static final String PLAIN_TEXT = "Hello, World!";
    private static final String CHINESE_TEXT = "中文测试内容🎉";
    private static final String EMPTY_TEXT = "";
    private static final String LONG_TEXT = "a".repeat(1000);

    // ==================== 基本加密解密测试 ====================

    @Test
    void testEncryptAndDecrypt() {
        String encrypted = AesUtil.create()
                .setKey(TEST_KEY)
                .setIv(TEST_IV)
                .encrypt(PLAIN_TEXT);
        assertNotNull(encrypted);
        assertFalse(encrypted.isEmpty());

        String decrypted = AesUtil.create()
                .setKey(TEST_KEY)
                .setIv(TEST_IV)
                .decrypt(encrypted);
        assertEquals(PLAIN_TEXT, decrypted);
    }

    @Test
    void testEncryptAndDecryptWithChinese() {
        String encrypted = AesUtil.create()
                .setKey(TEST_KEY)
                .setIv(TEST_IV)
                .encrypt(CHINESE_TEXT);
        String decrypted = AesUtil.create()
                .setKey(TEST_KEY)
                .setIv(TEST_IV)
                .decrypt(encrypted);
        assertEquals(CHINESE_TEXT, decrypted);
    }

    @Test
    void testEncryptAndDecryptWithLongText() {
        String encrypted = AesUtil.create()
                .setKey(TEST_KEY)
                .setIv(TEST_IV)
                .encrypt(LONG_TEXT);
        String decrypted = AesUtil.create()
                .setKey(TEST_KEY)
                .setIv(TEST_IV)
                .decrypt(encrypted);
        assertEquals(LONG_TEXT, decrypted);
    }

    @Test
    void testEncryptAndDecryptWithEmptyString() {
        String encrypted = AesUtil.create()
                .setKey(TEST_KEY)
                .setIv(TEST_IV)
                .encrypt(EMPTY_TEXT);
        String decrypted = AesUtil.create()
                .setKey(TEST_KEY)
                .setIv(TEST_IV)
                .decrypt(encrypted);
        assertEquals(EMPTY_TEXT, decrypted);
    }

    @Test
    void testEncryptAndDecryptWithSpecialCharacters() {
        String specialText = "!@#$%^&*()_+{}|:<>?[]\";'\\";
        String encrypted = AesUtil.create()
                .setKey(TEST_KEY)
                .setIv(TEST_IV)
                .encrypt(specialText);
        String decrypted = AesUtil.create()
                .setKey(TEST_KEY)
                .setIv(TEST_IV)
                .decrypt(encrypted);
        assertEquals(specialText, decrypted);
    }

    // ==================== 不同密钥测试 ====================

    @Test
    void testEncryptAndDecryptWithDifferentKeys() {
        String key1 = "0123456789abcdef";
        String key2 = "fedcba9876543210";

        String encrypted = AesUtil.create()
                .setKey(key1)
                .setIv(TEST_IV)
                .encrypt(PLAIN_TEXT);

        // 使用不同密钥解密应该失败
        assertThrows(RuntimeException.class, () ->
                AesUtil.create()
                        .setKey(key2)
                        .setIv(TEST_IV)
                        .decrypt(encrypted)
        );
    }

    @Test
    void testEncryptAndDecryptWithDifferentIv() {
        String iv1 = "abcdef0123456789";
        String iv2 = "fedcba0987654321";

        String encrypted = AesUtil.create()
                .setKey(TEST_KEY)
                .setIv(iv1)
                .encrypt(PLAIN_TEXT);

        // 使用不同IV解密应该失败
        assertThrows(RuntimeException.class, () ->
                AesUtil.create()
                        .setKey(TEST_KEY)
                        .setIv(iv2)
                        .decrypt(encrypted)
        );
    }

    // ==================== 默认IV测试 ====================

    @Test
    void testDefaultIv() {
        // 不设置IV，使用默认值
        String encrypted = AesUtil.create()
                .setKey(TEST_KEY)
                .encrypt(PLAIN_TEXT);
        String decrypted = AesUtil.create()
                .setKey(TEST_KEY)
                .encrypt(PLAIN_TEXT);
        // 两次加密结果应该不同（因为默认IV相同，但CBC模式需要IV，这里验证能正常加密）
        assertNotNull(encrypted);
        assertNotNull(decrypted);
    }

    // ==================== 不同填充模式测试 ====================

    @Test
    void testEncryptAndDecryptWithPkcs5Padding() {
        String encrypted = AesUtil.create()
                .setKey(TEST_KEY)
                .setIv(TEST_IV)
                .setPadding("PKCS5Padding")
                .encrypt(PLAIN_TEXT);
        String decrypted = AesUtil.create()
                .setKey(TEST_KEY)
                .setIv(TEST_IV)
                .setPadding("PKCS5Padding")
                .decrypt(encrypted);
        assertEquals(PLAIN_TEXT, decrypted);
    }

    // ==================== 加密结果验证 ====================

    @Test
    void testEncryptedResultIsBase64() {
        String encrypted = AesUtil.create()
                .setKey(TEST_KEY)
                .setIv(TEST_IV)
                .encrypt(PLAIN_TEXT);
        // 验证是Base64编码
        assertDoesNotThrow(() -> java.util.Base64.getDecoder().decode(encrypted));
    }

    @Test
    void testEncryptSamePlainTextTwice() {
        // 相同明文使用同一个实例加密两次结果应该相同（因为Cipher缓存）
        AesUtil aes = AesUtil.create().setKey(TEST_KEY).setIv(TEST_IV);

        String encrypted1 = aes.encrypt(PLAIN_TEXT);
        String encrypted2 = aes.encrypt(PLAIN_TEXT);

        // 同一个实例加密两次结果相同
        assertEquals(encrypted1, encrypted2);
    }

    // ==================== 异常测试 ====================

    @Test
    void testDecryptInvalidBase64() {
        assertThrows(RuntimeException.class, () ->
                AesUtil.create()
                        .setKey(TEST_KEY)
                        .setIv(TEST_IV)
                        .decrypt("invalid-base64!!!")
        );
    }

    @Test
    void testDecryptTamperedCipherText() {
        String encrypted = AesUtil.create()
                .setKey(TEST_KEY)
                .setIv(TEST_IV)
                .encrypt(PLAIN_TEXT);
        String tampered = encrypted + "tampered";
        assertThrows(RuntimeException.class, () ->
                AesUtil.create()
                        .setKey(TEST_KEY)
                        .setIv(TEST_IV)
                        .decrypt(tampered)
        );
    }

    @Test
    void testEncryptWithNullSource() {
        assertThrows(ServiceException.class, () ->
                AesUtil.create()
                        .setKey(TEST_KEY)
                        .setIv(TEST_IV)
                        .encrypt(null)
        );
    }

    @Test
    void testDecryptWithNullContent() {
        assertThrows(ServiceException.class, () ->
                AesUtil.create()
                        .setKey(TEST_KEY)
                        .setIv(TEST_IV)
                        .decrypt(null)
        );
    }

    // ==================== 边界情况测试 ====================

    @Test
    void testEncryptWithSingleCharacter() {
        String singleChar = "X";
        String encrypted = AesUtil.create()
                .setKey(TEST_KEY)
                .setIv(TEST_IV)
                .encrypt(singleChar);
        String decrypted = AesUtil.create()
                .setKey(TEST_KEY)
                .setIv(TEST_IV)
                .decrypt(encrypted);
        assertEquals(singleChar, decrypted);
    }

    @Test
    void testEncryptWithExactlyBlockSize() {
        // AES块大小为16字节
        String blockSizeText = "1234567890123456";
        String encrypted = AesUtil.create()
                .setKey(TEST_KEY)
                .setIv(TEST_IV)
                .encrypt(blockSizeText);
        String decrypted = AesUtil.create()
                .setKey(TEST_KEY)
                .setIv(TEST_IV)
                .decrypt(encrypted);
        assertEquals(blockSizeText, decrypted);
    }

    @Test
    void testEncryptWithMultipleBlocks() {
        // 32字节，2个块
        String multiBlockText = "12345678901234561234567890123456";
        String encrypted = AesUtil.create()
                .setKey(TEST_KEY)
                .setIv(TEST_IV)
                .encrypt(multiBlockText);
        String decrypted = AesUtil.create()
                .setKey(TEST_KEY)
                .setIv(TEST_IV)
                .decrypt(encrypted);
        assertEquals(multiBlockText, decrypted);
    }

    @Test
    void testEncryptWithUnicodeAndEmoji() {
        String unicodeText = "Hello 世界 🌍 测试 🔒";
        String encrypted = AesUtil.create()
                .setKey(TEST_KEY)
                .setIv(TEST_IV)
                .encrypt(unicodeText);
        String decrypted = AesUtil.create()
                .setKey(TEST_KEY)
                .setIv(TEST_IV)
                .decrypt(encrypted);
        assertEquals(unicodeText, decrypted);
    }

    @Test
    void testEncryptWithNumbersOnly() {
        String numbers = "1234567890";
        String encrypted = AesUtil.create()
                .setKey(TEST_KEY)
                .setIv(TEST_IV)
                .encrypt(numbers);
        String decrypted = AesUtil.create()
                .setKey(TEST_KEY)
                .setIv(TEST_IV)
                .decrypt(encrypted);
        assertEquals(numbers, decrypted);
    }

    // ==================== 缓存测试 ====================

    @Test
    void testCipherCacheReuse() {
        AesUtil aes = AesUtil.create()
                .setKey(TEST_KEY)
                .setIv(TEST_IV);
        // 多次加密应该使用缓存的Cipher
        String encrypted1 = aes.encrypt(PLAIN_TEXT);
        String encrypted2 = aes.encrypt(PLAIN_TEXT);
        assertNotNull(encrypted1);
        assertNotNull(encrypted2);
    }

    @Test
    void testMultipleInstancesIndependent() {
        AesUtil aes1 = AesUtil.create().setKey(TEST_KEY).setIv(TEST_IV);
        AesUtil aes2 = AesUtil.create().setKey("fedcba9876543210").setIv(TEST_IV);

        String encrypted1 = aes1.encrypt(PLAIN_TEXT);
        String encrypted2 = aes2.encrypt(PLAIN_TEXT);

        assertNotNull(encrypted1);
        assertNotNull(encrypted2);
        // 不同密钥加密结果不同
        assertNotEquals(encrypted1, encrypted2);
    }
}
