package xyz.license.validator.resolver;

import global.namespace.fun.io.api.Source;
import global.namespace.truelicense.api.LicenseValidationException;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.license.validator.auth.Messages;
import xyz.license.validator.entity.LicenseBody;
import xyz.license.validator.utils.SignatureHelper;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * License密钥解析器 V1
 *
 * @author luchaoxin
 * @version V 1.0
 * @since 2023-11-06
 */
@Data
@NoArgsConstructor
public class ResolverV1 implements LicenceResolver {

    /**
     * license 内容
     */
    private Source lic;

    public ResolverV1(Source lic) {
        this.lic = lic;
    }


    @Override
    public LicenseBody resolve() throws LicenseValidationException {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(
                    lic.applyReader(InputStream::readAllBytes)
            );
            int uuidLen = buffer.getInt();
            byte[] uuidBytes = new byte[uuidLen];
            buffer.get(uuidBytes, 0, uuidLen);

            int signLen = buffer.getInt();
            byte[] signBytes = new byte[signLen];
            buffer.get(signBytes, 0, signLen);

            int productCodeLen = buffer.getInt();
            byte[] productCodeBytes = new byte[productCodeLen];
            buffer.get(productCodeBytes, 0, productCodeLen);

            int start = buffer.position() + buffer.arrayOffset();
            int length = buffer.limit() - uuidLen - signLen - productCodeLen - Integer.BYTES * 3;
            byte[] licBytes = new byte[length];
            buffer.get(start, licBytes, 0, length);

            String uuid = new String(uuidBytes, StandardCharsets.UTF_8);
            String sign = new String(signBytes, StandardCharsets.UTF_8);
            String productCode = new String(productCodeBytes, StandardCharsets.UTF_8);
            String encoded = Base64.getEncoder().encodeToString(licBytes);
            String genSign = SignatureHelper.genSign(encoded + productCode, uuid);
            if (!sign.equals(genSign)) {
                throw new LicenseValidationException(Messages.lite("invalid signature"));
            }
            return new LicenseBody(uuid, sign, productCode, licBytes);
        } catch (Exception e) {
            if (e instanceof LicenseValidationException ee) {
                throw ee;
            }
            throw new LicenseValidationException(Messages.lite("invalid signature"));
        }
    }
}
