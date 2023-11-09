package com.license.validator;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * License 校验成功缓存
 *
 * @author luchaoxin
 * @version V 1.0
 * @since 2023-11-08
 */
public class LocalFileLicenseStore implements LicenseStore {
    private static final String LOCK_FILE_NAME = "lock";

    @Override
    public LicenseToken getLicenseToken() throws IOException {
        try (InputStream lic = Files.newInputStream(Path.of(LOCK_FILE_NAME))) {
            return JSON.parseObject(lic.readAllBytes(), new TypeReference<LicenseToken>() {
            }.getType());
        }
    }

    @Override
    public void storeLicenseToken(LicenseToken token) throws IOException {
        try (OutputStream out = Files.newOutputStream(Path.of(LOCK_FILE_NAME))) {
            JSON.writeTo(out, token);
        }
    }
}
