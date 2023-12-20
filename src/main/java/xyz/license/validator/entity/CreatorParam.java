package xyz.license.validator.entity;

import global.namespace.truelicense.api.License;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * License生成类需要的参数
 *
 * @author luchaoxin
 * @version V 1.0
 * @since 2023-11-06
 */
@Data
public class CreatorParam implements Serializable {

    @Serial
    private static final long serialVersionUID = -7793154252684580872L;

    private Long secretId = 1L;

    /**
     * 证书生效时间，非必传
     */
    private Date issued;

    /**
     * 证书失效时间，必传
     */
    @NotNull(message = "expiry must not nbe null")
    private Date expiry;

    /**
     * 产品编码
     */
    private String sku;

    /**
     * 证书x500Name,非必传
     */
    private String x500Name = "CN=localhost, OU=IT, O=localhost, L=GZ, ST=GD, C=CN";

    /**
     * 能激活次数，限制该证书能让几个客户使用，默认1，非必转,如果是User类型，则只能为1
     */
    private Integer consumers = 1;

    /**
     * 描述信息
     */
    private String description;

    /**
     * 可被允许的IP地址
     */
    private Set<String> ipAddrs;

    /**
     * 可被允许的MAC地址
     */
    private Set<String> macAddrs;

    /**
     * 可被允许的CPU序列号
     */
    private String cpuSerial;

    /**
     * 可被允许的主板序列号
     */
    private String boardSerial;

    public License toLicense(License orig) {
        orig.setIssued(this.issued);
        orig.setNotAfter(this.expiry);
        orig.setNotBefore(this.issued);
        orig.setConsumerAmount(this.consumers);
        orig.setConsumerType("System");
        orig.setInfo(this.description);
        Map<String, Object> ext = new HashMap<>();
        if (ipAddrs != null) {
            ext.put("ips", ipAddrs);
        }
        if (macAddrs != null) {
            ext.put("macs", macAddrs);
        }
        if (cpuSerial != null) {
            ext.put("cpu", cpuSerial);
        }
        if (boardSerial != null) {
            ext.put("board", boardSerial);
        }
        orig.setExtra(ext);
        return orig;
    }
}
