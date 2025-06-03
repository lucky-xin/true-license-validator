package xyz.license.validator.text;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 文本段
 *
 * @author chaoxin.lu
 * @version V 1.0
 * @since 2024-04-18
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Segment implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 段长度
     */
    private int length;

    /**
     * 段内容
     */
    private byte[] bytes;

    public Segment(byte[] bytes) {
        this.bytes = bytes;
        this.length = bytes.length;
    }

    public Segment(byte[] bytes, int length) {
        this.bytes = bytes;
        this.length = length;
    }
}
