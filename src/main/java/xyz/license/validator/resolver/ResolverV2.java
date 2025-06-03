package xyz.license.validator.resolver;

import global.namespace.fun.io.api.Source;
import global.namespace.truelicense.api.LicenseValidationException;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.license.validator.auth.Messages;
import xyz.license.validator.entity.LicenseBody;
import xyz.license.validator.text.Block;
import xyz.license.validator.text.Segment;
import xyz.license.validator.utils.SignatureHelper;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static xyz.license.validator.utils.LicenseConstants.LIC_INDEX;
import static xyz.license.validator.utils.LicenseConstants.MAGIC_BYTE;
import static xyz.license.validator.utils.LicenseConstants.SIGN_INDEX;
import static xyz.license.validator.utils.LicenseConstants.SKU_INDEX;
import static xyz.license.validator.utils.LicenseConstants.UUID_INDEX;

/**
 * License密钥解析器 V2
 *
 * @author luchaoxin
 * @version V 1.0
 * @since 2023-11-06
 */
@Data
@NoArgsConstructor
public class ResolverV2 implements LicenceResolver {

    /**
     * license 内容
     */
    private Source lic;

    public ResolverV2(Source lic) {
        this.lic = lic;
    }

    @Override
    public LicenseBody resolve() throws LicenseValidationException {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(
                    lic.applyReader(InputStream::readAllBytes)
            );

            Block block = Block.fromBuffer(MAGIC_BYTE, buffer);
            List<Segment> segments = block.getSegments();
            String uuid = new String(segments.get(UUID_INDEX).getBytes(), StandardCharsets.UTF_8);
            String sign = new String(segments.get(SIGN_INDEX).getBytes(), StandardCharsets.UTF_8);
            String encoded = Base64.getEncoder().encodeToString(segments.get(LIC_INDEX).getBytes());
            String productCode = new String(segments.get(SKU_INDEX).getBytes(), StandardCharsets.UTF_8);
            String toSign = encoded + productCode;
            String genSign = SignatureHelper.genSign(toSign, uuid);
            if (!sign.equals(genSign)) {
                throw new LicenseValidationException(Messages.lite("invalid signature"));
            }
            return new LicenseBody(uuid, sign, productCode, segments.get(LIC_INDEX).getBytes());
        } catch (Exception e) {
            if (e instanceof LicenseValidationException ee) {
                throw ee;
            }
            throw new LicenseValidationException(Messages.lite("invalid signature"));
        }
    }
}
