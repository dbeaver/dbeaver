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
package org.jkiss.dbeaver.ext.sqlite.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericExecutionContext;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.ext.sqlite.SQLiteExtensionConfirmation;
import org.jkiss.dbeaver.ext.sqlite.SQLiteExtensionLoading;
import org.jkiss.dbeaver.ext.sqlite.SQLiteExtensionLoadingException;
import org.jkiss.dbeaver.ext.sqlite.SQLiteExtensionPermissionStore;
import org.jkiss.dbeaver.ext.sqlite.SQLiteExtensionSettings;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

public class SQLiteDataSource extends GenericDataSource {

    private SQLiteExtensionLoading extensionLoading;

    public SQLiteDataSource(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSourceContainer container,
        @NotNull GenericMetaModel metaModel
    ) throws DBException {
        super(monitor, container, metaModel, new SQLiteSQLDialect());

    }

    public SQLiteDataSource(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSourceContainer container,
        @NotNull GenericMetaModel metaModel,
        @NotNull SQLDialect dialect
    ) throws DBException {
        super(monitor, container, metaModel, dialect);
    }


    @NotNull
    @Override
    protected JDBCExecutionContext createExecutionContext(@NotNull JDBCRemoteInstance instance, @NotNull String type) {
        return new GenericExecutionContext(instance, type) {
            @Override
            protected boolean initContextBootstrap(@NotNull DBRProgressMonitor monitor, boolean autoCommit) throws DBException {
                try {
                    return super.initContextBootstrap(monitor, autoCommit);
                } catch (DBException exception) {
                    if (SQLiteExtensionLoading.isLoadingDenied(exception)) {
                        throw new SQLiteExtensionLoadingException(exception, this);
                    }
                    throw exception;
                }
            }
        };
    }

    @NotNull
    @Override
    protected DBPDataSourceInfo createDataSourceInfo(@NotNull DBRProgressMonitor monitor, @NotNull JDBCDatabaseMetaData metaData) {
        return new SQLiteDataSourceInfo(container.getDriver(), metaData);
    }

    @Override
    protected boolean isPopulateClientAppName() {
        return false;
    }

    @Nullable
    @Override
    public DBSDataType getLocalDataType(@Nullable String typeName) {
        if (typeName == null) {
            return super.getLocalDataType(defaultAffinity().name());
        }
        // Resolve type name according to https://www.sqlite.org/datatype3.html
        // except for the GUID type name considered as BLOB to allow its conversion to UUID
        typeName = typeName.toUpperCase(Locale.ENGLISH);
        SQLiteAffinity affinity;
        if (typeName.startsWith(SQLConstants.DATA_TYPE_INT)) {
            affinity = SQLiteAffinity.INTEGER;
        } else if (typeName.contains("CHAR") || typeName.contains("CLOB") || typeName.contains("TEXT") || typeName.startsWith("DATE") || typeName.startsWith("TIME")) {
            affinity = SQLiteAffinity.TEXT;
        } else if (typeName.contains("BLOB") || typeName.contains("GUID")) {
            affinity = SQLiteAffinity.BLOB;
        } else if (typeName.startsWith("REAL") || typeName.startsWith("FLOA") || typeName.startsWith("DOUB")) {
            affinity = SQLiteAffinity.REAL;
        } else if (typeName.contains(SQLConstants.DATA_TYPE_INT) || typeName.contains("NUMERIC") || typeName.contains("DECIMAL") ||
            typeName.contains("BOOL")) {
            affinity = SQLiteAffinity.NUMERIC;
        } else {
            affinity = defaultAffinity();
        }
        return super.getLocalDataType(affinity.name());
    }

    private static SQLiteAffinity defaultAffinity() {
        // If type is unknown, let's assume it's a text. Otherwise, search and data editor doesn't work right.
        return SQLiteAffinity.TEXT;
    }

    @Override
    protected boolean isConnectionReadOnlyBroken() {
        return true;
    }

    @NotNull
    @Override
    protected Map<String, String> getInternalConnectionProperties(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDriver driver,
        @Nullable JDBCExecutionContext context,
        @NotNull String purpose,
        @NotNull DBPConnectionConfiguration connectionInfo
    ) throws DBCException {
        Map<String, String> connectionsProps = new HashMap<>();
        if (getContainer().isConnectionReadOnly()) {
            // Read-only prop
            connectionsProps.put("open_mode", "1");  //1 == readonly
        }
        String enabled = connectionInfo.getProviderProperty(SQLiteExtensionLoading.PROP_ENABLED);
        if (enabled != null) {
            connectionsProps.put(SQLiteExtensionLoading.ENABLE_LOAD_EXTENSION, enabled);
        } else if (CommonUtils.isNotEmpty(
            connectionInfo.getProviderProperty(SQLiteExtensionLoading.PROP_EXTENSIONS))) {
            connectionsProps.put(SQLiteExtensionLoading.ENABLE_LOAD_EXTENSION, Boolean.TRUE.toString());
        }
        return connectionsProps;
    }

    @Override
    protected void validateConnectionParameters(
        @NotNull DBRProgressMonitor monitor,
        @Nullable String url,
        @NotNull Properties connectProps
    ) throws DBCException {
        SQLiteExtensionSettings settings = SQLiteExtensionSettings.capture(getContainer(), url, connectProps);
        var application = DBWorkbench.getPlatform().getApplication();
        if (settings.enabled() && (application.isHeadlessMode() || application.isMultiuser())) {
            throw new DBCException("SQLite native extensions require local approval in the desktop application");
        }
        getExtensionLoading().validate(settings, connectProps, SQLiteExtensionPermissionStore.getInstance(), () -> {
            SQLiteExtensionConfirmation confirmation = DBWorkbench.getService(SQLiteExtensionConfirmation.class);
            return confirmation != null && confirmation.confirmExtensionLoading(settings, true);
        });
    }

    @NotNull
    private synchronized SQLiteExtensionLoading getExtensionLoading() {
        // generic datasource opens its first connection from the superclass constructor
        if (extensionLoading == null) {
            extensionLoading = new SQLiteExtensionLoading();
        }
        return extensionLoading;
    }

    @NotNull
    @Override
    public Class<? extends DBSObject> getPrimaryChildType(@Nullable DBRProgressMonitor monitor) throws DBException {
        return SQLiteTable.class;
    }

    @NotNull
    @Override
    public ErrorType discoverErrorType(@NotNull Throwable error) {
        if (error instanceof SQLException && ((SQLException) error).getErrorCode() == 19) {
            // SQLite doesn't have a specific Exception SQL state for the constraint violation, but it has a specific vendor code
            return ErrorType.UNIQUE_KEY_VIOLATION;
        }
        return super.discoverErrorType(error);
    }

    public boolean supportsStrictTyping() {
        return isServerVersionAtLeast(3, 37);
    }
}
