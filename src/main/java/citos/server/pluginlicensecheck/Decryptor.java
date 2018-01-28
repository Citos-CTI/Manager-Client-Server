/*
 * Copyright (c) 2018.  Johannes Engler, Citos CTI
 */
package citos.server.pluginlicensecheck;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Decryptor {
    private PublicKey publicKey;
    private Cipher cipher;
    private boolean setUpSuccess;
    public Decryptor(String filepath) {
        try {
            this.getPublic(filepath);
            this.cipher = Cipher.getInstance("RSA");
            setUpSuccess = true;
        } catch (Exception e) {
            setUpSuccess = false;
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);
        }
    }
    private void getPublic(String filename) throws Exception {
        byte[] keyBytes = Files.readAllBytes(new File(filename).toPath());
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        this.publicKey = kf.generatePublic(spec);
    }

    public String decryptString(String msg) {
        try {
            this.cipher.init(Cipher.DECRYPT_MODE, publicKey);
            return new String(cipher.doFinal(Base64.decodeBase64(msg)), "UTF-8");
        } catch (InvalidKeyException | BadPaddingException | UnsupportedEncodingException | IllegalBlockSizeException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE,null,e);
            return "";
        }
    }
    public boolean isSetUpSuccess() {
        return setUpSuccess;
    }
}
