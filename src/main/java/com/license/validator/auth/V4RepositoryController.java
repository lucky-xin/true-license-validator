package com.license.validator.auth;

import global.namespace.fun.io.api.Decoder;
import global.namespace.fun.io.api.Store;
import global.namespace.fun.io.bios.BIOS;
import global.namespace.truelicense.api.auth.RepositoryController;
import global.namespace.truelicense.api.auth.RepositoryIntegrityException;
import global.namespace.truelicense.api.codec.Codec;

import java.security.Signature;
import java.util.Base64;

import static global.namespace.fun.io.bios.BIOS.memory;
import static java.util.Objects.requireNonNull;

/**
 * V4RepositoryController
 *
 * @author luchaoxin
 * @version V 1.0
 * @since 2023-11-17
 */
public class V4RepositoryController implements RepositoryController {
    private final Codec codec;
    private final V4RepositoryFactory.V4RepositoryModel model;

    V4RepositoryController(Codec codec, V4RepositoryFactory.V4RepositoryModel model) {
        this.codec = requireNonNull(codec);
        this.model = requireNonNull(model);
    }

    @Override
    public final Decoder sign(Signature engine, Object artifact) throws Exception {
        Store store = BIOS.memory(1024 * 1024);
        codec.encoder(store).encode(artifact);
        byte[] artifactData = store.content();
        engine.update(artifactData);
        byte[] signatureData = engine.sign();
        String encodedArtifact = Base64.getEncoder().encodeToString(artifactData);
        String encodedSignature = Base64.getEncoder().encodeToString(signatureData);
        String signatureAlgorithm = engine.getAlgorithm();
        model.setSignature(encodedSignature);
        model.setArtifact(encodedArtifact);
        model.setAlgorithm(signatureAlgorithm);
        return codec.decoder(store);
    }

    @Override
    public final Decoder verify(final Signature engine) throws Exception {
        if (!engine.getAlgorithm().equalsIgnoreCase(model.getAlgorithm())) {
            throw new IllegalArgumentException();
        }
        byte[] artifactData = Base64.getDecoder().decode(model.getArtifact());
        engine.update(artifactData);
        if (!engine.verify(Base64.getDecoder().decode(model.getSignature()))) {
            throw new RepositoryIntegrityException();
        }
        Store store = memory();
        store.content(artifactData);
        return codec.decoder(store);
    }
}
