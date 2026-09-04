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
package org.jkiss.dbeaver.ext.iotdb.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericExecutionContext;
import org.jkiss.dbeaver.ext.generic.model.GenericObjectContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.iotdb.model.meta.IoTDBTableMetaModel;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPConnectionBootstrap;
import org.jkiss.dbeaver.model.exec.DBCCachedContextDefaults;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;

/**
 * IoTDB execution context.
 *
 * IoTDB's table-dialect JDBC implementation turns {@code setSchema(name)} into
 * a parameterized {@code USE ?} statement. JDBC string binding then produces a
 * string literal, while IoTDB requires an identifier. Execute {@code USE}
 * directly for table-dialect connections and keep the generic behavior for the
 * tree dialect.
 */
class IoTDBExecutionContext extends GenericExecutionContext {
    private static final Log log = Log.getLog(IoTDBExecutionContext.class);

    @Nullable
    private String activeDatabaseName;

    IoTDBExecutionContext(@NotNull JDBCRemoteInstance instance, @NotNull String purpose) {
        super(instance, purpose);
    }

    @NotNull
    @Override
    public IoTDBDataSource getDataSource() {
        return (IoTDBDataSource) super.getDataSource();
    }

    @Nullable
    @Override
    public GenericSchema getDefaultSchema() {
        if (!isTableModel()) {
            return super.getDefaultSchema();
        }
        return CommonUtils.isEmpty(activeDatabaseName) ? null : getDataSource().getSchema(activeDatabaseName);
    }

    @Nullable
    @Override
    public GenericObjectContainer getDefaultObject() {
        return isTableModel() ? getDefaultSchema() : super.getDefaultObject();
    }

    @Override
    public boolean supportsSchemaChange() {
        return isTableModel() || super.supportsSchemaChange();
    }

    @Override
    public void setDefaultSchema(
        @NotNull DBRProgressMonitor monitor,
        @Nullable GenericSchema schema
    ) throws DBCException {
        if (!isTableModel()) {
            super.setDefaultSchema(monitor, schema);
            return;
        }
        if (schema == null) {
            log.debug("Null current schema");
            return;
        }

        GenericSchema oldSchema = getDefaultSchema();
        setActiveDatabase(monitor, schema.getName());
        activeDatabaseName = schema.getName();
        DBUtils.fireObjectSelectionChange(oldSchema, schema, this);
    }

    @Override
    public void initDefaultsFrom(
        @NotNull DBRProgressMonitor monitor,
        @NotNull GenericExecutionContext context
    ) throws DBCException {
        if (!isTableModel()) {
            super.initDefaultsFrom(monitor, context);
            return;
        }

        String databaseName = context instanceof IoTDBExecutionContext iotdbContext
            ? iotdbContext.activeDatabaseName
            : context.getDefaultSchemaCached();
        if (CommonUtils.isNotEmpty(databaseName)) {
            setActiveDatabase(monitor, databaseName);
            activeDatabaseName = databaseName;
        }
    }

    @Override
    public boolean refreshDefaults(@NotNull DBRProgressMonitor monitor, boolean useBootstrapSettings) throws DBException {
        if (!isTableModel()) {
            return super.refreshDefaults(monitor, useBootstrapSettings);
        }

        String oldDatabaseName = activeDatabaseName;
        GenericSchema oldSchema = getDefaultSchema();

        if (useBootstrapSettings) {
            DBPConnectionBootstrap bootstrap = getBootstrapSettings();
            if (CommonUtils.isNotEmpty(bootstrap.getDefaultSchemaName())) {
                setActiveDatabase(monitor, bootstrap.getDefaultSchemaName());
                activeDatabaseName = bootstrap.getDefaultSchemaName();
            }
        }

        try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.META, "Determine active database")) {
            String schemaName = session.getSchema();
            if (CommonUtils.isNotEmpty(schemaName)) {
                activeDatabaseName = schemaName;
            }
        } catch (SQLException e) {
            log.debug("Unable to determine the active IoTDB database", e);
        }

        if (!CommonUtils.equalObjects(oldDatabaseName, activeDatabaseName)) {
            GenericSchema newSchema = getDefaultSchema();
            if (newSchema != null) {
                DBUtils.fireObjectSelectionChange(oldSchema, newSchema, this);
            }
            return true;
        }
        return false;
    }

    @Nullable
    @Override
    public String getDefaultSchemaCached() {
        return isTableModel() ? activeDatabaseName : super.getDefaultSchemaCached();
    }

    @NotNull
    @Override
    public DBCCachedContextDefaults getCachedDefault() {
        if (!isTableModel()) {
            return super.getCachedDefault();
        }
        return new DBCCachedContextDefaults(getDefaultCatalogCached(), activeDatabaseName);
    }

    private void setActiveDatabase(@NotNull DBRProgressMonitor monitor, @NotNull String databaseName) throws DBCException {
        try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.UTIL, TASK_TITLE_SET_SCHEMA)) {
            try (JDBCStatement statement = session.createStatement()) {
                statement.execute("USE " + DBUtils.getQuotedIdentifier(getDataSource(), databaseName));
            }
        } catch (SQLException e) {
            throw new DBCException(e, this);
        }
    }

    private boolean isTableModel() {
        // The data source's isTree field is initialized after the generic constructor opens this context.
        return getDataSource().getMetaModel() instanceof IoTDBTableMetaModel;
    }
}
