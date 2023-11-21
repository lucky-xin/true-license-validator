package xyz.license.validator.auth;

import global.namespace.fun.io.api.Source;
import global.namespace.truelicense.api.auth.AuthenticationParameters;
import global.namespace.truelicense.api.passwd.PasswordProtection;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Optional;

/**
 * V4AuthenticationParameters
 *
 * @author luchaoxin
 * @version V 1.0
 * @since 2023-11-20
 */
public record V4AuthenticationParameters(
        Optional<String> algorithm,
        String alias,
        PasswordProtection keyProtection,
        Optional<Source> source,
        PasswordProtection storeProtection,
        String storeType,
        Optional<PrivateKey> privateKey,
        Optional<PublicKey> publicKey
) implements AuthenticationParameters {

}
