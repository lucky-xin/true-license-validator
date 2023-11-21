package com.license.validator;

import com.license.validator.entity.LicenseKey;
import com.license.validator.entity.LicenseResolver;
import com.license.validator.entity.LicenseToken;
import global.namespace.fun.io.api.Store;
import global.namespace.fun.io.bios.BIOS;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.UUID;

/**
 * 自定义LicenseManager，离线线校验
 *
 * @author luchaoxin
 * @version V 1.0
 * @since 2023-11-06
 */
public class OfflineLicenseValidator extends BaseLicenseValidator {
    private final LicenseResolver resolver;

    public OfflineLicenseValidator(
            SecretKey secretKey,
            LicenseKey licenseKey,
            String licenseFile) throws IOException {
        this(secretKey, licenseKey, BIOS.file(licenseFile));
    }

    public OfflineLicenseValidator(
            SecretKey secretKey,
            LicenseKey licenseKey,
            Store license) throws IOException {
        super(secretKey, licenseKey, null);
        this.resolver = new LicenseResolver(license.content());
        this.license = resolver.toStore();
    }

    /**
     * 复写install方法，其中validate方法调用本类中的validate方法，校验IP地址、Mac地址等其他信息
     *
     * @return LicenseContent
     */
    public synchronized LicenseToken install() throws Exception {
        consumerLicenseManager.install(resolver.toStore());
        return new LicenseToken(UUID.randomUUID().toString(), System.currentTimeMillis());
    }

    public LicenseToken verify() throws Exception {
        if (token == null) {
            synchronized (this) {
                if (token == null) {
                    token = install();
                }
            }
        }
        consumerLicenseManager.verify();
        return token;
    }
}
