package com.license.validator.entity;

import com.license.validator.auth.Messages;
import com.license.validator.utils.SignatureHelper;
import global.namespace.fun.io.api.Store;
import global.namespace.fun.io.bios.BIOS;
import global.namespace.truelicense.api.LicenseValidationException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

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
public class LicenseResolver implements Serializable {

    @Serial
    private static final long serialVersionUID = 8600137500316662317L;

    /**
     * license 内容
     */
    byte[] content;

    public record LicenseBody(String uuid, String sign, Store secret) {

    }

    public LicenseBody resolve() throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(content);
        int uuidLen = buffer.getInt();
        byte[] uuidBytes = new byte[uuidLen];
        buffer.get(uuidBytes, 0, uuidLen);

        int signLen = buffer.getInt();
        byte[] signBytes = new byte[signLen];
        buffer.get(signBytes, 0, signLen);
        int start = buffer.position() + buffer.arrayOffset();
        int length = buffer.limit() - uuidLen - signLen - 8;
        byte[] licBytes = new byte[length];
        buffer.get(start, licBytes, 0, length);

        String uuid = new String(uuidBytes, StandardCharsets.UTF_8);
        String sign = new String(signBytes, StandardCharsets.UTF_8);

        String encoded = Base64.getEncoder().encodeToString(licBytes);
        String genSign = SignatureHelper.genSign(encoded, uuid);
        if (!sign.equals(genSign)) {
            throw new LicenseValidationException(Messages.lite("invalid signature"));
        }
        Store tmp = BIOS.memory(licBytes.length);
        tmp.content(licBytes);
        return new LicenseBody(uuid, sign, tmp);
    }
}
