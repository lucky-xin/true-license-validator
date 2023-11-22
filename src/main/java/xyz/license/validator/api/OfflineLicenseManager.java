package xyz.license.validator.api;

import global.namespace.fun.io.api.Source;
import global.namespace.fun.io.bios.BIOS;
import global.namespace.truelicense.api.LicenseManagementException;
import global.namespace.truelicense.api.LicenseValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.license.validator.entity.LicenseKey;
import xyz.license.validator.entity.LicenseFileResolver;

import javax.crypto.SecretKey;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * OfflineLicenseManager
 *
 * @author chaoxin.lu
 * @version V 1.0
 * @since 2023/11/19
 */
public class OfflineLicenseManager extends BaseLicenseManager {

    static final Logger log = LoggerFactory.getLogger(OfflineLicenseManager.class);

    private final LicenseFileResolver resolver;
    private final AtomicBoolean installed = new AtomicBoolean(false);

    public OfflineLicenseManager(SecretKey secretKey,
                                 LicenseKey licenseKey,
                                 String licenseFile) throws LicenseManagementException {
        super(secretKey, licenseKey);
        try {
            this.resolver = new LicenseFileResolver(BIOS.file(licenseFile));
            this.license = resolver.resolve().toStore();
        } catch (Exception e) {
            throw new LicenseManagementException(e);
        }
    }

    @Override
    public void install(Source source) throws LicenseManagementException {
        super.install(source);
        installed.compareAndSet(false, true);
    }

    @Override
    public void verify() throws LicenseManagementException {
        try {
            if (installed.compareAndSet(false, true)) {
                this.license = resolver.resolve().toStore();
                super.install(this.license);
            }
            super.verify();
            installed.compareAndSet(true, false);
        } catch (Exception e) {
            if (e instanceof LicenseValidationException l) {
                throw l;
            }
            log.error("license verify failed", e);
        }
    }

}
