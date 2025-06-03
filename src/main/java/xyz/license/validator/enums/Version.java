package xyz.license.validator.enums;

import lombok.Getter;

/**
 * FileType
 *
 * @author chaoxin.lu
 * @version V 1.0
 * @since 2025-05-29
 */
@Getter
public enum Version {
    /**
     * one
     */
    V_1_0((byte) 1),

    /**
     * two
     */
    V_2_0((byte) 2);
    private final Byte value;

    Version(Byte value) {
        this.value = value;
    }

     public static Version from(Byte value) {
        for (Version v : Version.values()) {
            if (v.value.equals(value)) {
                return v;
            }
        }
        return null;
    }
}
