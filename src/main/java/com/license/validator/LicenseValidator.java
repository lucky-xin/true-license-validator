package com.license.validator;

import com.license.validator.entity.LicenseToken;
import global.namespace.truelicense.api.LicenseManagementException;

/**
 * 校验器接口
 *
 * @author luchaoxin
 * @version V 1.0
 * @since 2023-11-06
 */
public interface LicenseValidator {


    /**
     * 复写install方法，其中validate方法调用本类中的validate方法，校验IP地址、Mac地址等其他信息
     *
     * @return LicenseContent
     * @throws LicenseManagementException
     */
    LicenseToken install() throws LicenseManagementException;

    /**
     * 校验license
     *
     * @return LicenseToken
     * @throws LicenseManagementException
     */
    LicenseToken verify() throws LicenseManagementException;
}
