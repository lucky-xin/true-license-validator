package xyz.license.validator.utils;

import global.namespace.fun.io.api.Store;
import global.namespace.fun.io.bios.BIOS;
import xyz.license.validator.entity.LicenseFileResolver;
import xyz.license.validator.enums.FileType;

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

    public static LicenseFileResolver createResolver(String licenseFilePath, FileType type) {
        return switch (type) {
            case BINARY -> new LicenseFileResolver(BIOS.file(licenseFilePath));
            case BASE64 -> {
                try (InputStream in = new FileInputStream(licenseFilePath)) {
                    byte[] bytes = Base64.getDecoder().decode(in.readAllBytes());
                    Store memory = BIOS.memory(bytes.length);
                    memory.content(bytes);
                    yield new LicenseFileResolver(memory);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
            default -> throw new IllegalStateException("Invalid type");
        };
    }
}
