package com.license.validator.svr;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

/**
 * 自定义需要校验的License参数
 *
 * @author luchaoxin
 * @version V 1.0
 * @since 2023-11-06
 */
@Data
public class ServerInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 8600137500316662317L;

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
