package com.license.validator;

import com.license.validator.entity.CreatorParam;
import com.license.validator.entity.CustomKeyStoreParam;
import com.license.validator.entity.LicenseKey;
import com.license.validator.svr.ServerInfo;
import com.license.validator.utils.SysUtil;
import de.schlichtherle.license.CipherParam;
import de.schlichtherle.license.DefaultCipherParam;
import de.schlichtherle.license.DefaultLicenseParam;
import de.schlichtherle.license.KeyStoreParam;
import de.schlichtherle.license.LicenseContent;
import de.schlichtherle.license.LicenseContentException;
import de.schlichtherle.license.LicenseManager;
import de.schlichtherle.license.LicenseNotary;
import de.schlichtherle.license.LicenseParam;
import de.schlichtherle.license.NoLicenseInstalledException;
import de.schlichtherle.xml.GenericCertificate;

import javax.security.auth.x500.X500Principal;
import java.beans.XMLDecoder;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Objects;
import java.util.Set;
import java.util.prefs.Preferences;

/**
 * 自定义LicenseManager，用于增加额外的服务器硬件信息校验
 *
 * @author zifangsky
 * @date 2018/4/23
 * @since 1.0.0
 */
public class OffLineLicenseValidator extends LicenseManager {

    //默认BUFSIZE
    private static final int DEFAULT_BUFSIZE = 8 * 1024;

    public OffLineLicenseValidator(LicenseKey param) {
        super(param(param));
    }

    public byte[] create(CreatorParam param) throws Exception {
        X500Principal principal = new X500Principal(param.getX500Name());
        LicenseContent licenseContent = new LicenseContent();
        licenseContent.setHolder(principal);
        licenseContent.setIssuer(principal);
        licenseContent.setSubject(super.getLicenseParam().getSubject());
        licenseContent.setIssued(param.getIssued());
        licenseContent.setNotBefore(param.getIssued());
        licenseContent.setNotAfter(param.getExpiry());
        licenseContent.setConsumerType(param.getConsumerType());
        licenseContent.setConsumerAmount(param.getConsumers());
        licenseContent.setInfo(param.getDescription());

        //扩展校验服务器硬件信息
        ServerInfo extra = new ServerInfo();
        extra.setIpAddrs(param.getIpAddrs());
        extra.setMacAddrs(param.getMacAddrs());
        licenseContent.setExtra(extra);
        return super.create(licenseContent);
    }


    /**
     * 初始化证书生成参数
     *
     * @return LicenseParam
     */
    public static LicenseParam param(LicenseKey param) {
        Preferences preferences = Preferences.userNodeForPackage(OffLineLicenseValidator.class);
        //设置对证书内容加密的秘钥
        CipherParam cipherParam = new DefaultCipherParam(param.getStorePass());
        KeyStoreParam privateStoreParam = new CustomKeyStoreParam(
                param.getKeysStoreBytes(),
                param.getAlias(),
                param.getStorePass(),
                param.getKeyPass()
        );
        return new DefaultLicenseParam(
                param.getSubject(),
                preferences,
                privateStoreParam,
                cipherParam
        );
    }

    /**
     * 复写create方法
     *
     * @return gzip byte[]
     */
    @Override
    protected synchronized byte[] create(LicenseContent content, LicenseNotary notary) throws Exception {
        initialize(content);
        this.validateCreate(content);
        GenericCertificate certificate = notary.sign(content);
        return getPrivacyGuard().cert2key(certificate);
    }

    /**
     * 复写install方法，其中validate方法调用本类中的validate方法，校验IP地址、Mac地址等其他信息
     *
     * @param key    key
     * @param notary notary
     * @return LicenseContent
     */
    @Override
    protected synchronized LicenseContent install(byte[] key, LicenseNotary notary) throws Exception {
        GenericCertificate certificate = getPrivacyGuard().key2cert(key);
        notary.verify(certificate);
        LicenseContent content = (LicenseContent) this.load(certificate.getEncoded());
        this.validate(content);
        setLicenseKey(key);
        setCertificate(certificate);
        return content;
    }

    /**
     * 复写verify方法，调用本类中的validate方法，校验IP地址、Mac地址等其他信息
     *
     * @param notary
     * @return LicenseContent
     */
    @Override
    protected synchronized LicenseContent verify(final LicenseNotary notary)
            throws Exception {
        // Load license key from preferences,
        byte[] key = getLicenseKey();
        if (null == key) {
            throw new NoLicenseInstalledException(getLicenseParam().getSubject());
        }
        GenericCertificate certificate = getPrivacyGuard().key2cert(key);
        notary.verify(certificate);
        final LicenseContent content = (LicenseContent) this.load(certificate.getEncoded());
        this.validate(content);
        setCertificate(certificate);

        return content;
    }

    /**
     * 校验生成证书的参数信息
     *
     * @param content 证书正文
     * @author zifangsky
     * @date 2018/5/2 15:43
     * @since 1.0.0
     */
    protected synchronized void validateCreate(LicenseContent content)
            throws LicenseContentException {
        Date now = new Date();
        Date notBefore = content.getNotBefore();
        Date notAfter = content.getNotAfter();
        if (null != notAfter && now.after(notAfter)) {
            throw new LicenseContentException("证书失效时间不能早于当前时间");
        }
        if (null != notBefore && null != notAfter && notAfter.before(notBefore)) {
            throw new LicenseContentException("证书生效时间不能晚于证书失效时间");
        }
        final String consumerType = content.getConsumerType();
        if (null == consumerType) {
            throw new LicenseContentException("用户类型不能为空");
        }
    }


    /**
     * 复写validate方法，增加IP地址、Mac地址等其他信息校验
     *
     * @param content LicenseContent
     */
    @Override
    protected synchronized void validate(final LicenseContent content) throws LicenseContentException {
        //1. 首先调用父类的validate方法
        super.validate(content);

        //2. 然后校验自定义的License参数
        //License中可被允许的参数信息
        ServerInfo expectedCheckModel = (ServerInfo) content.getExtra();
        if (expectedCheckModel == null) {
            return;
        }
        //当前服务器真实的参数信息
        ServerInfo serverCheckModel = null;
        try {
            serverCheckModel = SysUtil.getServerInfo();
        } catch (Exception e) {
            throw new LicenseContentException(e.getMessage());
        }

        if (serverCheckModel == null) {
            return;
        }

        //校验IP地址
        if (checkIpAddress(expectedCheckModel.getIpAddrs(), serverCheckModel.getIpAddrs())) {
            throw new LicenseContentException("当前服务器的IP没在授权范围内");
        }

        //校验Mac地址
        if (checkIpAddress(expectedCheckModel.getMacAddrs(), serverCheckModel.getMacAddrs())) {
            throw new LicenseContentException("当前服务器的Mac地址没在授权范围内");
        }

        //校验主板序列号
        if (checkSerial(expectedCheckModel.getBoardSerial(), serverCheckModel.getBoardSerial())) {
            throw new LicenseContentException("当前服务器的主板序列号没在授权范围内");
        }

        //校验CPU序列号
        if (checkSerial(expectedCheckModel.getCpuSerial(), serverCheckModel.getCpuSerial())) {
            throw new LicenseContentException("当前服务器的CPU序列号没在授权范围内");
        }

    }


    /**
     * 重写XMLDecoder解析XML
     *
     * @param encoded XML类型字符串
     * @return java.lang.Object
     */
    private Object load(String encoded) throws IOException {
        try (BufferedInputStream inputStream =
                     new BufferedInputStream(new ByteArrayInputStream(encoded.getBytes(StandardCharsets.UTF_8)));
             XMLDecoder decoder = new XMLDecoder(new BufferedInputStream(inputStream, DEFAULT_BUFSIZE), null, null);) {
            return decoder.readObject();
        }
    }

    /**
     * 校验当前服务器的IP/Mac地址是否在可被允许的IP范围内<br/>
     * 如果存在IP在可被允许的IP/Mac地址范围内，则返回true
     *
     * @return boolean
     */
    private boolean checkIpAddress(Set<String> expectedList, Set<String> serverList) {
        if (expectedList != null && !expectedList.isEmpty()) {
            if (serverList != null && !serverList.isEmpty()) {
                for (String expected : expectedList) {
                    if (serverList.contains(expected.trim())) {
                        return false;
                    }
                }
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * 校验当前服务器硬件（主板、CPU等）序列号是否在可允许范围内
     *
     * @return boolean
     */
    private boolean checkSerial(String expectedSerial, String serverSerial) {
        return Objects.equals(expectedSerial, serverSerial);
    }

}
