package com.license.validator;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

/**
 * 自定义LicenseManager，在线校验
 *
 * @author luchaoxin
 * @version V 1.0
 * @since 2023-11-06
 */
public class OnLineLicenseValidator {
    static final Logger log = LoggerFactory.getLogger(OnLineLicenseValidator.class);

    private final String licenseFilePath;
    private volatile LicenseToken token;

    private final String url;
    private final HttpClient cli;

    @Setter
    private LicenseStore licenseStore;

    public OnLineLicenseValidator(String licenseServerEndpoint, String licenseFilePath) {
        this.licenseFilePath = licenseFilePath;
        if (licenseServerEndpoint.endsWith("/")) {
            this.url = licenseServerEndpoint + "license/activate";
        } else {
            this.url = licenseServerEndpoint + "/license/activate";
        }

        boolean isHttps = licenseServerEndpoint.startsWith("https");
        HttpClient.Version version = HttpClient.Version.HTTP_1_1;
        if (isHttps) {
            version = HttpClient.Version.HTTP_2;
        }
        this.cli = HttpClient.newBuilder()
                .version(version)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.licenseStore = new LocalFileLicenseStore();
    }


    /**
     * 复写install方法，其中validate方法调用本类中的validate方法，校验IP地址、Mac地址等其他信息
     *
     * @return LicenseContent
     */
    protected synchronized LicenseToken install() throws Exception {
        try (InputStream in = Files.newInputStream(Path.of(licenseFilePath))) {
            token = verify(in.readAllBytes(), "");
        }
        licenseStore.storeLicenseToken(token);
        return token;
    }

    public LicenseToken verify() throws Exception {
        if (token == null) {
            synchronized (this) {
                if (token == null) {
                    token = initVerify();
                }
            }
        }
        return token;
    }

    private LicenseToken initVerify() throws Exception {
        LicenseToken licenseToken = licenseStore.getLicenseToken();
        if (licenseToken == null) {
            return install();
        }

        try (InputStream lic = Files.newInputStream(Path.of(licenseFilePath))) {
            return verify(lic.readAllBytes(), licenseToken.getSerial());
        } catch (Exception e) {
            log.error("license verify failed", e);
            return null;
        }
    }

    @SuppressWarnings("all")
    public LicenseToken verify(byte[] bytes, String serial) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        int uuidLen = buffer.getInt();
        byte[] uuidBytes = new byte[uuidLen];
        buffer.get(uuidBytes, 0, uuidLen);

        int signLen = buffer.getInt();
        byte[] signBytes = new byte[signLen];
        buffer.get(signBytes, 0, signLen);
        String uuid = new String(uuidBytes, StandardCharsets.UTF_8);
        String sign = new String(signBytes, StandardCharsets.UTF_8);

        int start = buffer.position() + buffer.arrayOffset();
        int length = buffer.limit() - uuidLen - signLen - 8;
        byte[] licBytes = new byte[length];
        buffer.get(start, licBytes, 0, length);

        String encoded = Base64.getEncoder().encodeToString(licBytes);
        String genSign = SignatureHelper.genSign(encoded, uuid);
        if (!sign.equals(genSign)) {
            throw new IllegalStateException("invalid signature");
        }
        ServerInfo serverInfo = SysUtil.getServerInfo();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofString(
                                JSON.toJSONString(
                                        Map.of(
                                                "uuid", uuid,
                                                "sign", sign,
                                                "svr", serverInfo,
                                                "serial", serial
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
            throw new IllegalStateException(r.getMsg());
        }
        return r.getData();
    }
}