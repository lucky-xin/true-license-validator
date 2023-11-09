package com.license.validator.entity;

import de.schlichtherle.license.KeyStoreParam;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * 自定义KeyStoreParam，用于将公私钥存储文件存放到其他磁盘位置而不是项目中
 *
 * @author luchaoxin
 * @version V 1.0
 * @since 2023-11-06
 */
public class CustomKeyStoreParam implements KeyStoreParam {

    private final byte[] secret;
    private final String alias;
    private final String storePwd;
    private final String keyPwd;

    public CustomKeyStoreParam(byte[] secret,
                               String alias,
                               String storePwd,
                               String keyPwd) {
        this.secret = secret;
        this.alias = alias;
        this.storePwd = storePwd;
        this.keyPwd = keyPwd;
    }


    @Override
    public String getAlias() {
        return alias;
    }

    @Override
    public String getStorePwd() {
        return storePwd;
    }

    @Override
    public String getKeyPwd() {
        return keyPwd;
    }

    /**
     * @return java.io.InputStream
     */
    @Override
    public InputStream getStream() {
        return new ByteArrayInputStream(secret);
    }
}
