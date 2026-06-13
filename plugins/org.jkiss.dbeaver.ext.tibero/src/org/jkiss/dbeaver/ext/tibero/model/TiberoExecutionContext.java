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
package org.jkiss.dbeaver.ext.tibero.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPConnectionBootstrap;
import org.jkiss.dbeaver.model.exec.DBCCachedContextDefaults;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;

public class TiberoExecutionContext extends JDBCExecutionContext implements DBCExecutionContextDefaults<DBSCatalog, TiberoSchema> {

    private static final Log log = Log.getLog(TiberoExecutionContext.class);

    private String activeSchemaName;

    TiberoExecutionContext(@NotNull JDBCRemoteInstance instance, String purpose) {
        super(instance, purpose);
    }

    @NotNull
    @Override
    public TiberoDataSource getDataSource() {
        return (TiberoDataSource) super.getDataSource();
    }

    @NotNull
    @Override
    public TiberoExecutionContext getContextDefaults() {
        return this;
    }

    @Override
    public DBSCatalog getDefaultCatalog() {
        return null;
    }

    @Override
    public TiberoSchema getDefaultSchema() {
        try {
            return activeSchemaName == null ? null : (TiberoSchema) getDataSource().getChild(
                new VoidProgressMonitor(),
                activeSchemaName);
        } catch (Exception e) {
            log.error(e);
            return null;
        }
    }

    @Override
    public boolean supportsCatalogChange() {
        return false;
    }

    @Override
    public boolean supportsSchemaChange() {
        return true;
    }

    @Override
    public void setDefaultCatalog(DBRProgressMonitor monitor, DBSCatalog catalog, TiberoSchema schema) throws DBCException {
        throw new DBCException("Catalog change is not supported");
    }

    @Override
    public void setDefaultSchema(DBRProgressMonitor monitor, TiberoSchema schema) throws DBCException {
        final TiberoSchema oldDefaultSchema = getDefaultSchema();
        if (schema == null || oldDefaultSchema == schema) {
            return;
        }
        setCurrentSchema(monitor, schema);
    }

    @Override
    public boolean refreshDefaults(DBRProgressMonitor monitor, boolean useBootstrapSettings) throws DBException {
        try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.META, "Query Tibero active schema")) {
            if (useBootstrapSettings) {
                DBPConnectionBootstrap bootstrap = getBootstrapSettings();
                String bootstrapSchemaName = bootstrap.getDefaultSchemaName();
                if (!CommonUtils.isEmpty(bootstrapSchemaName) && !bootstrapSchemaName.equals(activeSchemaName)) {
                    setCurrentSchema(session, bootstrapSchemaName);
                }
            }

            this.activeSchemaName = getCurrentSchema(session);
            if (CommonUtils.isEmpty(this.activeSchemaName)) {
                this.activeSchemaName = null;
            }
        } catch (SQLException e) {
            throw new DBCException(e, this);
        }
        return true;
    }

    void setCurrentSchema(DBRProgressMonitor monitor, TiberoSchema schema) throws DBCException {
        if (schema == null) {
            log.debug("Null current schema");
            return;
        }
        setCurrentSchema(monitor, schema.getName());
    }

    private void setCurrentSchema(DBRProgressMonitor monitor, String schemaName) throws DBCException {
        DBSObject oldDefaultSchema = getDefaultSchema();
        try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.UTIL, TASK_TITLE_SET_SCHEMA)) {
            setCurrentSchema(session, schemaName);
            this.activeSchemaName = schemaName;
            DBSObject newDefaultSchema = getDefaultSchema();
            DBUtils.fireObjectSelectionChange(oldDefaultSchema, newDefaultSchema, this);
        } catch (SQLException e) {
            throw new DBCException(e, this);
        }
    }

    private void setCurrentSchema(JDBCSession session, String schemaName) throws SQLException {
        JDBCUtils.executeSQL(session,
            "ALTER SESSION SET CURRENT_SCHEMA=" + DBUtils.getQuotedIdentifier(session.getDataSource(), schemaName));
    }

    private String getCurrentSchema(JDBCSession session) throws SQLException {
        return JDBCUtils.queryString(session, "SELECT SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') FROM DUAL");
    }

    @NotNull
    @Override
    public DBCCachedContextDefaults getCachedDefault() {
        return new DBCCachedContextDefaults(null, activeSchemaName);
    }
}
