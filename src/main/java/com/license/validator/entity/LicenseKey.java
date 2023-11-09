package com.license.validator.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * License密钥
 *
 * @author luchaoxin
 * @version V 1.0
 * @since 2023-11-06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LicenseKey implements Serializable {

    @Serial
    private static final long serialVersionUID = 8600137500316662317L;

    /**
     * 证书subject
     */
    private String subject;

    /**
     * 密钥别称
     */
    private String alias;

    /**
     * 密钥密码（需要妥善保管，不能让使用者知道）
     */
    private String keyPass;

    /**
     * 访问秘钥库的密码
     */
    private String storePass;

    /**
     * key alg
     */
    private String alg;

    /**
     * 密钥文件bytes
     */
    private byte[] keysStoreBytes;
}
