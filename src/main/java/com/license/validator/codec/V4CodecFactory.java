package com.license.validator.codec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.license.validator.crypto.V4Encryption;
import global.namespace.fun.io.api.Decoder;
import global.namespace.fun.io.api.Encoder;
import global.namespace.fun.io.api.Socket;
import global.namespace.truelicense.api.codec.Codec;
import global.namespace.truelicense.api.codec.CodecFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;

/**
 * V4 CodecFactory
 *
 * @author chaoxin.lu
 * @version V 1.0
 * @since 2023/11/16
 */
public class V4CodecFactory implements CodecFactory {

    private final V4Encryption encryption;
    private final V4Codec codec;

    private class V4Codec implements Codec {

        private final ObjectMapper mapper = new global.namespace.truelicense.v4.V4CodecFactory().objectMapper();
        @Override
        public String contentType() {
            return "application/AES";
        }

        @Override
        public String contentTransferEncoding() {
            return "base64";
        }

        @Override
        public Encoder encoder(Socket<OutputStream> output) {
            return o -> encryption.output(output)
                    .accept(c -> c.write(mapper.writeValueAsBytes(o)));
        }

        @Override
        public Decoder decoder(Socket<InputStream> input) {
            return new Decoder() {
                @Override
                public <T> T decode(Type expected) throws Exception {
                    return encryption.input(input)
                            .apply(in -> mapper.readValue(in.readAllBytes(), mapper.constructType(expected)));
                }
            };
        }
    }

    public V4CodecFactory(V4Encryption encryption) {
        this.encryption = encryption;
        this.codec = new V4Codec();
    }

    @Override
    public Codec codec() {
        return codec;
    }
}
