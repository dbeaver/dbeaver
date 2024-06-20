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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.DBCFeatureNotSupportedException;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.secret.DBSSecretController;
import org.jkiss.dbeaver.model.secret.DBSSecretObject;
import org.jkiss.dbeaver.model.secret.DBSSecretValue;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.io.IOException;
import java.util.List;

/**
 * Default local secret controller.
 * <p>
 * Uses Eclipse preferences to read/write secrets. Doesn't encrypt values.
 */
public class LocalSecretController implements DBSSecretController {
    public static final LocalSecretController INSTANCE = new LocalSecretController();

    private final DBPPreferenceStore preferenceStore = DBWorkbench.getPlatform().getPreferenceStore();

    @Nullable
    @Override
    public String getPrivateSecretValue(@NotNull String secretId) {
        return preferenceStore.getString(makeKey(secretId));
    }

    @Override
    public void setPrivateSecretValue(@NotNull String secretId, @Nullable String secretValue) throws DBException {
        String key = makeKey(secretId);
        if (secretValue != null) {
            preferenceStore.setValue(key, secretValue);
        } else {
            preferenceStore.setToDefault(key);
        }
    }

    @NotNull
    @Override
    public List<DBSSecretValue> discoverCurrentUserSecrets(@NotNull DBSSecretObject secretObject) throws DBException {
        throw new DBCFeatureNotSupportedException("Secrets discovery not supported");
    }

    @Override
    public void flushChanges() throws DBException {
        try {
            preferenceStore.save();
        } catch (IOException e) {
            throw new DBException("Error flushing secure preferences", e);
        }
    }

    @NotNull
    private static String makeKey(@NotNull String secretId) {
        return "secrets/" + secretId;
    }
}