/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry.encode;

import net.sf.jkiss.utils.Base64;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
 * Encryption util
 */
public class SimpleStringEncrypter implements PasswordEncrypter {

    //public static final String SCHEME_DES = "DES";
    private static final byte[] PASSWORD_ENCRYPTION_KEY = "sdf@!#$verf^wv%6Fwe%$$#FFGwfsdefwfe135s$^H)dg".getBytes();

    private static final String CHARSET = "UTF8";

    //private String encryptionKey;
    //private DESKeySpec keySpec;
    //private Cipher cipher;

    public SimpleStringEncrypter()
    {
/*
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
*/

    }

    public String encrypt(String unencryptedString) throws EncryptionException
    {
        if (unencryptedString == null || unencryptedString.trim().length() == 0) {
            throw new IllegalArgumentException("Empty string");
        }

        try {
            byte[] stringBytes = unencryptedString.getBytes(CHARSET);
            byte[] plainBytes = Arrays.copyOf(stringBytes, stringBytes.length + 2);
            plainBytes[plainBytes.length - 2] = 0;
            plainBytes[plainBytes.length - 1] = -127;
            xorStringByKey(plainBytes);
            return Base64.encode(plainBytes);
/*
            SecretKey key = makeSecretKey();//keyFactory.generateSecret(keySpec);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] cleartext = unencryptedString.getBytes(CHARSET);
            byte[] ciphertext = cipher.doFinal(cleartext);

            return Base64.encode(ciphertext);
*/
        } catch (Exception e) {
            throw new EncryptionException(e);
        }
    }

    private void xorStringByKey(byte[] plainBytes) throws UnsupportedEncodingException
    {
        int keyOffset = 0;
        for (int i = 0; i < plainBytes.length; i++) {
            byte keyChar = PASSWORD_ENCRYPTION_KEY[keyOffset];
            keyOffset++;
            if (keyOffset >= PASSWORD_ENCRYPTION_KEY.length) {
                keyOffset = 0;
            }
            plainBytes[i] ^= keyChar;
        }
    }

    public String decrypt(String encryptedString) throws EncryptionException
    {
        if (encryptedString == null || encryptedString.trim().length() <= 0) {
            throw new IllegalArgumentException("Empty encrypted string");
        }

        try {
            byte[] cleartext = Base64.decode(encryptedString);
            xorStringByKey(cleartext);
            if (cleartext[cleartext.length - 2] != 0 || cleartext[cleartext.length - 1] != -127) {
                throw new EncryptionException("Invalid encrypted string");
            }
            return new String(cleartext, 0, cleartext.length - 2, CHARSET);
/*
            SecretKey key = makeSecretKey();
            cipher.init(Cipher.DECRYPT_MODE, key*/
/*, SecureRandom.getInstance("SHA1PRNG")*//*
);
            byte[] cleartext = Base64.decode(encryptedString);
            byte[] ciphertext = cipher.doFinal(cleartext);

            return new String(ciphertext, CHARSET);
*/
        } catch (Exception e) {
            throw new EncryptionException(e);
        }
    }

/*
    private SecretKey makeSecretKey()
    {
        return new SecretKeySpec(keySpec.getKey(), "DES");
    }
*/

}
