package com.license.validator.exception;

/**
 * validator
 *
 * @author luchaoxin
 * @version V 1.0
 * @since 2023-11-09
 */
public class LicenseInvalidException extends RuntimeException {
    public LicenseInvalidException(String message) {
        super(message);
    }

    public LicenseInvalidException(Throwable cause) {
        super(cause);
    }
}
