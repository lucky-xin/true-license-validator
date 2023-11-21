package xyz.license.validator.entity;

import global.namespace.fun.io.api.Source;
import global.namespace.truelicense.api.passwd.Password;
import global.namespace.truelicense.api.passwd.PasswordProtection;
import global.namespace.truelicense.api.passwd.PasswordUsage;
import global.namespace.truelicense.core.auth.NotaryException;
import global.namespace.truelicense.obfuscate.Obfuscate;
import xyz.license.validator.auth.V4AuthenticationParameters;
import xyz.license.validator.exception.LicenseInvalidException;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Optional;

import static xyz.license.validator.auth.Messages.message;

/**
 * KeyStoreResolver
 *
 * @author luchaoxin
 * @version V 1.0
 * @since 2023-11-21
 */
public class KeyStoreResolver {
    private KeyStore keyStore;
    private final V4AuthenticationParameters parameters;

    @Obfuscate
    static final String NO_PRIVATE_KEY = "noPrivateKey";

    @Obfuscate
    static final String NO_CERTIFICATE = "noCertificate";

    @Obfuscate
    static final String NO_SUCH_ENTRY = "noSuchEntry";

    @Obfuscate
    private static final String DEFAULT_ALGORITHM = "SHA1withDSA";

    public KeyStoreResolver(V4AuthenticationParameters parameters) {
        this.parameters = parameters;
    }

    public PrivateKey privateKey() {
        final KeyStore.Entry entry = keyStoreEntry(PasswordUsage.ENCRYPTION);
        if (entry instanceof KeyStore.PrivateKeyEntry e) {
            return e.getPrivateKey();
        } else {
            throw new LicenseInvalidException(message(NO_PRIVATE_KEY).toString());
        }
    }

    public PublicKey publicKey() {
        return certificate().getPublicKey();
    }

    public Certificate certificate() {
        final KeyStore.Entry entry = keyStoreEntry(PasswordUsage.DECRYPTION);
        if (entry instanceof KeyStore.PrivateKeyEntry e) {
            return e.getCertificate();
        } else if (entry instanceof KeyStore.TrustedCertificateEntry e) {
            return e.getTrustedCertificate();
        } else {
            throw new LicenseInvalidException(message(NO_CERTIFICATE).toString());
        }
    }

    public String alias() {
        return parameters.alias();
    }

    PasswordProtection keyProtection() {
        return parameters.keyProtection();
    }

    PasswordProtection storeProtection() {
        return parameters.storeProtection();
    }

    public Optional<String> configuredAlgorithm() {
        return parameters.algorithm();
    }

    String storeType() {
        return parameters.storeType();
    }

    Optional<Source> source() {
        return parameters.source();
    }

    public KeyStore.Entry keyStoreEntry(final PasswordUsage usage) {
        try {
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
                assert !ks().containsAlias(alias());
                throw new NotaryException(message(NO_SUCH_ENTRY));
            }
        } catch (Exception e) {
            throw new LicenseInvalidException("load key store failed", e);
        }
    }

    boolean isKeyEntry() throws Exception {
        return ks().isKeyEntry(alias());
    }

    boolean isCertificateEntry() throws Exception {
        return ks().isCertificateEntry(alias());
    }

    KeyStore.Entry keyStoreEntry(Optional<KeyStore.PasswordProtection> protection) throws Exception {
        return ks().getEntry(alias(), protection.orElse(null));
    }

    public KeyStore ks() throws Exception {
        if (keyStore != null) {
            return keyStore;
        }
        keyStore = newKeyStore();
        return keyStore;
    }

    public String algorithm() {
        return configuredAlgorithm().orElseGet(this::defaultAlgorithm);
    }

    String defaultAlgorithm() {
        final Certificate cert = certificate();
        if (cert instanceof X509Certificate x) {
            return x.getSigAlgName();
        } else {
            return DEFAULT_ALGORITHM;
        }
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
}
