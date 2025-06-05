package xyz.license.validator.api;

import cn.hutool.core.io.resource.Resource;
import cn.hutool.core.net.DefaultTrustManager;
import cn.hutool.core.net.SSLContextBuilder;
import cn.hutool.core.net.SSLProtocols;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.license.validator.auth.Messages;
import xyz.license.validator.entity.LicenseBody;
import xyz.license.validator.entity.LicenseKey;
import xyz.license.validator.entity.LicenseToken;
import xyz.license.validator.entity.R;
import xyz.license.validator.enums.FileType;
import xyz.license.validator.enums.Version;
import xyz.license.validator.factory.LicenceResolverFactory;
import xyz.license.validator.resolver.LicenceResolver;
import xyz.license.validator.store.LicenseTokenStore;
import xyz.license.validator.store.LocalFileLicenseTokenStore;
import xyz.license.validator.svr.ServerInfo;
import xyz.license.validator.utils.LicenseConstants;
import xyz.license.validator.utils.SysUtil;

import javax.crypto.NoSuchPaddingException;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

/**
 * RemoteLicenseManager
 *
 * @author chaoxin.lu
 * @version V 1.0
 * @since 2023/11/19
 */

public class RemoteLicenseManager implements ConsumerLicenseManager {
    static final Logger log = LoggerFactory.getLogger(RemoteLicenseManager.class);

    private final LicenseTokenStore tokenStore;
    private final LicenceResolver resolver;
    private final HttpClient cli;
    private final Random random;
    private final String url;

    private LicenseToken token;
    private Store store;

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
        if (!body.sku().equals(System.getenv(LicenseConstants.SKU_NAME))) {
            throw new LicenseValidationException(Messages.lite("Invalid license"));
        }

        String serial = "";
        try {
            token = tokenStore.get();
            if (token != null) {
                token.check(body.uuid());
                serial = token.getSerial();
            }
        } catch (Exception e) {
            throw new LicenseManagementException(e);
        }
        R<LicenseToken> r;
        try {
            int len = random.nextInt(9) + Byte.SIZE;
            byte[] array = new byte[len];
            byte[] licBytes = body.licBytes();
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
            Type tr = new TypeReference<R<LicenseToken>>() {
            }.getType();
            r = JSON.parseObject(json, tr);
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
                tokenStore.store(token);
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
            tokenStore.remove();
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

    private static SSLContext createContext() {
        return SSLContextBuilder.create()
                .setProtocol(SSLProtocols.TLS)
                .setTrustManagers(DefaultTrustManager.INSTANCE)
                .setSecureRandom(new SecureRandom())
                .build();
    }


    private static LicenseTokenStore $default$tokenStore() {
        return new LocalFileLicenseTokenStore();
    }


    private static Version $default$version() {
        return Version.V_1_0;
    }


    private static FileType $default$type() {
        return FileType.BASE64;
    }


    private static Random $default$random() {
        return new Random();
    }

    RemoteLicenseManager(LicenseTokenStore tokenStore,
                         Random random,
                         LicenceResolver resolver,
                         String url,
                         HttpClient cli) {
        this.tokenStore = tokenStore;
        this.random = random;
        this.resolver = resolver;
        this.url = url;
        this.cli = cli;
    }


    public static OnlineLicenseManagerBuilder builder() {
        return new OnlineLicenseManagerBuilder();
    }


    public static class OnlineLicenseManagerBuilder {

        private LicenseTokenStore tokenStore;

        private Version version;

        private FileType type;

        private Random random;

        private Resource license;

        private String url;

        private SSLContext sslContext;

        private LocalLicenseManager localLicenseManager;

        private LicenseKey licenseKey;

        OnlineLicenseManagerBuilder() {
        }

        public OnlineLicenseManagerBuilder tokenStore(LicenseTokenStore tokenStore) {
            this.tokenStore = tokenStore;
            return this;
        }

        public OnlineLicenseManagerBuilder version(Version version) {
            this.version = version;
            return this;
        }

        public OnlineLicenseManagerBuilder type(FileType type) {
            this.type = type;
            return this;
        }

        public OnlineLicenseManagerBuilder random(Random random) {
            this.random = random;
            return this;
        }

        public OnlineLicenseManagerBuilder license(Resource license) {
            this.license = license;
            return this;
        }

        public OnlineLicenseManagerBuilder url(String url) {
            this.url = url;
            return this;
        }

        public OnlineLicenseManagerBuilder sslContext(SSLContext sslContext) {
            this.sslContext = sslContext;
            return this;
        }

        public OnlineLicenseManagerBuilder offlineLicenseManager(LocalLicenseManager localLicenseManager) {
            this.localLicenseManager = localLicenseManager;
            return this;
        }

        public OnlineLicenseManagerBuilder licenseKey(LicenseKey licenseKey) {
            this.licenseKey = licenseKey;
            return this;
        }

        public RemoteLicenseManager build() throws IOException, NoSuchPaddingException, NoSuchAlgorithmException,
                NoSuchProviderException, InvalidKeyException, LicenseManagementException {
            tokenStore = Optional.ofNullable(this.tokenStore)
                    .orElseGet(RemoteLicenseManager::$default$tokenStore);
            version = Optional.ofNullable(this.version)
                    .orElseGet(RemoteLicenseManager::$default$version);
            type = Optional.ofNullable(this.type)
                    .orElseGet(RemoteLicenseManager::$default$type);
            random = Optional.ofNullable(this.random)
                    .orElseGet(RemoteLicenseManager::$default$random);
            LicenceResolver resolver = LicenceResolverFactory.builder()
                    .type(type)
                    .license(this.license)
                    .version(version)
                    .build()
                    .create();
            HttpClient.Builder builder = HttpClient.newBuilder();
            boolean isHttps = this.url.startsWith("https");
            HttpClient.Version httpVersion = HttpClient.Version.HTTP_1_1;
            if (isHttps) {
                httpVersion = HttpClient.Version.HTTP_2;
                SSLContext context = Optional.ofNullable(sslContext)
                        .orElseGet(() -> {
                            try {
                                return createContext();
                            } catch (Exception e) {
                                throw new IllegalStateException(e);
                            }
                        });
                builder.sslContext(context);
            }
            HttpClient cli = builder.version(httpVersion).connectTimeout(Duration.ofSeconds(10)).build();
            if (this.localLicenseManager == null && this.licenseKey != null) {
                this.localLicenseManager = new LocalLicenseManager(
                        this.licenseKey,
                        this.license,
                        this.type,
                        this.version
                );
            }
            return new RemoteLicenseManager(
                    tokenStore,
                    random,
                    resolver,
                    Objects.requireNonNull(this.url),
                    cli
            );
        }

        @Override
        public String toString() {
            return "OnlineLicenseManager.OnlineLicenseManagerBuilder(tokenStore=" + this.tokenStore + ", version=" + this.version + ", type=" + this.type + ", random=" + this.random + ", license=" + this.license + ", url=" + this.url + ")";
        }
    }
}
