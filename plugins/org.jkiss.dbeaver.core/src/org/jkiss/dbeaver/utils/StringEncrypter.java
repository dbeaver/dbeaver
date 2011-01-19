/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.utils;

import net.sf.jkiss.utils.Base64;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * Encryption util
 */
public class StringEncrypter {

    public static final String SCHEME_DES = "DES";

    private static final String CHARSET = "UTF8";

    private DESKeySpec keySpec;
    private Cipher cipher;

    public StringEncrypter(String encryptionKey) throws EncryptionException
    {
        if (encryptionKey == null) {
            throw new IllegalArgumentException("encryption key was null");
        }
        if (encryptionKey.trim().length() < 24) {
            throw new IllegalArgumentException("encryption key was less than 24 characters");
        }
        try {
            byte[] keyAsBytes = encryptionKey.getBytes(CHARSET);

            keySpec = new DESKeySpec(keyAsBytes);

            //keyFactory = SecretKeyFactory.getInstance(encryptionScheme);
            cipher = Cipher.getInstance(SCHEME_DES);

        } catch (InvalidKeyException e) {
            throw new EncryptionException(e);
        } catch (UnsupportedEncodingException e) {
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
            SecretKey key = makeSecretKey();//keyFactory.generateSecret(keySpec);
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
            SecretKey key = makeSecretKey();
            cipher.init(Cipher.DECRYPT_MODE, key/*, SecureRandom.getInstance("SHA1PRNG")*/);
            byte[] cleartext = Base64.decode(encryptedString);
            byte[] ciphertext = cipher.doFinal(cleartext);

            return new String(ciphertext, CHARSET);
        } catch (Exception e) {
            throw new EncryptionException(e);
        }
    }

    private SecretKey makeSecretKey()
    {
        return new SecretKeySpec(keySpec.getKey(), "DES");
    }

    @SuppressWarnings("serial")
    public static class EncryptionException extends Exception {
        public EncryptionException(Throwable t)
        {
            super(t);
        }
    }

}
