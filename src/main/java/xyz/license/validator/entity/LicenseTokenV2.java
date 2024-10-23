package xyz.license.validator.entity;

import global.namespace.truelicense.api.LicenseManagementException;
import global.namespace.truelicense.api.LicenseValidationException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.encryption.SM2;
import xyz.license.validator.auth.Messages;
import xyz.license.validator.text.Block;
import xyz.license.validator.text.Segment;
import xyz.license.validator.utils.LicenseConstants;

import java.io.Serial;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * LicenseTokenV2
 *
 * @author luchaoxin
 * @version V 1.0
 * @since 2023-11-06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LicenseTokenV2 implements Serializable {

    @Serial
    private static final long serialVersionUID = 8600137500316662317L;

    /**
     * 分段数据块
     */
    private Block block;

    public static LicenseTokenV2 create(String uuid) throws LicenseManagementException {
        try {
            SM2 sm2 = SM2.fromHex(
                    System.getenv("TRUE_LICENSE_PRIVATE_KEY_HEX"),
                    System.getenv("TRUE_LICENSE_PUBLIC_KEY_HEX")
            );
            String random = UUID.randomUUID().toString();
            String timestamp = String.valueOf(System.currentTimeMillis());
            String signed = sm2.sign(random + "/" + uuid + "/" + timestamp);
            byte[] randomBytes = random.getBytes(StandardCharsets.UTF_8);
            byte[] signBytes = signed.getBytes(StandardCharsets.UTF_8);
            byte[] timestampBytes = String.valueOf(System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8);
            Block b = new Block();
            b.setMagic(LicenseConstants.MAGIC_BYTE);
            b.setSegments(List.of(
                    new Segment(randomBytes.length, randomBytes),
                    new Segment(signBytes.length, signBytes),
                    new Segment(timestampBytes.length, timestampBytes)
            ));
            return new LicenseTokenV2(b);
        } catch (Exception e) {
            throw new LicenseManagementException(e);
        }
    }

    public static LicenseTokenV2 from(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        Block b = Block.fromBuffer(LicenseConstants.MAGIC_BYTE, buffer);
        return new LicenseTokenV2(b);
    }

    public Parser check(String uuid) throws LicenseValidationException {
        try {
            assert this.block.getSegments().size() == 3;
            String random = new String(this.block.getSegments().get(0).getBytes(), StandardCharsets.UTF_8);
            String sign = new String(this.block.getSegments().get(1).getBytes(), StandardCharsets.UTF_8);
            String timestamp = new String(this.block.getSegments().get(2).getBytes(), StandardCharsets.UTF_8);

            SM2 sm2 = SM2.fromHex(
                    System.getenv("TRUE_LICENSE_PRIVATE_KEY_HEX"),
                    System.getenv("TRUE_LICENSE_PUBLIC_KEY_HEX")
            );
            String newSign = sm2.sign(random + "/" + uuid + "/" + timestamp);
            assert Objects.equals(sign, newSign);
            return new Parser(random, sign);
        } catch (Exception e) {
            throw new LicenseValidationException(Messages.lite("invalid license token"));
        }
    }

    public record Parser(String random, String sign) {

    }
}
