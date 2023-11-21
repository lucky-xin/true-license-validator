package xyz.license.validator.crypto;

import global.namespace.truelicense.api.crypto.EncryptionParameters;
import global.namespace.truelicense.api.passwd.PasswordProtection;

import javax.crypto.SecretKey;

/**
 * V4EncryptionParameters
 *
 * @author chaoxin.lu
 * @version V 1.0
 * @since 2023/11/16
 */
public record V4EncryptionParameters(SecretKey aesKey,
                                     String algorithm,
                                     PasswordProtection protection) implements EncryptionParameters {
}
