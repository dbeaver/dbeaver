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
package org.jkiss.dbeaver.ext.sqlite;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.preferences.ConfirmedActionsStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SQLiteExtensionPermissionStore {
    private static final String PERMISSIONS_FILE_NAME = "sqlite-extension-permissions.json";
    private static final int MAX_TEMPORARY_PERMISSIONS = 100;
    private static SQLiteExtensionPermissionStore instance;

    private final ConfirmedActionsStore actions;
    private final Map<String, String> temporaryPermissions = new LinkedHashMap<>();

    SQLiteExtensionPermissionStore(@NotNull Path path) {
        actions = new ConfirmedActionsStore(path);
    }

    @NotNull
    public static synchronized SQLiteExtensionPermissionStore getInstance() {
        if (instance == null) {
            instance = new SQLiteExtensionPermissionStore(
                DBWorkbench.getPlatform().getGlobalConfigurationFile(PERMISSIONS_FILE_NAME));
        }
        return instance;
    }

    public synchronized boolean isApproved(@NotNull SQLiteExtensionSettings settings) throws DBException {
        return settings.enabled() && ((settings.temporary()
            && settings.fingerprint().equals(temporaryPermissions.get(settings.temporaryKey())))
            || actions.contains(approval(settings)));
    }

    public synchronized void approve(@NotNull SQLiteExtensionSettings settings) throws DBException {
        if (!settings.enabled()) {
            revoke(settings);
        } else if (settings.temporary()) {
            temporaryPermissions.put(settings.temporaryKey(), settings.fingerprint());
            if (temporaryPermissions.size() > MAX_TEMPORARY_PERMISSIONS) {
                temporaryPermissions.remove(temporaryPermissions.keySet().iterator().next());
            }
        } else {
            actions.replaceWithPrefix(settings.connectionKey() + ":", approval(settings));
        }
    }

    public synchronized void invalidate(@NotNull SQLiteExtensionSettings settings) throws DBException {
        if (!isApproved(settings)) {
            revoke(settings);
        }
    }

    public synchronized void revoke(@NotNull SQLiteExtensionSettings settings) throws DBException {
        if (settings.temporary()) {
            temporaryPermissions.remove(settings.temporaryKey());
        } else {
            actions.replaceWithPrefix(settings.connectionKey() + ":", null);
        }
    }

    @NotNull
    private static String approval(@NotNull SQLiteExtensionSettings settings) {
        return settings.connectionKey() + ":" + settings.fingerprint();
    }
}
