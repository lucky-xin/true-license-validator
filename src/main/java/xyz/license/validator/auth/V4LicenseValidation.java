package xyz.license.validator.auth;

import global.namespace.truelicense.api.License;
import global.namespace.truelicense.api.LicenseValidation;
import global.namespace.truelicense.api.LicenseValidationException;
import xyz.license.validator.exception.LicenseInvalidException;
import xyz.license.validator.svr.ServerInfo;
import xyz.license.validator.utils.SysUtil;

import java.time.Clock;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static xyz.license.validator.auth.Messages.message;

/**
 * V4LicenseValidation
 *
 * @author luchaoxin
 * @version V 1.0
 * @since 2023-11-21
 */
public class V4LicenseValidation implements LicenseValidation {
    private final Clock clock;
    private final String subject;

    public V4LicenseValidation(Clock clock, String subject) {
        this.clock = clock;
        this.subject = subject;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void validate(final License bean) throws LicenseValidationException {
        if (0 >= bean.getConsumerAmount()) {
            throw new LicenseValidationException(message(Messages.CONSUMER_AMOUNT_IS_NOT_POSITIVE, bean.getConsumerAmount()));
        }
        if (null == bean.getConsumerType()) {
            throw new LicenseValidationException(message(Messages.CONSUMER_TYPE_IS_NULL));
        }
        if (null == bean.getHolder()) {
            throw new LicenseValidationException(message(Messages.HOLDER_IS_NULL));
        }
        if (null == bean.getIssued()) {
            throw new LicenseValidationException(message(Messages.ISSUED_IS_NULL));
        }
        if (null == bean.getIssuer()) {
            throw new LicenseValidationException(message(Messages.ISSUER_IS_NULL));
        }
        // don't trust the system clock!
        final Date now = now();
        final Date notAfter = bean.getNotAfter();
        if (null != notAfter && now.after(notAfter)) {
            throw new LicenseValidationException(message(Messages.LICENSE_HAS_EXPIRED, notAfter));
        }
        final Date notBefore = bean.getNotBefore();
        if (null != notBefore && now.before(notBefore)) {
            throw new LicenseValidationException(message(Messages.LICENSE_IS_NOT_YET_VALID, notBefore));
        }
        if (!subject.equals(bean.getSubject())) {
            throw new LicenseValidationException(message(Messages.INVALID_SUBJECT, bean.getSubject(), subject));
        }

        //2. 然后校验自定义的License参数
        //License中可被允许的参数信息
        Map<String, Object> extra = (Map<String, Object>) bean.getExtra();
        if (extra == null || extra.isEmpty()) {
            return;
        }
        //当前服务器真实的参数信息
        ServerInfo serverCheckModel = null;
        try {
            serverCheckModel = SysUtil.getServerInfo();
        } catch (Exception e) {
            throw new LicenseInvalidException(e);
        }

        if (serverCheckModel == null) {
            return;
        }

        //校验IP地址
        Collection<String> ips = (Collection<String>) extra.get("ips");
        if (checkIpAddress(ips, serverCheckModel.getIpAddrs())) {
            throw new LicenseValidationException(Messages.lite("invalid ip"));
        }

        //校验Mac地址
        Collection<String> macs = (Collection<String>) extra.get("macs");
        if (checkIpAddress(macs, serverCheckModel.getMacAddrs())) {
            throw new LicenseValidationException(Messages.lite("invalid MAC"));
        }

        //校验主板序列号
        String cpu = (String) extra.get("cpu");
        if (checkSerial(cpu, serverCheckModel.getBoardSerial())) {
            throw new LicenseValidationException(Messages.lite("invalid board serial"));
        }

        //校验CPU序列号
        String board = (String) extra.get("board");
        if (checkSerial(board, serverCheckModel.getCpuSerial())) {
            throw new LicenseValidationException(Messages.lite("invalid board serial"));
        }
    }


    /**
     * 校验当前服务器的IP/Mac地址是否在可被允许的IP范围内<br/>
     * 如果存在IP在可被允许的IP/Mac地址范围内，则返回true
     *
     * @return boolean
     */
    private boolean checkIpAddress(Collection<String> expectedList, Set<String> serverList) {
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

    private Date now() {
        return Date.from(clock.instant());
    }
}
