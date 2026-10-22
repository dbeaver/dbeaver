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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.secret.DBSSecretController;

import java.util.Collection;
import java.util.List;

class DDDesktopTokenStore {
    private static final String SECRET_ID = "datadam.desktop.access-tokens";
    static final DDDesktopTokenStore INSTANCE = new DDDesktopTokenStore();

    private DDDesktopTokenStore() {
    }

    @NotNull
    List<DDDesktopUserToken> load() throws DBException {
        String value = DBSSecretController.getGlobalSecretController().getPrivateSecretValue(SECRET_ID);
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            return List.of(JSONUtils.GSON.fromJson(value, DDDesktopUserToken[].class));
        } catch (RuntimeException e) {
            throw new DBException("Invalid saved DataDam desktop token", e);
        }
    }

    void save(@NotNull Collection<DDDesktopUserToken> tokens) throws DBException {
        DBSSecretController controller = DBSSecretController.getGlobalSecretController();
        controller.setPrivateSecretValue(SECRET_ID, JSONUtils.GSON.toJson(tokens));
        controller.flushChanges();
    }

    void clear() throws DBException {
        DBSSecretController controller = DBSSecretController.getGlobalSecretController();
        controller.setPrivateSecretValue(SECRET_ID, null);
        controller.flushChanges();
    }

}
