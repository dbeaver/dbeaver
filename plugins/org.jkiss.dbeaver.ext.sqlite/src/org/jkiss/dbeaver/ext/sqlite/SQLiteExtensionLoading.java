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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.sql.DBSQLException;

import java.util.Locale;
import java.util.Properties;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

public final class SQLiteExtensionLoading {
    public static final String ENABLE_LOAD_EXTENSION = "enable_load_extension";
    public static final String PROP_ENABLED = "sqlite-native-extensions";
    public static final String PROP_EXTENSIONS = "sqlite-extensions";

    public static boolean requestEnable(
        @NotNull DBPDataSourceContainer container,
        @NotNull SQLiteExtensionPermissionStore permissions,
        @NotNull Predicate<SQLiteExtensionSettings> confirmation
    ) throws DBException {
        if (container.isTemporary()) {
            return false;
        }
        SQLiteExtensionSettings settings = SQLiteExtensionSettings.captureForLoading(container);
        if (!confirmation.test(settings)) {
            return false;
        }
        if (!settings.equals(SQLiteExtensionSettings.captureForLoading(container))) {
            throw new DBException("SQLite connection settings changed during approval. Try again");
        }
        permissions.approve(settings);
        container.getConnectionConfiguration().setProviderProperty(PROP_ENABLED, Boolean.TRUE.toString());
        container.getRegistry().updateDataSource(container);
        return true;
    }

    public synchronized void validate(
        @NotNull SQLiteExtensionSettings settings,
        @NotNull Properties properties,
        @NotNull SQLiteExtensionPermissionStore permissions,
        @NotNull BooleanSupplier confirmation
    ) throws DBCException {
        properties.setProperty(ENABLE_LOAD_EXTENSION, Boolean.FALSE.toString());
        if (!settings.enabled()) {
            try {
                permissions.revoke(settings);
            } catch (DBException exception) {
                Log.getLog(SQLiteExtensionLoading.class).warn("Cannot revoke SQLite native extension permission", exception);
            }
            return;
        }
        try {
            permissions.invalidate(settings);
            if (!permissions.isApproved(settings)) {
                if (!confirmation.getAsBoolean()) {
                    throw new DBCException("SQLite native extension loading was not approved. Connection cancelled");
                }
                permissions.approve(settings);
            }
            properties.setProperty(ENABLE_LOAD_EXTENSION, Boolean.TRUE.toString());
        } catch (DBCException exception) {
            throw exception;
        } catch (DBException exception) {
            throw new DBCException("Cannot authorize SQLite native extension loading", exception);
        }
    }

    public static boolean isLoadingDenied(@Nullable Throwable error) {
        boolean extensionQuery = false;
        boolean denied = false;
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current instanceof DBSQLException sqlError) {
                extensionQuery |= sqlError.getSqlQuery().toLowerCase(Locale.ENGLISH).contains("load_extension");
            }
            String message = current.getMessage();
            if (message != null) {
                String lower = message.toLowerCase(Locale.ENGLISH);
                denied |= lower.contains("not authorized") || lower.contains("no such function: load_extension");
                extensionQuery |= lower.contains("load_extension");
            }
        }
        return extensionQuery && denied;
    }

    public static boolean isEnabled(@Nullable String url, @NotNull Properties properties) {
        if (properties.containsKey(ENABLE_LOAD_EXTENSION)) {
            return Boolean.parseBoolean(properties.getProperty(ENABLE_LOAD_EXTENSION));
        }
        int delimiter = url == null ? -1 : url.indexOf('?');
        if (delimiter >= 0) {
            String[] parameters = url.substring(delimiter + 1).split("&");
            // xerial gives properties precedence over the URL and reads URL parameters in reverse order
            for (int index = parameters.length - 1; index >= 0; index--) {
                String[] setting = parameters[index].trim().split("=");
                if (setting.length > 1 && ENABLE_LOAD_EXTENSION.equalsIgnoreCase(setting[0].trim())) {
                    String value = setting[1].trim();
                    if (!value.isEmpty()) {
                        return Boolean.parseBoolean(value);
                    }
                }
            }
        }
        return false;
    }
}
