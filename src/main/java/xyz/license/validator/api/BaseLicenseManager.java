package xyz.license.validator.api;

import global.namespace.fun.io.api.Source;
import global.namespace.fun.io.api.Store;
import global.namespace.fun.io.bios.BIOS;
import global.namespace.truelicense.api.ConsumerLicenseManager;
import global.namespace.truelicense.api.License;
import global.namespace.truelicense.api.LicenseFunctionComposition;
import global.namespace.truelicense.api.LicenseManagementContext;
import global.namespace.truelicense.api.LicenseManagementException;
import global.namespace.truelicense.api.LicenseManagerParameters;
import global.namespace.truelicense.api.UncheckedConsumerLicenseManager;
import global.namespace.truelicense.core.passwd.MinimumPasswordPolicy;
import global.namespace.truelicense.core.passwd.ObfuscatedPasswordProtection;
import global.namespace.truelicense.obfuscate.ObfuscatedString;
import global.namespace.truelicense.v4.V4;
import xyz.license.validator.auth.V4Authentication;
import xyz.license.validator.auth.V4AuthenticationParameters;
import xyz.license.validator.auth.V4RepositoryFactory;
import xyz.license.validator.codec.V4CodecFactory;
import xyz.license.validator.crypto.V4Encryption;
import xyz.license.validator.crypto.V4EncryptionParameters;
import xyz.license.validator.entity.LicenseKey;
import xyz.license.validator.utils.V4ParametersUtils;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.time.Clock;
import java.util.zip.Deflater;

/**
 * BaseLicenseManager
 *
 * @author chaoxin.lu
 * @version V 1.0
 * @since 2023/11/19
 */
public class BaseLicenseManager implements ConsumerLicenseManager {

    protected Store license;
    protected final ConsumerLicenseManager consumerLicenseManager;

    public BaseLicenseManager(
            SecretKey secretKey,
            LicenseKey licenseKey) throws IOException {
        ObfuscatedString obfuscatedString = new ObfuscatedString(ObfuscatedString.array(licenseKey.getKeyPass()));
        ObfuscatedPasswordProtection protection = new ObfuscatedPasswordProtection(obfuscatedString);
        V4EncryptionParameters parameters = new V4EncryptionParameters(
                secretKey,
                V4Encryption.ALGORITHM,
                protection
        );
        V4AuthenticationParameters authParams = V4ParametersUtils.authParams(licenseKey);
        V4Encryption encryption = new V4Encryption(parameters);
        LicenseManagementContext context = V4.builder()
                .authenticationFactory(p -> new V4Authentication(authParams))
                .cachePeriodMillis(1000L)
                .codecFactory(new V4CodecFactory(encryption))
                .encryptionFactory(p -> encryption)
                .clock(Clock.systemUTC())
                .compression(BIOS.deflate(Deflater.BEST_COMPRESSION))
                .initializationComposition(LicenseFunctionComposition.decorate)
                .passwordPolicy(new MinimumPasswordPolicy())
                .subject(licenseKey.getSubject())
                .validationComposition(LicenseFunctionComposition.decorate)
                .repositoryFactory(new V4RepositoryFactory())
                .build();

        Store clm = BIOS.memory(1024 * 1024);
        byte[] keysStoreBytes = licenseKey.getKeysStoreBytes();
        Store ks = BIOS.memory(keysStoreBytes.length);
        ks.content(keysStoreBytes);
        this.consumerLicenseManager = context.consumer()
                .encryption(encryption)
                .storeInSystemPreferences(BaseLicenseManager.class)
                .authentication()
                .storeProtection(protection)
                .keyProtection(protection)
                .alias(licenseKey.getAlias())
                .algorithm(licenseKey.getAlg())
                .loadFrom(ks)
                .up()
                .storeIn(clm)
                .build();
    }

    @Override
    public void install(Source source) throws LicenseManagementException {
        consumerLicenseManager.install(source);
    }

    @Override
    public License load() throws LicenseManagementException {
        return consumerLicenseManager.load();
    }

    @Override
    public void verify() throws LicenseManagementException {
        consumerLicenseManager.verify();
    }

    @Override
    public void uninstall() throws LicenseManagementException {
        consumerLicenseManager.uninstall();
    }

    @Override
    public UncheckedConsumerLicenseManager unchecked() {
        return consumerLicenseManager.unchecked();
    }

    @Override
    public LicenseManagerParameters parameters() {
        return consumerLicenseManager.parameters();
    }

    @Override
    public LicenseManagementContext context() {
        return consumerLicenseManager.context();
    }
}
