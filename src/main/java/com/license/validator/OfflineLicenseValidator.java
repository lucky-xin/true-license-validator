package com.license.validator;

import com.license.validator.entity.LicenseKey;
import com.license.validator.entity.LicenseResolver;
import com.license.validator.entity.LicenseToken;
import global.namespace.fun.io.api.Store;
import global.namespace.fun.io.bios.BIOS;
import global.namespace.truelicense.api.LicenseManagementException;

import javax.crypto.SecretKey;

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
            String licenseFile) throws Exception {
        this(secretKey, licenseKey, BIOS.file(licenseFile));
    }

    public OfflineLicenseValidator(
            SecretKey secretKey,
            LicenseKey licenseKey,
            Store licenseFile) throws Exception {
        super(secretKey, licenseKey, null);
        this.resolver = new LicenseResolver(licenseFile.content());
        this.license = resolver.resolve().toStore();
    }

    @Override
    public LicenseToken verify() throws LicenseManagementException {
        this.resolver.resolve();
        return super.verify();
    }
}
