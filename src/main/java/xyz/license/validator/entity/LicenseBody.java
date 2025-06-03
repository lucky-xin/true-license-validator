package xyz.license.validator.entity;

import global.namespace.fun.io.api.Store;
import global.namespace.fun.io.bios.BIOS;
import global.namespace.truelicense.api.LicenseManagementException;

import java.util.Arrays;
import java.util.Objects;

/**
 * LicenseBody
 *
 * @author chaoxin.lu
 * @version V 1.0
 * @since 2025-06-03
 */
public record LicenseBody(String uuid,
                          String sign,
                          String sku,
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
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LicenseBody that = (LicenseBody) o;
        return Objects.equals(uuid, that.uuid)
                && Objects.equals(sign, that.sign)
                && Objects.equals(sku, that.sku)
                && Arrays.equals(licBytes, that.licBytes);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(uuid, sign, sku);
        result = 31 * result + Arrays.hashCode(licBytes);
        return result;
    }

    @Override
    public String toString() {
        return "LicenseBody{" +
                "uuid='" + uuid + '\'' +
                ", sign='" + sign + '\'' +
                ", productCode='" + sku + '\'' +
                ", licBytes=" + Arrays.toString(licBytes) +
                '}';
    }
}
