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

package org.jkiss.dbeaver.ext.dameng.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPConnectionBootstrap;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;

/**
 * @author Shengkai Bai
 */
public class DamengExecutionContext extends JDBCExecutionContext implements DBCExecutionContextDefaults<DBSCatalog, DamengSchema> {

    private String activeSchemaName;

    public DamengExecutionContext(JDBCRemoteInstance instance, String purpose) {
        super(instance, purpose);
    }

    @Override
    public DBSCatalog getDefaultCatalog() {
        return null;
    }

    @Override
    public DamengSchema getDefaultSchema() {
        return activeSchemaName == null ? null : (DamengSchema) getDataSource().getSchemaCache().getCachedObject(activeSchemaName);
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
    public void setDefaultCatalog(DBRProgressMonitor monitor, DBSCatalog catalog, DamengSchema schema) throws DBCException {
        throw new DBCFeatureNotSupportedException();
    }

    @Override
    public void setDefaultSchema(DBRProgressMonitor monitor, DamengSchema schema) throws DBCException {
        final DamengSchema oldSelectedEntity = getDefaultSchema();
        if (schema == null || oldSelectedEntity == schema) {
            return;
        }
        setCurrentSchema(monitor, schema.getName());
        activeSchemaName = schema.getName();
    }

    @Override
    public boolean refreshDefaults(DBRProgressMonitor monitor, boolean useBootstrapSettings) throws DBException {
        try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.META, "Query active schema")) {
            if (useBootstrapSettings) {
                DBPConnectionBootstrap bootstrap = getBootstrapSettings();
                String bootstrapSchemaName = bootstrap.getDefaultSchemaName();
                if (!CommonUtils.isEmpty(bootstrapSchemaName) && !bootstrapSchemaName.equals(activeSchemaName)) {
                    setCurrentSchema(monitor, bootstrap.getDefaultSchemaName());
                }
            }
            this.activeSchemaName = JDBCUtils.queryString(session, "SELECT SYS_CONTEXT( 'USERENV', 'CURRENT_SCHEMA' ) FROM DUAL");
        } catch (Exception e) {
            throw new DBCException(e, this);
        }

        return true;
    }

    @Override
    public DamengDataSource getDataSource() {
        return (DamengDataSource) super.getDataSource();
    }

    @Override
    public DamengExecutionContext getContextDefaults() {
        return this;
    }

    private void setCurrentSchema(DBRProgressMonitor monitor, String activeSchemaName) throws DBCException {
        DBSObject oldDefaultSchema = getDefaultSchema();
        try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.UTIL, TASK_TITLE_SET_SCHEMA)) {
            JDBCUtils.executeSQL(session, "SET SCHEMA " + DBUtils.getQuotedIdentifier(session.getDataSource(), activeSchemaName));
            this.activeSchemaName = activeSchemaName;
            DBSObject newDefaultSchema = getDefaultSchema();
            DBUtils.fireObjectSelectionChange(oldDefaultSchema, newDefaultSchema, this);
        } catch (SQLException e) {
            throw new DBCException(e, this);
        }
    }

    @NotNull
    @Override
    public DBCCachedContextDefaults getCachedDefault() {
        return new DBCCachedContextDefaults(null, activeSchemaName);
    }
}
