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
package org.jkiss.dbeaver.model.impl;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.impl.app.DefaultCertificateStorage;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Assert;
import org.junit.Test;

import java.io.StringReader;
import java.security.SecureRandom;
import java.util.Base64;
public class DefaultCertificateStorageTest extends DBeaverUnitTest {
    @Test
    public void loadDerFromPem() throws Exception {
        final var derKey = makeKey();
        final var pemKey = makePemKey(derKey);

        Assert.assertArrayEquals(derKey, DefaultCertificateStorage.loadDerFromPem(new StringReader(pemKey)));
    }

    @NotNull
    private static byte[] makeKey() {
        final SecureRandom random = new SecureRandom();
        final byte[] key = new byte[1024];

        random.nextBytes(key);

        return key;
    }

    @NotNull
    private static String makePemKey(@NotNull byte[] content) {
        final String key = Base64.getEncoder().encodeToString(content);
        final StringBuilder sb = new StringBuilder(key.length() + 100);

        sb.append("-----BEGIN PRIVATE KEY-----\n");

        for (int i = 0; i < key.length(); i += 64) {
            sb.append(key, i, Math.min(key.length(), i + 64));
            sb.append('\n');
        }

        sb.append("-----END PRIVATE KEY-----\n");

        return sb.toString();
    }
}
