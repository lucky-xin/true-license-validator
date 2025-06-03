package xyz.license.validator.resolver;

import global.namespace.truelicense.api.LicenseValidationException;
import xyz.license.validator.entity.LicenseBody;

/**
 * LicenceResolver
 *
 * @author chaoxin.lu
 * @version V 1.0
 * @since 2025-06-03
 */
public interface LicenceResolver {
    /**
     * 解析
     *
     * @return LicenseBody
     * @throws LicenseValidationException LicenseValidationException
     */
    LicenseBody resolve() throws LicenseValidationException;
}
