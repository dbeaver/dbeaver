/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.runtime.encode;

import org.jkiss.utils.IOUtils;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * Content encryption/description
 */
public class ContentEncrypter {

    private SecretKey secretKey;
    private Cipher cipher;

    public ContentEncrypter(SecretKey secretKey) {
        this.secretKey = secretKey;
        try {
            this.cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        } catch (Exception e) {
            throw new IllegalStateException("Internal error during encrypted init", e);
        }
    }

    public byte[] encrypt(String content) throws InvalidKeyException, IOException {
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] iv = cipher.getIV();

        ByteArrayOutputStream resultBuffer = new ByteArrayOutputStream();
        try (CipherOutputStream cipherOut = new CipherOutputStream(resultBuffer, cipher)) {
            resultBuffer.write(iv);
            cipherOut.write(content.getBytes(StandardCharsets.UTF_8));
        }
        return resultBuffer.toByteArray();
    }

    public String decrypt(byte[] contents) throws InvalidAlgorithmParameterException, InvalidKeyException, IOException {
        try (InputStream byteStream = new ByteArrayInputStream(contents)) {
            byte[] fileIv = new byte[16];
            byteStream.read(fileIv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(fileIv));

            try (CipherInputStream cipherIn = new CipherInputStream(byteStream, cipher)) {
                ByteArrayOutputStream resultBuffer = new ByteArrayOutputStream();
                IOUtils.copyStream(cipherIn, resultBuffer);
                return new String(resultBuffer.toByteArray(), StandardCharsets.UTF_8);
            }

        }
    }

}