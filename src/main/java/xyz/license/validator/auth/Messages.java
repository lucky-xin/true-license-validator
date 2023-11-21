package xyz.license.validator.auth;

import global.namespace.truelicense.api.i18n.Message;
import global.namespace.truelicense.obfuscate.Obfuscate;
import global.namespace.truelicense.spi.i18n.FormattedMessage;

/**
 * Messages
 *
 * @author luchaoxin
 * @version V 1.0
 * @since 2023-11-21
 */
public class Messages {
    @Obfuscate
    static final String CONSUMER_AMOUNT_IS_NOT_POSITIVE = "consumerAmountIsNotPositive";

    @Obfuscate
    static final String CONSUMER_TYPE_IS_NULL = "consumerTypeIsNull";

    @Obfuscate
    static final String HOLDER_IS_NULL = "holderIsNull";

    @Obfuscate
    static final String INVALID_SUBJECT = "invalidSubject";

    @Obfuscate
    static final String INVALID_IP = "invalidIP";

    @Obfuscate
    static final String INVALID_MAC = "invalidMAC";

    @Obfuscate
    static final String ISSUED_IS_NULL = "issuedIsNull";

    @Obfuscate
    static final String ISSUER_IS_NULL = "issuerIsNull";

    @Obfuscate
    static final String LICENSE_HAS_EXPIRED = "licenseHasExpired";

    @Obfuscate
    static final String LICENSE_IS_NOT_YET_VALID = "licenseIsNotYetValid";

    /**
     * The message key for the canonical name of an unknown user.
     */
    @Obfuscate
    static final String UNKNOWN = "unknown";

    private static final Class<?> BASE_CLASS = Messages.class;

    public static Message message(String key, Object... args) {
        return new FormattedMessage(BASE_CLASS, key, args);
    }

    public static Message lite(String msg) {
        return locale -> msg;
    }

    private Messages() {
    }
}
