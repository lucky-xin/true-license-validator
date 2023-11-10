package com.license.validator.store;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.license.validator.entity.LicenseToken;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * License 校验成功缓存
 *
 * @author luchaoxin
 * @version V 1.0
 * @since 2023-11-08
 */
public class LocalFileLicenseStore implements LicenseStore {
    private final String lockFileName;

    public LocalFileLicenseStore() {
        lockFileName = Optional.ofNullable(System.getenv("LICENSE_FILE_PATH"))
                .map(f -> {
                    int idx = f.lastIndexOf(File.separator);
                    return f.substring(0, idx) + File.separator + "lock";
                })
                .orElse("lock");
    }

    @Override
    public LicenseToken getLicenseToken() throws IOException {
        File file = new File(lockFileName);
        if (!file.exists()) {
            return null;
        }
        try (InputStream lic = new FileInputStream(file)) {
            return JSON.parseObject(lic.readAllBytes(), new TypeReference<LicenseToken>() {
            }.getType());
        }
    }

    @Override
    public void storeLicenseToken(LicenseToken token) throws IOException {
        try (OutputStream out = Files.newOutputStream(Path.of(lockFileName))) {
            JSON.writeTo(out, token);
        }
    }
}

