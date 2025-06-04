package xyz.license.validator.text;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

/**
 * 文本数据块
 *
 * @author chaoxin.lu
 * @version V 1.0
 * @since 2024-04-18
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Block implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 魔法标识
     */
    private Byte magic;

    /**
     * 分段列表
     */
    private List<Segment> segments;

    /**
     * 将当前数据转换为ByteBuffer。
     * 该方法首先计算所有segments（文本片段）的字节长度总和，如果存在magic字节，则总长度会增加1。
     * 然后分配一个足够大的ByteBuffer来容纳所有数据，并将数据写入ByteBuffer。
     *
     * @return ByteBuffer 包含所有segments数据以及可能的magic字节的ByteBuffer。
     */
    public ByteBuffer toBuffer() {
        // 计算所有segments的字节长度总和，如果存在magic字节，总长度会增加1
        int count = segments.stream()
                .mapToInt(t -> t.getBytes().length + Integer.BYTES)
                .sum();
        if (magic != null) {
            count++;
        }
        // 分配足够大的ByteBuffer
        ByteBuffer buffer = ByteBuffer.allocate(count);
        if (magic != null) {
            // 写入magic字节，如果存在
            buffer.put(magic);
        }
        // 写入所有segments的数据
        segments.forEach(t -> {
            buffer.putInt(t.getLength());
            buffer.put(t.getBytes());
        });
        return buffer;
    }

    /**
     * 从字节缓冲区创建一个 Block 对象。
     *
     * @param magic  魔法字节，用于验证数据的完整性。
     * @param buffer 包含 Block 数据的字节缓冲区。
     * @return 从给定的字节缓冲区构建的 Block 对象。
     * @throws IllegalArgumentException 如果验证魔法字节失败。
     */
    public static Block fromBuffer(Byte magic, ByteBuffer buffer) {
        int idx = 0;
        // 验证魔法字节
        if (magic != null) {
            idx++;
            if (buffer.get() != magic) {
                throw new IllegalArgumentException("Invalid magic");
            }
        }
        List<Segment> segments = new LinkedList<>();
        while (idx < buffer.limit()) {
            int len = buffer.getInt();
            byte[] bytes = new byte[len];
            buffer.get(bytes, 0, len);
            segments.add(new Segment(len, bytes));
            idx += len + 4;
        }
        return new Block(magic, segments);
    }
}
