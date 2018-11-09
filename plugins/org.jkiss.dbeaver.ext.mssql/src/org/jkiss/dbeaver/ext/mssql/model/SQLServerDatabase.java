/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.mssql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.mssql.SQLServerConstants;
import org.jkiss.dbeaver.ext.mssql.SQLServerUtils;
import org.jkiss.dbeaver.ext.mssql.model.generic.SQLServerGenericSchema;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
* SQL Server database
*/
public class SQLServerDatabase implements DBSCatalog, DBPSaveableObject, DBPRefreshableObject, DBPSystemObject{

    private final SQLServerDataSource dataSource;
    private boolean persisted;
    private String name;
    private SchemaCache schemaCache = new SchemaCache();

    SQLServerDatabase(SQLServerDataSource dataSource, JDBCResultSet resultSet) {
        this.dataSource = dataSource;
        this.name = JDBCUtils.safeGetString(resultSet, "name");

        this.persisted = true;
    }

    @Override
    public SQLServerDataSource getDataSource() {
        return dataSource;
    }

    @Override
    @Property(viewable = true, editable = true, order = 1)
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public DBSObject getParentObject() {
        return dataSource;
    }

    @Override
    public boolean isPersisted() {
        return this.persisted;
    }

    @Override
    public void setPersisted(boolean persisted) {
        this.persisted = persisted;
    }

    @Override
    public boolean isSystem() {
        return name.equals("msdb");
    }

    @Override
    public DBSObject refreshObject(DBRProgressMonitor monitor) throws DBException {
        return this;
    }

    //////////////////////////////////////////////////
    // Schemas

    @Association
    public Collection<SQLServerSchema> getSchemas(DBRProgressMonitor monitor) throws DBException {
        return getChildren(monitor);
    }


    @Override
    public Collection<SQLServerSchema> getChildren(DBRProgressMonitor monitor) throws DBException {
        return schemaCache.getAllObjects(monitor, this);
    }

    @Override
    public DBSObject getChild(DBRProgressMonitor monitor, String childName) throws DBException {
        return schemaCache.getObject(monitor, this, childName);
    }

    @Override
    public Class<? extends DBSObject> getChildType(DBRProgressMonitor monitor) throws DBException {
        return SQLServerSchema.class;
    }

    @Override
    public void cacheStructure(DBRProgressMonitor monitor, int scope) throws DBException {
        schemaCache.getAllObjects(monitor, this);
    }


    static class SchemaCache extends JDBCObjectCache<SQLServerDatabase, SQLServerSchema> {
        SchemaCache() {
            setListOrderComparator(DBUtils.nameComparatorIgnoreCase());
        }

        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull SQLServerDatabase owner) throws SQLException {
            SQLServerDataSource dataSource = owner.getDataSource();
            boolean showAllSchemas = SQLServerUtils.isShowAllSchemas(dataSource);

            String sysSchema = SQLServerUtils.getSystemSchemaFQN(dataSource, owner.getName(), SQLServerConstants.SQL_SERVER_SYSTEM_SCHEMA);
            StringBuilder sql = new StringBuilder();
            if (showAllSchemas) {
                if (dataSource.isServerVersionAtLeast(SQLServerConstants.SQL_SERVER_2005_VERSION_MAJOR ,0)) {
                    sql.append("SELECT * FROM ").append(sysSchema).append(".schemas");
                } else {
                    sql.append("SELECT * FROM ").append(sysSchema).append(".sysusers");
                }
            } else {
                sql.append("SELECT * FROM ").append(sysSchema).append(".schemas s WHERE EXISTS (SELECT 1 FROM ")
                    .append(sysSchema).append(".sysobjects o ").append("WHERE s.schema_id=o.uid)");
            }
            final DBSObjectFilter schemaFilters = dataSource.getContainer().getObjectFilter(SQLServerSchema.class, owner, false);
            if (schemaFilters != null && schemaFilters.isEnabled()) {
                JDBCUtils.appendFilterClause(sql, schemaFilters, "name", false);
            }

            return session.prepareStatement(sql.toString());
        }

        @Override
        protected SQLServerSchema fetchObject(@NotNull JDBCSession session, @NotNull SQLServerDatabase owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new SQLServerSchema(owner, resultSet);
        }

    }

}
