/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry.encode;

import org.jkiss.utils.Base64;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.DESedeKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.KeySpec;

/**
 * Secured password encrypter
 */
public class SecuredPasswordEncrypter implements PasswordEncrypter {

    private static final byte[] PASSWORD_ENCRYPTION_KEY = "sdf@!#$verf^wv%6Fwe%$$#FFGwfsdefwfe135s$^H)dg".getBytes();

    public static final String SCHEME_DESEDE = "DESede";
    public static final String SCHEME_DES = "DES";

    private static final String CHARSET = "UTF8";

    private KeySpec keySpec;
    private SecretKeyFactory keyFactory;
    private Cipher cipher;

    public SecuredPasswordEncrypter() throws EncryptionException
    {
        this(SCHEME_DES);
    }

    public SecuredPasswordEncrypter(String encryptionScheme) throws EncryptionException
    {

        try {
            if (encryptionScheme.equals(SCHEME_DESEDE)) {
                keySpec = new DESedeKeySpec(PASSWORD_ENCRYPTION_KEY);
            } else if (encryptionScheme.equals(SCHEME_DES)) {
                keySpec = new DESKeySpec(PASSWORD_ENCRYPTION_KEY);
            } else {
                throw new IllegalArgumentException("Encryption scheme not supported: " + encryptionScheme);
            }

            keyFactory = SecretKeyFactory.getInstance(encryptionScheme);
            cipher = Cipher.getInstance(encryptionScheme);

        } catch (InvalidKeyException e) {
            throw new EncryptionException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new EncryptionException(e);
        } catch (NoSuchPaddingException e) {
            throw new EncryptionException(e);
        }

    }

    public String encrypt(String unencryptedString) throws EncryptionException
    {
        if (unencryptedString == null || unencryptedString.trim().length() == 0) {
            throw new IllegalArgumentException("Empty string");
        }

        try {
            SecretKey key = keyFactory.generateSecret(keySpec);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] cleartext = unencryptedString.getBytes(CHARSET);
            byte[] ciphertext = cipher.doFinal(cleartext);

            return Base64.encode(ciphertext);
        } catch (Exception e) {
            throw new EncryptionException(e);
        }
    }

    public String decrypt(String encryptedString) throws EncryptionException
    {
        if (encryptedString == null || encryptedString.trim().length() <= 0) {
            throw new IllegalArgumentException("Empty encrypted string");
        }

        try {
            SecretKey key = keyFactory.generateSecret(keySpec);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] cleartext = Base64.decode(encryptedString);
            byte[] ciphertext = cipher.doFinal(cleartext);

            return new String(ciphertext, CHARSET);
        } catch (Exception e) {
            throw new EncryptionException(e);
        }
    }

}
