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
package org.jkiss.dbeaver.model.datadam.auth.desktop;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.datadam.auth.DDSecretStorage;
import org.jkiss.dbeaver.model.secret.DBSSecretController;

class DDDesktopTokenStore {
    private static final String SECRET_ID = "datadam.desktop.access-token";
    static final DDDesktopTokenStore INSTANCE = new DDDesktopTokenStore(DDSecretStorage.getInstance());

    private final DBSSecretController secretStorage;

    DDDesktopTokenStore(@NotNull DBSSecretController secretStorage) {
        this.secretStorage = secretStorage;
    }

    @Nullable
    DDDesktopUserToken load() throws DBException {
        String value = secretStorage.getPrivateSecretValue(SECRET_ID);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return JSONUtils.GSON.fromJson(value, DDDesktopUserToken.class);
        } catch (RuntimeException e) {
            throw new DBException("Invalid saved DataDam desktop token", e);
        }
    }

    void save(@NotNull DDDesktopUserToken token) throws DBException {
        secretStorage.setPrivateSecretValue(SECRET_ID, JSONUtils.GSON.toJson(token));
        secretStorage.flushChanges();
    }

    void clear() throws DBException {
        secretStorage.setPrivateSecretValue(SECRET_ID, null);
        secretStorage.flushChanges();
    }

}
