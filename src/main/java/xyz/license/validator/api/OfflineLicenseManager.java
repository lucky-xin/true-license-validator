package xyz.license.validator.api;

import global.namespace.fun.io.api.Store;
import global.namespace.truelicense.api.LicenseManagementException;
import global.namespace.truelicense.api.LicenseValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.license.validator.entity.LicenseKey;
import xyz.license.validator.entity.LicenseResolver;

import javax.crypto.SecretKey;
import java.io.IOException;

/**
 * OfflineLicenseManager
 *
 * @author chaoxin.lu
 * @version V 1.0
 * @since 2023/11/19
 */
public class OfflineLicenseManager extends BaseLicenseManager {

    static final Logger log = LoggerFactory.getLogger(OfflineLicenseManager.class);

    private final LicenseResolver resolver;

    public OfflineLicenseManager(SecretKey secretKey,
                                 LicenseKey licenseKey,
                                 Store licenseFile) throws IOException, LicenseValidationException {
        super(secretKey, licenseKey);
        this.resolver = new LicenseResolver(licenseFile.content());
        this.license = resolver.resolve().toStore();
    }

    @Override
    public void verify() throws LicenseManagementException {
        try {
            this.license = resolver.resolve().toStore();
            super.verify();
        } catch (Exception e) {
            if (e instanceof LicenseValidationException l) {
                throw l;
            }
            log.error("license verify failed", e);
        }
    }

}
