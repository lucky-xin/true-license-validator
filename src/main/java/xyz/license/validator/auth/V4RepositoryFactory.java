package xyz.license.validator.auth;

import global.namespace.truelicense.api.auth.RepositoryController;
import global.namespace.truelicense.api.auth.RepositoryFactory;
import global.namespace.truelicense.api.codec.Codec;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * V4 RepositoryFactory
 *
 * @author luchaoxin
 * @version V 1.0
 * @since 2023-11-17
 */
public final class V4RepositoryFactory
        implements RepositoryFactory<V4RepositoryFactory.V4RepositoryModel> {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class V4RepositoryModel {
        String algorithm;
        String artifact;
        String signature;
    }

    @Override
    public V4RepositoryModel model() {
        return new V4RepositoryModel();
    }

    @Override
    public Class<V4RepositoryModel> modelClass() {
        return V4RepositoryModel.class;
    }

    @Override
    public RepositoryController controller(Codec codec, V4RepositoryModel model) {
        return new V4RepositoryController(codec, model);
    }
}
