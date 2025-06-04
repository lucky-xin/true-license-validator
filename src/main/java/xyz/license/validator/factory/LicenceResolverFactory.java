package xyz.license.validator.factory;

import cn.hutool.core.io.resource.Resource;
import global.namespace.fun.io.api.Store;
import global.namespace.fun.io.bios.BIOS;
import lombok.Builder;
import xyz.license.validator.enums.FileType;
import xyz.license.validator.enums.Version;
import xyz.license.validator.resolver.LicenceResolver;
import xyz.license.validator.resolver.ResolverV1;
import xyz.license.validator.resolver.ResolverV2;

import java.io.IOException;
import java.util.Base64;

/**
 * LicenseManagerUtils
 *
 * @author chaoxin.lu
 * @version V 1.0
 * @since 2025-05-29
 */
@Builder
public class LicenceResolverFactory {

    private Resource license;
    private FileType type;
    private Version version;

    public LicenceResolver create() throws IOException {
        byte[] bytes = switch (type) {
            case BINARY -> license.readBytes();
            case BASE64 -> Base64.getDecoder().decode(license.readBytes());
            default -> throw new IllegalStateException("Invalid type");
        };
        Store store = BIOS.memory(bytes.length);
        store.content(bytes);
        return switch (version) {
            case V_1_0 -> new ResolverV1(store);
            case V_2_0 -> new ResolverV2(store);
        };
    }
}
