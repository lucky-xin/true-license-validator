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
     * 证书x500Name
     */
    private String x500Name = "CN=localhost, OU=IT, O=localhost, L=GZ, ST=GD, C=CN";

    /**
     * license id
     */
    private String uuid;

    /**
     * 密钥id
     */
    private Integer secretId;

    /**
     * 证书生效时间
     */
    private Date issued;

    /**
     * 证书失效时间
     */
    private Date expiry;

    /**
     * 用户类型: user:个人，1:企业
     */
    private String consumerType = "user";

    /**
     * 用户数量
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
