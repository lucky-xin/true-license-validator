package xyz.license.validator.api;

import global.namespace.fun.io.api.Source;
import global.namespace.truelicense.api.LicenseManagementException;
import global.namespace.truelicense.api.LicenseValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.license.validator.entity.LicenseKey;
import xyz.license.validator.enums.FileType;
import xyz.license.validator.enums.Version;
import xyz.license.validator.factory.LicenceResolverFactory;
import xyz.license.validator.resolver.LicenceResolver;

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

    private final LicenceResolver resolver;
    private final AtomicBoolean installed = new AtomicBoolean(false);


    public OfflineLicenseManager(SecretKey secretKey,
                                 LicenseKey licenseKey,
                                 String licenseFile,
                                 FileType type,
                                 Version version) throws LicenseManagementException {
        super(secretKey, licenseKey);
        try {
            this.resolver = LicenceResolverFactory.builder()
                    .licenseFilePath(licenseFile)
                    .type(type)
                    .version(version)
                    .build()
                    .create();
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
