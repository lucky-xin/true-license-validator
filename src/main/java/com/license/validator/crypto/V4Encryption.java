package com.license.validator.crypto;

import global.namespace.fun.io.api.Socket;
import global.namespace.truelicense.api.crypto.Encryption;
import global.namespace.truelicense.core.crypto.EncryptionMixin;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * V4Encryption
 *
 * @author chaoxin.lu
 * @version V 1.0
 * @since 2023/11/12
 */
public class V4Encryption extends EncryptionMixin implements Encryption {

    private final V4EncryptionParameters parameters;

    public V4Encryption(V4EncryptionParameters parameters) {
        super(parameters);
        this.parameters = parameters;
    }

    @Override
    public Socket<OutputStream> output(final Socket<OutputStream> output) {
        return output.map(out -> {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, parameters.aesKey());
            return new CipherOutputStream(out, cipher);
        });
    }

    @Override
    public Socket<InputStream> input(final Socket<InputStream> input) {
        return input.map(in -> {
            Cipher cipher = inCipher();
            return new CipherInputStream(in, cipher);
        });
    }

    public Cipher inCipher() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, parameters.aesKey());
        return cipher;
    }


}
