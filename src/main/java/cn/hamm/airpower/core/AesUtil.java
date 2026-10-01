package cn.hamm.airpower.core;

import cn.hamm.airpower.core.exception.ServiceException;
import lombok.AccessLevel;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Objects;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.crypto.Cipher.DECRYPT_MODE;
import static javax.crypto.Cipher.ENCRYPT_MODE;

/**
 * <h1>AES 工具类</h1>
 *
 * @author Hamm.cn
 */
@Accessors(chain = true)
public class AesUtil {
    /**
     * 合法的 AES 密钥长度（字节）
     */
    private static final Set<Integer> VALID_KEY_LENGTHS = Set.of(16, 24, 32);

    /**
     * CBC 模式要求的 IV 长度（字节）
     */
    private static final int IV_LENGTH = 16;

    /**
     * 加密算法
     */
    @Setter(AccessLevel.NONE)
    private String algorithm = "AES";

    /**
     * 默认偏移向量
     */
    private static final byte[] DEFAULT_IV = "0000000000000000".getBytes(UTF_8);

    /**
     * 密钥
     */
    private byte[] key;

    /**
     * 偏移向量
     * <p>刻意不生成 Lombok setter：{@code setKey} 做了 {@code clone()} 防调用方
     * 事后篡改密钥，{@code setIv} 若直接存引用就会与 setKey 的保护不对称——
     * 调用方拿到 iv 数组的引用后改一个字节，IV 就被静默换掉了</p>
     */
    private byte[] iv = DEFAULT_IV.clone();

    /**
     * 工作模式
     */
    @Setter(AccessLevel.NONE)
    private String mode = "CBC";

    /**
     * 填充模式
     */
    @Setter
    private String padding = "PKCS5Padding";

    /**
     * 禁止外部实例化
     */
    @Contract(pure = true)
    private AesUtil() {

    }

    /**
     * 创建实例
     *
     * @return 新实例
     */
    @Contract(" -> new")
    public static @NotNull AesUtil create() {
        return new AesUtil();
    }

    /**
     * 设置密钥
     *
     * @param base64Key Base64 编码的密钥
     * @return this
     */
    public AesUtil setKey(String base64Key) {
        if (Objects.isNull(base64Key)) {
            throw new ServiceException("加密密钥不能为null");
        }
        if (base64Key.isBlank()) {
            throw new ServiceException("加密密钥不能为空字符串");
        }
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(base64Key);
        } catch (IllegalArgumentException e) {
            throw new ServiceException("加密密钥不是合法的 Base64 字符串");
        }
        return setKey(decoded);
    }

    /**
     * 设置密钥
     *
     * @param key 密钥
     * @return this
     * @apiNote 长度必须为 {@code 16 / 24 / 32} 字节（AES-128/192/256
     */
    public AesUtil setKey(byte[] key) {
        if (Objects.isNull(key)) {
            throw new ServiceException("加密密钥不能为null");
        }
        if (!VALID_KEY_LENGTHS.contains(key.length)) {
            throw new ServiceException("AES 密钥长度必须为 16、24 或 32 字节，当前为 " + key.length + " 字节");
        }
        this.key = key.clone();
        return this;
    }

    /**
     * 设置偏移向量
     *
     * @param iv 偏移向量
     * @return this
     * @apiNote 与 {@link #setKey(byte[])} 一样做防御性拷贝，避免调用方事后篡改数组
     */
    public AesUtil setIv(byte[] iv) {
        // 传 null 时保持 null（不回落默认值）：null 会在加密时由 IV_LENGTH 校验
        // 抛 ServiceException，那是既有契约，不该被这次改动悄悄改掉
        this.iv = Objects.isNull(iv) ? null : iv.clone();
        return this;
    }

    /**
     * 加密
     *
     * @param source 待加密的内容
     * @return {@code Base64} 编码的密文
     */
    public final String encrypt(String source) {
        if (Objects.isNull(source)) {
            throw new ServiceException("加密内容不能为null");
        }
        try {
            return Base64.getEncoder().encodeToString(getCipher(ENCRYPT_MODE)
                    .doFinal(source.getBytes(UTF_8)));
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("加密失败，" + e.getMessage());
        }
    }

    /**
     * 解密
     *
     * @param content {@code Base64} 编码的密文
     * @return 解密后的内容
     */
    @Contract("_ -> new")
    public final @NotNull String decrypt(String content) {
        if (Objects.isNull(content)) {
            throw new ServiceException("解密内容不能为null");
        }
        try {
            return new String(getCipher(DECRYPT_MODE)
                    .doFinal(Base64.getDecoder().decode(content)), UTF_8);
        } catch (ServiceException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw new ServiceException("解密内容不是合法的 Base64 字符串");
        } catch (Exception e) {
            throw new ServiceException("解密失败，" + e.getMessage());
        }
    }

    /**
     * 获取 {@code Cipher}
     *
     * @param type 模式
     * @return {@code Cipher}
     */
    private @NotNull Cipher getCipher(int type) {
        if (Objects.isNull(key)) {
            throw new ServiceException("加密密钥未设置");
        }
        if (Objects.isNull(iv)) {
            throw new ServiceException("偏移向量未设置");
        }
        if (iv.length != IV_LENGTH) {
            throw new ServiceException("偏移向量长度必须为 " + IV_LENGTH + " 字节，当前为 " + iv.length + " 字节");
        }
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, algorithm);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
            Cipher cipher = Cipher.getInstance(algorithm + "/" + mode + "/" + padding);
            cipher.init(type, secretKeySpec, ivParameterSpec);
            return cipher;
        } catch (Exception e) {
            throw new ServiceException("初始化密码器失败，" + e.getMessage());
        }
    }
}
