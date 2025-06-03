package xyz.license.validator.utils;

import global.namespace.fun.io.api.Store;
import global.namespace.fun.io.bios.BIOS;
import xyz.license.validator.enums.FileType;
import xyz.license.validator.enums.Version;
import xyz.license.validator.resolver.LicenceResolver;
import xyz.license.validator.resolver.ResolverV1;
import xyz.license.validator.resolver.ResolverV2;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

/**
 * LicenseManagerUtils
 *
 * @author chaoxin.lu
 * @version V 1.0
 * @since 2025-05-29
 */
public class LicenseManagerUtils {

    public static LicenceResolver createResolver(String licenseFilePath, FileType type, Version version) {
        return switch (type) {
            case BINARY -> switch (version) {
                case V_1_0 -> new ResolverV1(BIOS.file(licenseFilePath));
                case V_2_0 -> new ResolverV2(BIOS.file(licenseFilePath));
            };
            case BASE64 -> {
                try (InputStream in = new FileInputStream(licenseFilePath)) {
                    byte[] bytes = Base64.getDecoder().decode(in.readAllBytes());
                    Store memory = BIOS.memory(bytes.length);
                    memory.content(bytes);
                    yield switch (version) {
                        case V_1_0 -> new ResolverV1(memory);
                        case V_2_0 -> new ResolverV2(memory);
                    };
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
            default -> throw new IllegalStateException("Invalid type");
        };
    }
}
