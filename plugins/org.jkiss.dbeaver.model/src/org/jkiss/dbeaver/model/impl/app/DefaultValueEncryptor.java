/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.model.impl.app;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.secret.DBSValueEncryptor;
import org.jkiss.utils.IOUtils;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Default value encryptor.
 *
 * Uses Eclipse secure preferences to read/write secrets.
 */
public class DefaultValueEncryptor implements DBSValueEncryptor {

    public static final String CIPHER_NAME = "AES/CBC/PKCS5Padding";
    public static final String KEY_ALGORITHM = "AES";

    private final SecretKey secretKey;
    private final Cipher cipher;

    public DefaultValueEncryptor(SecretKey secretKey) {
        this.secretKey = secretKey;
        try {
            this.cipher = Cipher.getInstance(CIPHER_NAME);
        } catch (Exception e) {
            throw new IllegalStateException("Internal error during encrypted init", e);
        }
    }

    public static SecretKey makeSecretKeyFromPassword(String password) {
/*
        UUID projectID = getProjectID();
        ByteBuffer bb = ByteBuffer.wrap(new byte[8]);
        bb.putLong(projectID.getMostSignificantBits());
        byte[] salt = bb.array();
        PBEParameterSpec pbeParamSpec = new PBEParameterSpec(salt, 20);
*/

        //PBEKeySpec spec = new PBEKeySpec(password.toCharArray());
        byte[] bytes = password.getBytes(StandardCharsets.UTF_8);
        byte[] passBytes = Arrays.copyOf(bytes, 16);
        return new SecretKeySpec(passBytes, KEY_ALGORITHM);

/*
        try {
            return SecretKeyFactory.getInstance("AES").generateSecret(spec);
        } catch (Throwable e) {
            log.error("Error generating secret key for password", e);
            return null;
        }
*/
    }

    @NotNull
    @Override
    public byte[] encryptValue(@NotNull byte[] value) throws DBException {
        try {
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] iv = cipher.getIV();

            ByteArrayOutputStream resultBuffer = new ByteArrayOutputStream();
            try (CipherOutputStream cipherOut = new CipherOutputStream(resultBuffer, cipher)) {
                resultBuffer.write(iv);
                cipherOut.write(value);
            }
            return resultBuffer.toByteArray();
        } catch (Exception e) {
            throw new DBException("Error encrypting value", e);
        }
    }

    @NotNull
    @Override
    public byte[] decryptValue(@NotNull byte[] value) throws DBException {
        try (InputStream byteStream = new ByteArrayInputStream(value)) {
            byte[] fileIv = new byte[16];
            byteStream.read(fileIv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(fileIv));

            try (CipherInputStream cipherIn = new CipherInputStream(byteStream, cipher)) {
                ByteArrayOutputStream resultBuffer = new ByteArrayOutputStream();
                IOUtils.copyStream(cipherIn, resultBuffer);
                return resultBuffer.toByteArray();
            }

        } catch (Exception e) {
            throw new DBException("Error decrypting value", e);
        }
    }

}