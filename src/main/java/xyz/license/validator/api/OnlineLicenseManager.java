package xyz.license.validator.api;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import global.namespace.fun.io.api.Source;
import global.namespace.fun.io.api.Store;
import global.namespace.fun.io.bios.BIOS;
import global.namespace.truelicense.api.ConsumerLicenseManager;
import global.namespace.truelicense.api.License;
import global.namespace.truelicense.api.LicenseManagementContext;
import global.namespace.truelicense.api.LicenseManagementException;
import global.namespace.truelicense.api.LicenseManagerParameters;
import global.namespace.truelicense.api.LicenseValidationException;
import global.namespace.truelicense.api.UncheckedConsumerLicenseManager;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.license.validator.auth.Messages;
import xyz.license.validator.entity.LicenseBody;
import xyz.license.validator.entity.LicenseToken;
import xyz.license.validator.entity.R;
import xyz.license.validator.enums.FileType;
import xyz.license.validator.enums.Version;
import xyz.license.validator.resolver.LicenceResolver;
import xyz.license.validator.store.LicenseTokenStore;
import xyz.license.validator.store.LocalFileLicenseTokenStore;
import xyz.license.validator.svr.ServerInfo;
import xyz.license.validator.utils.LicenseConstants;
import xyz.license.validator.utils.LicenseManagerUtils;
import xyz.license.validator.utils.SysUtil;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
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
    private final LicenceResolver resolver;
    private volatile LicenseToken token;
    private final String url;
    private final HttpClient cli;
    @Setter
    private LicenseTokenStore licenseTokenStore;
    private final Random random = new Random();

    private Store store;

    public OnlineLicenseManager(String licenseValidatorUrl, String licenseFilePath, FileType type, Version ver) {
        this.url = licenseValidatorUrl;
        this.resolver = LicenseManagerUtils.createResolver(licenseFilePath, type, ver);
        HttpClient.Version version = HttpClient.Version.HTTP_1_1;
        HttpClient.Builder builder = HttpClient.newBuilder();
        boolean isHttps = licenseValidatorUrl.startsWith("https");
        if (isHttps) {
            version = HttpClient.Version.HTTP_2;
            try {
                SSLContext context = createContext();
                builder.sslContext(context);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        this.cli = builder.version(version).connectTimeout(Duration.ofSeconds(10)).build();
        this.licenseTokenStore = new LocalFileLicenseTokenStore();
    }

    @Override
    public void install(Source source) throws LicenseManagementException {
        try {
            if (this.store != null) {
                return;
            }
            byte[] bytes = BIOS.content(source);
            this.store = BIOS.memory(bytes.length);
            this.store.content(bytes);
        } catch (Exception e) {
            throw new LicenseManagementException(e);
        }
    }


    @Override
    public License load() throws LicenseManagementException {
        throw new UnsupportedOperationException("Unsupported load license");
    }

    @Override
    public void verify() throws LicenseManagementException {
        LicenseBody body = resolver.resolve();
        try {
            token = licenseTokenStore.get();
        } catch (Exception e) {
            throw new LicenseManagementException(e);
        }
        if (!body.sku().equals(System.getenv(LicenseConstants.SKU_NAME))) {
            throw new LicenseValidationException(Messages.lite("Invalid license"));
        }
        String serial = "";
        if (token != null) {
            token.check(body.uuid());
            serial = token.getSerial();
        }
        byte[] licBytes = body.licBytes();

        R<LicenseToken> r;
        try {
            int len = random.nextInt(9) + Byte.SIZE;
            byte[] array = new byte[len];
            random.nextBytes(array);
            ByteBuffer writerBuff = ByteBuffer.allocate(Integer.BYTES + 1 + len + licBytes.length);
            writerBuff.put(LicenseConstants.MAGIC_BYTE).putInt(len).put(array).put(licBytes);
            String secret = Base64.getEncoder().encodeToString(writerBuff.array());
            ServerInfo serverInfo = SysUtil.getServerInfo();
            Map<String, Serializable> params = Map.of(
                    "uuid", body.uuid(),
                    "secret", secret,
                    "svr", serverInfo,
                    "serial", serial
            );
            String reqJson = JSON.toJSONString(params);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.ofString(reqJson))
                    .setHeader("Content-Type", "application/json")
                    .build();
            HttpResponse<String> resp = cli.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String json = resp.body();
            Type type = new TypeReference<R<LicenseToken>>() {
            }.getType();
            r = JSON.parseObject(json, type);
            log.debug("license verify req:{} response:{}", reqJson, json);
        } catch (Exception e) {
            if (token != null) {
                // 联网校验失败，如果上一次认证token存在，则当前认证通过
                log.info("license verify failed has lock in cache skip error");
                return;
            }
            throw new LicenseManagementException(e);
        }
        if (r != null && r.getCode() == 1) {
            token = r.getData();
            log.info("install license succeed,serial:{}", token.getSerial());
            try {
                licenseTokenStore.store(token);
                return;
            } catch (IOException e) {
                throw new LicenseManagementException(e);
            }
        }
        if (r != null) {
            log.error("license verify failed,error:{}", r.getMsg());
        }
        throw new LicenseValidationException(Messages.lite("invalid license"));
    }

    @Override
    public void uninstall() throws LicenseManagementException {
        token = null;
        try {
            licenseTokenStore.remove();
        } catch (IOException e) {
            throw new LicenseManagementException(e);
        }
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

    private static SSLContext createContext() throws NoSuchAlgorithmException, KeyManagementException {
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }

                    @Override
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
        };

        SSLContext context = SSLContext.getInstance("SSL");
        context.init(null, trustAllCerts, new java.security.SecureRandom());
        return context;
    }

}
