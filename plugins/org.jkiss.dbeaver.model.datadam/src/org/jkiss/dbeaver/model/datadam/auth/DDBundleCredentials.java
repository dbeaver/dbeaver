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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.datadam.sync.core.DDSyncCredentials;

import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class DDBundleCredentials implements DDSyncCredentials {

    private static final String DATA_KEY_ALGORITHM = "AES";

    private final DDKeyBundle bundle;

    public DDBundleCredentials(@NotNull DDKeyBundle bundle) {
        this.bundle = bundle;
    }

    @NotNull
    @Override
    public SecretKey getDataKey() throws DBException {
        try {
            return new SecretKeySpec(Base64.getDecoder().decode(bundle.dataKey()), DATA_KEY_ALGORITHM);
        } catch (IllegalArgumentException e) {
            throw new DBException("Invalid data key in the bundle", e);
        }
    }
}
