package xyz.license.validator.entity;

import global.namespace.truelicense.api.LicenseManagementException;
import global.namespace.truelicense.api.LicenseValidationException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.license.validator.auth.Messages;
import xyz.license.validator.utils.LicenseConstants;
import xyz.license.validator.utils.SignatureHelper;

import java.io.Serial;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

/**
 * LicenseToken
 *
 * @author luchaoxin
 * @version V 1.0
 * @since 2023-11-06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LicenseToken implements Serializable {

    @Serial
    private static final long serialVersionUID = 8600137500316662317L;

    /**
     * 序列号
     */
    private String serial;

    /**
     * 时间戳
     */
    private Long timestamp;

    public static LicenseToken create(String uuid) throws LicenseManagementException {
        try {
            String random = UUID.randomUUID().toString();
            String sign = SignatureHelper.genSign(random, uuid);
            byte[] randomBytes = random.getBytes(StandardCharsets.UTF_8);
            byte[] signBytes = sign.getBytes(StandardCharsets.UTF_8);
            ByteBuffer buffer = ByteBuffer.allocate(5 + randomBytes.length + signBytes.length)
                    .put(LicenseConstants.MAGIC_BYTE)
                    .putInt(randomBytes.length)
                    .put(randomBytes)
                    .put(signBytes);
            return new LicenseToken(
                    new String(buffer.array(), StandardCharsets.UTF_8),
                    System.currentTimeMillis()
            );
        } catch (Exception e) {
            throw new LicenseManagementException(e);
        }
    }

    public record Parser(String random, String sign) {

    }

    public Parser check(String uuid) throws LicenseValidationException {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(serial.getBytes(StandardCharsets.UTF_8));
            assert buffer.get() == LicenseConstants.MAGIC_BYTE;
            int randomLen = buffer.getInt();
            byte[] randomBytes = new byte[randomLen];
            buffer.get(randomBytes, 0, randomLen);

            int start = buffer.position() + buffer.arrayOffset();
            int length = buffer.limit() - randomLen - 5;
            byte[] signBytes = new byte[length];
            buffer.get(start, signBytes, 0, length);

            String random = new String(randomBytes, StandardCharsets.UTF_8);
            String sign = new String(signBytes, StandardCharsets.UTF_8);
            String newSign = SignatureHelper.genSign(random, uuid);

            assert Objects.equals(sign, newSign);
            return new Parser(random, sign);
        } catch (Exception e) {
            throw new LicenseValidationException(Messages.lite("invalid license token"));
        }
    }
}
