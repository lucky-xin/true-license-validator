package xyz.license.validator.api;

import cn.hutool.core.io.resource.Resource;
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
import xyz.license.validator.utils.AESUtil;

import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * LocalLicenseManager
 *
 * @author chaoxin.lu
 * @version V 1.0
 * @since 2023/11/19
 */
public class LocalLicenseManager extends BaseLicenseManager {

    static final Logger log = LoggerFactory.getLogger(LocalLicenseManager.class);

    private final LicenceResolver resolver;
    private final AtomicBoolean installed = new AtomicBoolean(false);


    public LocalLicenseManager(LicenseKey licenseKey,
                               Resource license,
                               FileType type,
                               Version version)
            throws LicenseManagementException, NoSuchPaddingException, NoSuchAlgorithmException,
            NoSuchProviderException, InvalidKeyException {
        super(AESUtil.secretKey(licenseKey.getAesKeyBytes()), licenseKey);
        try {

            this.resolver = LicenceResolverFactory.builder()
                    .license(license)
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
