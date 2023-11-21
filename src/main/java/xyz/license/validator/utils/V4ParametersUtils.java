package xyz.license.validator.utils;

import global.namespace.fun.io.api.Source;
import global.namespace.fun.io.api.Store;
import global.namespace.fun.io.bios.BIOS;
import global.namespace.truelicense.core.passwd.ObfuscatedPasswordProtection;
import global.namespace.truelicense.obfuscate.ObfuscatedString;
import xyz.license.validator.auth.V4AuthenticationParameters;
import xyz.license.validator.crypto.V4Encryption;
import xyz.license.validator.crypto.V4EncryptionParameters;
import xyz.license.validator.entity.LicenseKey;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Optional;

/**
 * V4EncryptionParametersUtils
 *
 * @author luchaoxin
 * @version V 1.0
 * @since 2023-11-20
 */
public class V4ParametersUtils {

    public static SecretKey secretKey(byte[] bytes)
            throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException {
        SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG", "SUN");
        secureRandom.setSeed(bytes);
        // 密钥生成器
        KeyGenerator kgen = KeyGenerator.getInstance(V4Encryption.ALGORITHM);
        // 使用用户提供的随机源初始化此密钥生成器，使其具有确定的密钥大小(256位)
        kgen.init(256, secureRandom);
        // 生成一个密钥(SecretKey接口)
        SecretKey secretKey = kgen.generateKey();
        byte[] enCodeFormat = secretKey.getEncoded();
        // 根据给定的字节数组和密钥算法的名称构造一个密钥(SecretKey的实现类)
        SecretKeySpec key = new SecretKeySpec(enCodeFormat, V4Encryption.ALGORITHM);
        // 创建一个Cipeher类,此类为加密和解密提供密码功能
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        // 用密钥初始化此cipher
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return key;
    }

    public static V4EncryptionParameters encrParams(LicenseKey secret)
            throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException {
        ObfuscatedPasswordProtection protection = new ObfuscatedPasswordProtection(
                new ObfuscatedString(ObfuscatedString.array(secret.getKeyPass()))
        );
        SecretKey secretKey = secretKey(secret.getAesKeyBytes());
        return new V4EncryptionParameters(
                secretKey,
                V4Encryption.ALGORITHM,
                protection
        );
    }

    public static V4AuthenticationParameters authParams(LicenseKey secret) throws IOException {
        ObfuscatedPasswordProtection protection = new ObfuscatedPasswordProtection(
                new ObfuscatedString(ObfuscatedString.array(secret.getKeyPass()))
        );
        byte[] keysStoreBytes = secret.getKeysStoreBytes();
        Optional<Source> source = Optional.empty();
        if (keysStoreBytes != null) {
            Store ks = BIOS.memory(keysStoreBytes.length);
            ks.content(keysStoreBytes);
            source = Optional.of(ks);
        }
        return new V4AuthenticationParameters(
                Optional.ofNullable(secret.getAlg()),
                secret.getAlias(),
                protection,
                source,
                protection,
                secret.getStoreType(),
                Optional.empty(),
                Optional.empty()
        );
    }

    public static V4AuthenticationParameters authParamsByKey(
            String algorithm,
            PrivateKey privateKey,
            PublicKey publicKey) {
        return new V4AuthenticationParameters(
                Optional.of(algorithm),
                null,
                null,
                Optional.empty(),
                null,
                null,
                Optional.of(privateKey),
                Optional.of(publicKey)
        );
    }
}
