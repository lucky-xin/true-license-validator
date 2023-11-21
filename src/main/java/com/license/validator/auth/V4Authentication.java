package com.license.validator.auth;

import com.license.validator.entity.KeyStoreResolver;
import global.namespace.fun.io.api.Decoder;
import global.namespace.truelicense.api.auth.Authentication;
import global.namespace.truelicense.api.auth.RepositoryController;

import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Objects;

/**
 * V4 Authentication
 *
 * @author luchaoxin
 * @version V 1.0
 * @since 2023-11-17
 */
public class V4Authentication implements Authentication {

    private final V4AuthenticationParameters parameters;

    final Cache cache;

    public V4Authentication(final V4AuthenticationParameters parameters) {
        this.parameters = Objects.requireNonNull(parameters);
        this.cache = new Cache(parameters);
    }

    @Override
    public Decoder sign(RepositoryController controller, Object artifact) throws Exception {
        return cache.sign(controller, artifact);
    }

    @Override
    public Decoder verify(RepositoryController controller) throws Exception {
        return cache.verify(controller);
    }

    private final class Cache {

        private final KeyStoreResolver resolver;

        public Cache(V4AuthenticationParameters parameters) {
            this.resolver = new KeyStoreResolver(parameters);
        }

        Decoder sign(RepositoryController controller, Object artifact) throws Exception {
            Signature engine = engine();
            PrivateKey key = parameters.privateKey().orElseGet(resolver::privateKey);
            engine.initSign(key);
            return controller.sign(engine, artifact);
        }

        Decoder verify(RepositoryController controller) throws Exception {
            Signature engine = engine();
            PublicKey key = parameters.publicKey().orElseGet(resolver::publicKey);
            engine.initVerify(key);
            return controller.verify(engine);
        }

        Signature engine() throws NoSuchAlgorithmException {
            return Signature.getInstance(resolver.algorithm());
        }
    }
}
