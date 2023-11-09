package com.license.validator;

import java.io.IOException;

/**
 * License 校验成功缓存
 *
 * @author luchaoxin
 * @version V 1.0
 * @since 2023-11-08
 */
public interface LicenseStore {

    LicenseToken getLicenseToken() throws IOException;

    void storeLicenseToken(LicenseToken token) throws IOException;
}
