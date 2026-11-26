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
package org.jkiss.dbeaver.ext.duckdb.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.UIServiceConnectionInit;

import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * DuckDB Data source
 */
public class DuckDBDataSource extends GenericDataSource {
    private static final Pattern ARRAY_TYPE_NAME_PATTERN = Pattern.compile("\\[\\s*\\d*\\s*\\]\\s*$");

    public DuckDBDataSource(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSourceContainer container,
        @NotNull GenericMetaModel metaModel
    ) throws DBException {
        super(monitor, container, metaModel, new DuckDBSQLDialect());
    }

    @Override
    protected void validateConnectionParameters(
        @NotNull DBRProgressMonitor monitor,
        @Nullable String url,
        @NotNull Properties connectProps
    ) throws DBCException {
        List<String> settings = DuckDBExtensionSettings.getUnsafeSettings(url, connectProps);
        if (!settings.isEmpty()) {
            var application = DBWorkbench.getPlatform().getApplication();
            if (application.isHeadlessMode() || application.isMultiuser()) {
                throw new DBCException("DuckDB native extensions require local approval in the desktop application");
            }
            UIServiceConnectionInit service = DBWorkbench.getService(UIServiceConnectionInit.class);
            if (service == null || !service.confirmNativeExtensions(getContainer(), url, settings)) {
                throw new DBCException("DuckDB native extension loading was not approved. Connection cancelled");
            }
        }
    }

    @NotNull
    @Override
    public DBPDataKind resolveDataKind(@NotNull String typeName, int valueType) {
        if (DuckDBConstants.isGeometryType(typeName)) {
            return DBPDataKind.OBJECT;
        } else if (ARRAY_TYPE_NAME_PATTERN.matcher(typeName).find()) {
            return DBPDataKind.ARRAY;
        } else {
            return super.resolveDataKind(typeName, valueType);
        }
    }

    @NotNull
    @Override
    protected DBPDataSourceInfo createDataSourceInfo(@NotNull DBRProgressMonitor monitor, @NotNull JDBCDatabaseMetaData metaData) {
        return new DuckDBDataSourceInfo(getContainer().getDriver(), metaData);
    }

    @NotNull
    @Override
    protected JDBCExecutionContext createExecutionContext(@NotNull JDBCRemoteInstance instance, @NotNull String type) {
        return new DuckDBExecutionContext(instance, type);
    }
}
