/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.datadam.auth;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

class DDBundleCredentialsTest {
    @Test
    void legacyBundleStillProvidesEncryptionKeyWithoutSigningRequests() throws Exception {
        KeyGenerator dataKeyGenerator = KeyGenerator.getInstance("AES");
        dataKeyGenerator.init(256);
        SecretKey dataKey = dataKeyGenerator.generateKey();
        DDBundleCredentials credentials = new DDBundleCredentials(new DDKeyBundle(
            "73ce9dfa-05ad-40f3-802a-bc32e256b737",
            "unused-signing-key",
            Base64.getEncoder().encodeToString(dataKey.getEncoded()),
            1
        ));

        Assertions.assertArrayEquals(dataKey.getEncoded(), credentials.getDataKey().getEncoded());
    }
}
