package xyz.license.validator.utils;

import lombok.experimental.UtilityClass;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AESUtil
 *
 * @author chaoxin.lu
 * @version V 1.0
 * @since 2025-06-04
 */
@UtilityClass
public class AESUtil {

    private final String keyLengthErrMsg = "Key 不能爲空並且key長度必須等于16";

    public static SecretKey genKey() throws Exception {
        //获取一个密钥生成器实例
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(128);
        return keyGenerator.generateKey();
    }

    public static SecretKey secretKey(byte[] bytes)
            throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException {
        SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG", "SUN");
        secureRandom.setSeed(bytes);
        // 密钥生成器
        KeyGenerator kgen = KeyGenerator.getInstance("AES");
        // 使用用户提供的随机源初始化此密钥生成器，使其具有确定的密钥大小(256位)
        kgen.init(256, secureRandom);
        // 生成一个密钥(SecretKey接口)
        SecretKey secretKey = kgen.generateKey();
        byte[] enCodeFormat = secretKey.getEncoded();
        // 根据给定的字节数组和密钥算法的名称构造一个密钥(SecretKey的实现类)
        SecretKeySpec key = new SecretKeySpec(enCodeFormat, "AES");
        // 创建一个Cipeher类,此类为加密和解密提供密码功能
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        // 用密钥初始化此cipher
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return key;
    }

    /**
     * 使用AES算法，根据提供的密码来加密数据，
     * <br/>并将加密数据转换成16进制
     *
     * @param content        要加密的内容
     * @param secretKeyBytes 加密用的密码
     * @return
     */
    public static byte[] encrypt(byte[] content, byte[] secretKeyBytes) throws Exception {
        // 使用SecureRandom类产生加密的强随机数
        //  RNG 算法的名称:SHA1PRNG
        //  提供者的名称:SUN
        SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG", "SUN");
        secureRandom.setSeed(secretKeyBytes);
        // 密钥生成器
        KeyGenerator kgen = KeyGenerator.getInstance("AES");
        // 使用用户提供的随机源初始化此密钥生成器，使其具有确定的密钥大小(128位)
        kgen.init(128, secureRandom);

        // 生成一个密钥(SecretKey接口)
        SecretKey secretKey = kgen.generateKey();
        byte[] enCodeFormat = secretKey.getEncoded();
        // 根据给定的字节数组和密钥算法的名称构造一个密钥(SecretKey的实现类)
        SecretKeySpec key = new SecretKeySpec(enCodeFormat, "AES");
        // 创建一个Cipeher类,此类为加密和解密提供密码功能
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        // 用密钥初始化此cipher
        cipher.init(Cipher.ENCRYPT_MODE, key);
        // 按单部分操作加密数据
        return cipher.doFinal(content);

    }

    /**
     * 使用AES算法，根据提供的密码来解密数据
     *
     * @param content        要解密的内容
     * @param secretKeyBytes 解密用的密码
     * @return
     */
    public static byte[] decrypt(byte[] content, byte[] secretKeyBytes) throws Exception {
        KeyGenerator kgen = KeyGenerator.getInstance("AES");
        SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG", "SUN");
        secureRandom.setSeed(secretKeyBytes);
        kgen.init(128, secureRandom);
        SecretKey secretKey = kgen.generateKey();
        byte[] enCodeFormat = secretKey.getEncoded();
        SecretKeySpec key = new SecretKeySpec(enCodeFormat, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key);
        // 把要解密的数据转化为2进制之后，再进行解密
        return cipher.doFinal(content);
    }


    /**
     * 加密
     *
     * @param cleartext
     * @param key
     * @return
     * @throws Exception
     */
    public String encrypt(String cleartext, String key) throws Exception {
        return encrypt(cleartext, key.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 加密
     *
     * @param cleartext 明文
     * @param secretKey 密钥
     * @return
     * @throws Exception
     */
    public String encrypt(String cleartext, byte[] secretKey) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(secretKey, "AES");
        /**
         * 算法/模式/补码方式
         */
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
        byte[] encrypted = cipher.doFinal(cleartext.getBytes(StandardCharsets.UTF_8));
        //此处使用BASE64做转码功能，同时能起到2次加密的作用。
        return new String(Base64.getEncoder().encode(encrypted), StandardCharsets.UTF_8);
    }

    /**
     * 解密
     *
     * @param ciphertext
     * @param key
     * @return
     * @throws Exception
     */
    public static String decrypt(String ciphertext, String key) throws Exception {
        return decrypt(ciphertext, key.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 解密
     *
     * @param ciphertext 密文
     * @param secretKey  密钥
     * @return
     * @throws Exception
     */
    public static String decrypt(String ciphertext, byte[] secretKey) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(secretKey, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, skeySpec);
        /**
         * 先用base64解密
         */
        byte[] encrypted1 = Base64.getDecoder().decode(ciphertext);
        byte[] original = cipher.doFinal(encrypted1);
        return new String(original, StandardCharsets.UTF_8);
    }

    /**
     * AES CBC 加密
     *
     * @param cleartext 明文
     * @param secretKey 密钥 16, 24, or 32 bytes to select
     * @param iv        向量
     * @return
     * @throws Exception
     */
    public static String encrypt(byte[] cleartext, byte[] secretKey, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec skeySpec = new SecretKeySpec(secretKey, "AES");
        //使用CBC模式，需要一个向量iv，可增加加密算法的强度
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivSpec);
        byte[] encrypted = cipher.doFinal(cleartext);
        return Base64.getEncoder().encodeToString(encrypted);
    }

    /**
     * 解密
     *
     * @param ciphertext 密文
     * @param secretKey  密钥 16, 24, or 32 bytes to select
     * @param iv         向量
     * @return
     * @throws Exception
     */
    public static String decrypt(byte[] ciphertext, byte[] secretKey, byte[] iv) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(secretKey, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec);
        //先用base64解密
        byte[] encrypted1 = Base64.getDecoder().decode(ciphertext);
        byte[] original = cipher.doFinal(encrypted1);
        return new String(original, StandardCharsets.UTF_8);
    }
}
