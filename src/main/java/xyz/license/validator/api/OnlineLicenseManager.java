package xyz.license.validator.api;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import global.namespace.fun.io.api.Source;
import global.namespace.fun.io.bios.BIOS;
import global.namespace.truelicense.api.ConsumerLicenseManager;
import global.namespace.truelicense.api.License;
import global.namespace.truelicense.api.LicenseManagementContext;
import global.namespace.truelicense.api.LicenseManagementException;
import global.namespace.truelicense.api.LicenseManagerParameters;
import global.namespace.truelicense.api.LicenseValidationException;
import global.namespace.truelicense.api.UncheckedConsumerLicenseManager;
import lombok.Setter;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.license.validator.auth.Messages;
import xyz.license.validator.entity.LicenseResolver;
import xyz.license.validator.entity.LicenseToken;
import xyz.license.validator.entity.R;
import xyz.license.validator.store.LicenseStore;
import xyz.license.validator.store.LocalFileLicenseStore;
import xyz.license.validator.svr.ServerInfo;
import xyz.license.validator.utils.LicenseConstants;
import xyz.license.validator.utils.SysUtil;

import javax.net.ssl.SSLContext;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Random;

/**
 * OnlineLicenseManager
 *
 * @author chaoxin.lu
 * @version V 1.0
 * @since 2023/11/19
 */
public class OnlineLicenseManager implements ConsumerLicenseManager {

    static final Logger log = LoggerFactory.getLogger(OnlineLicenseManager.class);

    private final String licenseFilePath;
    private volatile LicenseToken token;

    private final String url;
    private final HttpClient cli;

    @Setter
    private LicenseStore licenseStore;

    private final Random random = new Random();

    public OnlineLicenseManager(
            String licenseValidatorUrl,
            String licenseFilePath) {
        this.licenseFilePath = licenseFilePath;
        this.url = licenseValidatorUrl;
        boolean isHttps = licenseValidatorUrl.startsWith("https");
        HttpClient.Version version = HttpClient.Version.HTTP_1_1;
        HttpClient.Builder builder = HttpClient.newBuilder();
        if (isHttps) {
            version = HttpClient.Version.HTTP_2;
            TrustStrategy ts = (x509Certificates, s) -> true;
            try {
                SSLContext context = SSLContexts.custom().loadTrustMaterial(ts).build();
                builder.sslContext(context);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        this.cli = builder
                .version(version)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.licenseStore = new LocalFileLicenseStore();
    }

    @Override
    public void install(Source source) throws LicenseManagementException {
        try {
            byte[] bytes = source.input().apply(InputStream::readAllBytes);
            LicenseResolver.LicenseBody resolve = new LicenseResolver(bytes).resolve();
            int len = random.nextInt(9) + 8;
            byte[] array = new byte[len];
            random.nextBytes(array);
            byte[] licBytes = resolve.licBytes();
            ByteBuffer writerBuff = ByteBuffer.allocate(LicenseConstants.INTEGER_LEN + 1 + len + licBytes.length);
            writerBuff.put(LicenseConstants.MAGIC_BYTE)
                    .putInt(len)
                    .put(array)
                    .put(licBytes);
            ServerInfo serverInfo = SysUtil.getServerInfo();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.ofString(
                                    JSON.toJSONString(
                                            Map.of(
                                                    "uuid", resolve.uuid(),
                                                    "secret", Base64.getEncoder().encodeToString(writerBuff.array()),
                                                    "svr", serverInfo,
                                                    "serial", ""
                                            )
                                    )
                            )
                    )
                    .setHeader("Content-Type", "application/json")
                    .build();
            HttpResponse<String> response = cli.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String json = response.body();
            Type type = new TypeReference<R<LicenseToken>>() {
            }.getType();
            R<LicenseToken> r = JSON.parseObject(json, type);
            if (r.getCode() != 1) {
                throw new LicenseValidationException(Messages.lite(r.getMsg()));
            }
            token = r.getData();
            licenseStore.storeLicenseToken(token);
        } catch (Exception e) {
            if (e instanceof LicenseValidationException ee) {
                throw ee;
            }
            throw new LicenseValidationException(Messages.lite("invalid license"));
        }
    }

    @Override
    public License load() throws LicenseManagementException {
        throw new UnsupportedOperationException("Unsupported load license");
    }

    @Override
    public void verify() throws LicenseManagementException {
        try {
            LicenseToken licenseToken = licenseStore.getLicenseToken();
            if (licenseToken == null) {
                install(BIOS.file(licenseFilePath));
            }
        } catch (Exception e) {
            if (e instanceof LicenseValidationException l) {
                throw l;
            }
            log.error("license verify failed", e);
        }
    }

    @Override
    public void uninstall() throws LicenseManagementException {
        token = null;
    }

    @Override
    public UncheckedConsumerLicenseManager unchecked() {
        return ConsumerLicenseManager.super.unchecked();
    }

    @Override
    public LicenseManagerParameters parameters() {
        throw new UnsupportedOperationException("Unsupported");
    }

    @Override
    public LicenseManagementContext context() {
        return ConsumerLicenseManager.super.context();
    }
}
