package xyz.license.validator.entity;

import global.namespace.fun.io.api.Source;
import global.namespace.fun.io.api.Store;
import global.namespace.fun.io.bios.BIOS;
import global.namespace.truelicense.api.LicenseManagementException;
import global.namespace.truelicense.api.LicenseValidationException;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.license.validator.auth.Messages;
import xyz.license.validator.utils.SignatureHelper;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

/**
 * License密钥文件解析器
 *
 * @author luchaoxin
 * @version V 1.0
 * @since 2023-11-06
 */
@Data
@NoArgsConstructor
public class LicenseFileResolver {

    /**
     * license 内容
     */
    private Source lic;

    public LicenseFileResolver(Source lic) {
        this.lic = lic;
    }

    public record LicenseBody(String uuid,
                              String sign,
                              byte[] licBytes) {

        public Store toStore() throws LicenseManagementException {
            try {
                Store tmp = BIOS.memory(licBytes.length);
                tmp.content(licBytes);
                return tmp;
            } catch (Exception e) {
                throw new LicenseManagementException(e);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LicenseBody that = (LicenseBody) o;
            return Objects.equals(uuid, that.uuid) && Objects.equals(sign, that.sign) && Arrays.equals(licBytes, that.licBytes);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(uuid, sign);
            result = 31 * result + Arrays.hashCode(licBytes);
            return result;
        }

        @Override
        public String toString() {
            return "LicenseBody{" +
                    "uuid='" + uuid + '\'' +
                    ", sign='" + sign + '\'' +
                    ", licBytes=" + Arrays.toString(licBytes) +
                    '}';
        }
    }

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
            return new LicenseBody(uuid, sign, licBytes);
        } catch (Exception e) {
            if (e instanceof LicenseValidationException ee) {
                throw ee;
            }
            throw new LicenseValidationException(Messages.lite("invalid signature"));
        }
    }
}
