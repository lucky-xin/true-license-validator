package xyz.license.validator.store;

import xyz.license.validator.entity.LicenseToken;

import java.io.IOException;

/**
 * License 校验成功缓存
 *
 * @author luchaoxin
 * @version V 1.0
 * @since 2023-11-08
 */
public interface LicenseTokenStore {

    /**
     * get the license token
     *
     * @return LicenseToken
     * @throws IOException
     */
    LicenseToken get() throws IOException;

    /**
     * remove the license token
     *
     * @throws IOException
     */
    void remove() throws IOException;

    /**
     * store the license token
     *
     * @param token tobe store
     * @throws IOException
     */

    void store(LicenseToken token) throws IOException;
}
