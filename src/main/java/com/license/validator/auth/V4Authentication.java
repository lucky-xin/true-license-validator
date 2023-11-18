package com.license.validator.auth;

import global.namespace.fun.io.api.Decoder;
import global.namespace.fun.io.api.Source;
import global.namespace.truelicense.api.auth.Authentication;
import global.namespace.truelicense.api.auth.AuthenticationParameters;
import global.namespace.truelicense.api.auth.RepositoryController;
import global.namespace.truelicense.api.i18n.Message;
import global.namespace.truelicense.api.passwd.Password;
import global.namespace.truelicense.api.passwd.PasswordProtection;
import global.namespace.truelicense.api.passwd.PasswordUsage;
import global.namespace.truelicense.core.auth.NotaryException;
import global.namespace.truelicense.obfuscate.Obfuscate;
import global.namespace.truelicense.spi.i18n.FormattedMessage;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Objects;
import java.util.Optional;

/**
 * V4 Authentication
 *
 * @author luchaoxin
 * @version V 1.0
 * @since 2023-11-17
 */
public class V4Authentication implements Authentication {

    private final AuthenticationParameters parameters;


    @Obfuscate
    private static final String DEFAULT_ALGORITHM = "SHA1withDSA";

    @Obfuscate
    static final String NO_PRIVATE_KEY = "noPrivateKey";

    @Obfuscate
    static final String NO_CERTIFICATE = "noCertificate";

    @Obfuscate
    static final String NO_SUCH_ENTRY = "noSuchEntry";

    final Cache cache = new Cache();

    public V4Authentication(final AuthenticationParameters parameters) {
        this.parameters = Objects.requireNonNull(parameters);
    }

    @Override
    public Decoder sign(RepositoryController controller, Object artifact) throws Exception {
        return cache.sign(controller, artifact);
    }

    @Override
    public Decoder verify(RepositoryController controller) throws Exception {
        return cache.verify(controller);
    }

    private AuthenticationParameters parameters() {
        return parameters;
    }

    private final class Cache {

        KeyStore keyStore;

        Decoder sign(RepositoryController controller, Object artifact) throws Exception {
            Signature engine = engine();
            PrivateKey key = privateKey();
            engine.initSign(key);
            return controller.sign(engine, artifact);
        }

        Decoder verify(RepositoryController controller) throws Exception {
            Signature engine = engine();
            PublicKey key = publicKey();
            engine.initVerify(key);
            return controller.verify(engine);
        }

        Signature engine() throws Exception {
            return Signature.getInstance(algorithm());
        }

        String algorithm() throws Exception {
            final Optional<String> configuredAlgorithm = configuredAlgorithm();
            return configuredAlgorithm.isPresent() ? configuredAlgorithm.get() : defaultAlgorithm();
        }

        String defaultAlgorithm() throws Exception {
            final Certificate cert = certificate();
            if (cert instanceof X509Certificate x) {
                return x.getSigAlgName();
            } else {
                return DEFAULT_ALGORITHM;
            }
        }

        PrivateKey privateKey() throws Exception {
            final KeyStore.Entry entry = keyStoreEntry(PasswordUsage.ENCRYPTION);
            if (entry instanceof KeyStore.PrivateKeyEntry e) {
                return e.getPrivateKey();
            } else {
                throw new NotaryException(message(NO_PRIVATE_KEY));
            }
        }

        PublicKey publicKey() throws Exception {
            return certificate().getPublicKey();
        }

        Certificate certificate() throws Exception {
            final KeyStore.Entry entry = keyStoreEntry(PasswordUsage.DECRYPTION);
            if (entry instanceof KeyStore.PrivateKeyEntry e) {
                return e.getCertificate();
            } else if (entry instanceof KeyStore.TrustedCertificateEntry e) {
                return e.getTrustedCertificate();
            } else {
                throw new NotaryException(message(NO_CERTIFICATE));
            }
        }

        KeyStore.Entry keyStoreEntry(final PasswordUsage usage) throws Exception {
            if (isKeyEntry()) {
                try (Password password = keyProtection().password(usage)) {
                    final KeyStore.PasswordProtection protection =
                            new KeyStore.PasswordProtection(password.characters());
                    try {
                        return keyStoreEntry(Optional.of(protection));
                    } finally {
                        protection.destroy();
                    }
                }
            } else if (isCertificateEntry()) {
                return keyStoreEntry(Optional.empty());
            } else {
                assert !keyStore().containsAlias(alias());
                throw new NotaryException(message(NO_SUCH_ENTRY));
            }
        }

        boolean isKeyEntry() throws Exception {
            return keyStore().isKeyEntry(alias());
        }

        boolean isCertificateEntry() throws Exception {
            return keyStore().isCertificateEntry(alias());
        }

        KeyStore.Entry keyStoreEntry(Optional<KeyStore.PasswordProtection> protection) throws Exception {
            return keyStore().getEntry(alias(), protection.orElse(null));
        }

        KeyStore keyStore() throws Exception {
            final KeyStore ks = keyStore;
            return null != ks ? ks : (keyStore = newKeyStore());
        }

        KeyStore newKeyStore() throws Exception {
            try (Password password = storeProtection().password(PasswordUsage.DECRYPTION)) {
                final KeyStore ks = KeyStore.getInstance(storeType());
                final char[] pc = password.characters();
                final Optional<Source> source = source();
                if (source.isPresent()) {
                    source.get().acceptReader(in -> ks.load(in, pc));
                } else {
                    ks.load(null, pc);
                }
                return ks;
            }
        }

        Message message(String key) {
            return new FormattedMessage(getClass(), key, alias());
        }

        String alias() {
            return parameters().alias();
        }

        PasswordProtection keyProtection() {
            return parameters().keyProtection();
        }

        Optional<String> configuredAlgorithm() {
            return parameters().algorithm();
        }

        Optional<Source> source() {
            return parameters().source();
        }

        PasswordProtection storeProtection() {
            return parameters().storeProtection();
        }

        String storeType() {
            return parameters().storeType();
        }
    }
}
