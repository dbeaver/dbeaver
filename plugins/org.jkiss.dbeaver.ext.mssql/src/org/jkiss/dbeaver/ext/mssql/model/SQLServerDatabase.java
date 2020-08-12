/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mssql.SQLServerConstants;
import org.jkiss.dbeaver.ext.mssql.SQLServerUtils;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.DBPSystemObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectLookupCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.LongKeyMap;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

/**
* SQL Server database
*/
public class SQLServerDatabase implements DBSCatalog, DBPSaveableObject, DBPRefreshableObject, DBPSystemObject {

    private static final Log log = Log.getLog(SQLServerDatabase.class);

    private final SQLServerDataSource dataSource;
    private boolean persisted;
    private String name;
    private String description;
    private DataTypeCache typesCache = new DataTypeCache();
    private SchemaCache schemaCache = new SchemaCache();
    private TriggerCache triggerCache = new TriggerCache();

    SQLServerDatabase(JDBCSession session, SQLServerDataSource dataSource, JDBCResultSet resultSet) {
        this.dataSource = dataSource;
        this.name = JDBCUtils.safeGetString(resultSet, "name");
        //this.description = JDBCUtils.safeGetString(resultSet, "description");

        this.persisted = true;

        if (CommonUtils.equalObjects(
            ((SQLServerExecutionContext) session.getExecutionContext()).getActiveDatabaseName(),
            this.name))
        {
            try {
                getSchemas(session.getProgressMonitor());
            } catch (DBException e) {
                log.debug("Error reading default database schemas", e);
            }
        }
    }

    @NotNull
    @Override
    public SQLServerDataSource getDataSource() {
        return dataSource;
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = true, order = 1)
    public String getName() {
        return name;
    }

    @Override
    @Property(viewable = true, editable = true, updatable = true, multiline = true, order = 100)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) {
        typesCache.clearCache();
        schemaCache.clearCache();
        triggerCache.clearCache();
        return this;
    }

    //////////////////////////////////////////////////
    // Data types

    @Association
    public Collection<SQLServerDataType> getDataTypes(DBRProgressMonitor monitor) throws DBException {
        return typesCache.getAllObjects(monitor, this);
    }

    public SQLServerDataType getDataType(DBRProgressMonitor monitor, String typeName) throws DBException {
        return typesCache.getObject(monitor, this, typeName);
    }

    SQLServerDataType getDataTypeByUserTypeId(DBRProgressMonitor monitor, int typeID) throws DBException {
        typesCache.getAllObjects(monitor, this);

        SQLServerDataType dataType = typesCache.getDataType(typeID);
        if (dataType != null) {
            return dataType;
        }
        dataType = dataSource.getSystemDataType(typeID);
        if (dataType != null) {
            return dataType;
        }
        log.debug("Data type '" + typeID + "' not found in database " + getName());
        return null;
    }

    @Override
    public String toString() {
        return getName();
    }

    ///////////////////////////////////////////////////////
    // Caches

    private class DataTypeCache extends JDBCObjectCache<SQLServerDatabase, SQLServerDataType> {

        private LongKeyMap<SQLServerDataType> dataTypeMap = new LongKeyMap<>();
        
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull SQLServerDatabase database) throws SQLException {
            return session.prepareStatement(
                "SELECT * FROM " + SQLServerUtils.getSystemTableName(database, "types") + " WHERE is_user_defined = 1 order by name");
        }

        @Override
        protected SQLServerDataType fetchObject(@NotNull JDBCSession session, @NotNull SQLServerDatabase database, @NotNull JDBCResultSet resultSet) {
            return new SQLServerDataType(database, resultSet);
        }

        SQLServerDataType getDataType(long typeID) {
            return dataTypeMap.get(typeID);
        }

        @Override
        public void clearCache() {
            super.clearCache();
            dataTypeMap.clear();
        }

        @Override
        public void removeObject(@NotNull SQLServerDataType object, boolean resetFullCache) {
            super.removeObject(object, resetFullCache);
            dataTypeMap.remove(object.getObjectId());
        }

        @Override
        public void cacheObject(@NotNull SQLServerDataType object) {
            super.cacheObject(object);
            dataTypeMap.put(object.getObjectId(), object);
        }

        @Override
        public void setCache(List<SQLServerDataType> cache) {
            super.setCache(cache);
            for (SQLServerDataType dt : cache) {
                dataTypeMap.put(dt.getObjectId(), dt);
            }
        }

    }

    //////////////////////////////////////////////////
    // Schemas

    @Association
    public Collection<SQLServerSchema> getSchemas(DBRProgressMonitor monitor) throws DBException {
        return getChildren(monitor);
    }

    public SQLServerSchema getSchema(DBRProgressMonitor monitor, long schemaId) throws DBException {
        for (SQLServerSchema schema : getSchemas(monitor)) {
            if (schema.getObjectId() == schemaId) {
                return schema;
            }
        }
        if (!monitor.isCanceled()) {
            log.debug("Schema '" + schemaId + "' not found");
        }
        return null;
    }

    public SQLServerSchema getSchema(DBRProgressMonitor monitor, String name) throws DBException {
        return schemaCache.getObject(monitor, this, name);
    }

    public SQLServerSchema getSchema(String name) {
        return schemaCache.getCachedObject(name);
    }

    @Override
    public Collection<SQLServerSchema> getChildren(@NotNull DBRProgressMonitor monitor) throws DBException {
        return schemaCache.getAllObjects(monitor, this);
    }

    @Override
    public DBSObject getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) throws DBException {
        return schemaCache.getObject(monitor, this, childName);
    }

    @NotNull
    @Override
    public Class<? extends DBSObject> getPrimaryChildType(@NotNull DBRProgressMonitor monitor) {
        return SQLServerSchema.class;
    }

    @Override
    public void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope) throws DBException {
        schemaCache.getAllObjects(monitor, this);
    }

    static class SchemaCache extends JDBCObjectCache<SQLServerDatabase, SQLServerSchema> {
        SchemaCache() {
            setListOrderComparator(DBUtils.nameComparatorIgnoreCase());
        }

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull SQLServerDatabase owner) throws SQLException {
            SQLServerDataSource dataSource = owner.getDataSource();
            boolean showAllSchemas = SQLServerUtils.isShowAllSchemas(dataSource);

            String sysSchema = SQLServerUtils.getSystemSchemaFQN(dataSource, owner.getName(), SQLServerConstants.SQL_SERVER_SYSTEM_SCHEMA);
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT ");
            if (!showAllSchemas) {
                sql.append("DISTINCT ");
            }
            sql.append("s.*,ep.value as description FROM ").append(sysSchema).append(".schemas s");
            sql.append("\nLEFT OUTER JOIN ").append(SQLServerUtils.getExtendedPropsTableName(owner)).append(" ep ON ep.class=").append(SQLServerObjectClass.SCHEMA.getClassId())
                .append(" AND ep.major_id=s.schema_id AND ep.minor_id=0 AND ep.name='").append(SQLServerConstants.PROP_MS_DESCRIPTION).append("'");
            if (!showAllSchemas) {
                sql.append("\nINNER JOIN ").append(sysSchema).append(".");
                if (dataSource.isServerVersionAtLeast(SQLServerConstants.SQL_SERVER_2008_VERSION_MAJOR, 0)) {
                    sql.append("all_objects o ").append("ON s.schema_id=o.schema_id");
                } else {
                    sql.append("sysobjects o ").append("ON s.schema_id=o.uid");
                }
            }
            final DBSObjectFilter schemaFilters = dataSource.getContainer().getObjectFilter(SQLServerSchema.class, owner, false);
            if (schemaFilters != null && schemaFilters.isEnabled()) {
                sql.append("\n");
                JDBCUtils.appendFilterClause(sql, schemaFilters, "s.name", true);
            }

            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            if (schemaFilters != null) {
                JDBCUtils.setFilterParameters(dbStat, 1, schemaFilters);
            }

            return dbStat;
        }

        @Override
        protected SQLServerSchema fetchObject(@NotNull JDBCSession session, @NotNull SQLServerDatabase owner, @NotNull JDBCResultSet resultSet) {
            return new SQLServerSchema(owner, resultSet);
        }

    }

    //////////////////////////////////////////////////
    // Triggers

    @Association
    public Collection<SQLServerDatabaseTrigger> getTriggers(DBRProgressMonitor monitor) throws DBException {
        return triggerCache.getAllObjects(monitor, this);
    }

    TriggerCache getTriggerCache() {
        return triggerCache;
    }

    class TriggerCache extends JDBCObjectLookupCache<SQLServerDatabase, SQLServerDatabaseTrigger> {

        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull SQLServerDatabase database, SQLServerDatabaseTrigger object, String objectName) throws SQLException {
            StringBuilder sql = new StringBuilder(500);
            sql.append(
                "SELECT t.* FROM \n")
                .append(SQLServerUtils.getSystemTableName(database, "triggers")).append(" t");
            sql.append("\nWHERE t.parent_id=0");
            if (object != null || objectName != null) {
                sql.append(" AND t.name=?");
            }
            sql.append("\nORDER BY t.name");

            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            if (object != null || objectName != null) {
                dbStat.setString(1, object != null ? object.getName() : objectName);
            }
            return dbStat;
        }

        @Override
        protected SQLServerDatabaseTrigger fetchObject(@NotNull JDBCSession session, @NotNull SQLServerDatabase database, @NotNull JDBCResultSet resultSet) {
            return new SQLServerDatabaseTrigger(database, resultSet);
        }

    }

}
