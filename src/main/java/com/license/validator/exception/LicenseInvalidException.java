package com.license.validator.exception;

/**
 * LicenseInvalidException
 *
 * @author luchaoxin
 * @version V 1.0
 * @since 2023-11-09
 */
public class LicenseInvalidException extends RuntimeException {
    public LicenseInvalidException(Throwable cause) {
        super(cause);
    }

    public LicenseInvalidException(String message) {
        super(message);
    }

    public LicenseInvalidException(String message, Throwable cause) {
        super(message, cause);
    }
}
