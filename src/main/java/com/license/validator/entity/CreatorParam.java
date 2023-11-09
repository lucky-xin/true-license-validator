package com.license.validator.entity;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
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

    /**
     * 证书失效时间，必传
     */
    private Date expiry;

    /**
     * 证书x500Name
     */
    private String x500Name = "CN=localhost, OU=IT, O=localhost, L=GZ, ST=GD, C=CN";

    /**
     * 证书生效时间，非必传
     */
    private Date issued;

    /**
     * 密钥id,默认1非必转
     */
    private Integer secretId;

    /**
     * 用户类型: User:个人只能激活一次，System:企业，非必转
     */
    private String consumerType = "User";

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
}
