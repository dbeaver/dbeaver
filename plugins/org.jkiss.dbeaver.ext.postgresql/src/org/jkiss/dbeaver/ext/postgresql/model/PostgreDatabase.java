/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.model.DBPStatefulObject;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSInstance;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

/**
 * PostgreDatabase
 */
public class PostgreDatabase implements DBSInstance, DBSCatalog, DBPStatefulObject, PostgreObject {

    static final Log log = Log.getLog(PostgreDatabase.class);

    private PostgreDataSource dataSource;
    private int oid;
    private String name;
    private int ownerId;
    private int encodingId;
    private String collate;
    private String ctype;
    private boolean isTemplate;
    private boolean allowConnect;
    private int connectionLimit;
    private int tablespaceId;

    final SchemaCache schemaCache = new SchemaCache();
    final PostgreDataTypeCache datatypeCache = new PostgreDataTypeCache();

    public PostgreDatabase(PostgreDataSource dataSource, ResultSet dbResult)
        throws SQLException
    {
        this.dataSource = dataSource;
        this.loadInfo(dbResult);
    }

    private void loadInfo(ResultSet dbResult)
        throws SQLException
    {
        this.oid = JDBCUtils.safeGetInt(dbResult, "oid");
        this.name = JDBCUtils.safeGetString(dbResult, "datname");
        this.ownerId = JDBCUtils.safeGetInt(dbResult, "datdba");
        this.encodingId = JDBCUtils.safeGetInt(dbResult, "encoding");
        this.collate = JDBCUtils.safeGetString(dbResult, "datcollate");
        this.ctype = JDBCUtils.safeGetString(dbResult, "datctype");
        this.isTemplate = JDBCUtils.safeGetBoolean(dbResult, "datistemplate");
        this.allowConnect = JDBCUtils.safeGetBoolean(dbResult, "datallowconn");
        this.connectionLimit = JDBCUtils.safeGetInt(dbResult, "datconnlimit");
        this.tablespaceId = JDBCUtils.safeGetInt(dbResult, "dattablespace");
    }

    @NotNull
    @Override
    public PostgreDatabase getDatabase() {
        return this;
    }

    @Override
    public int getObjectId() {
        return this.oid;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 2)
    public String getName()
    {
        return name;
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return null;
    }

    @Override
    public DBSObject getParentObject()
    {
        return dataSource.getContainer();
    }

    @NotNull
    @Override
    public PostgreDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    ///////////////////////////////////////////////////
    // Instance methods


    @NotNull
    @Override
    public DBCExecutionContext getDefaultContext(boolean meta) {
        return dataSource.getDefaultContext(meta);
    }

    @NotNull
    @Override
    public Collection<? extends DBCExecutionContext> getAllContexts() {
        return dataSource.getAllContexts();
    }

    @NotNull
    @Override
    public DBCExecutionContext openIsolatedContext(@NotNull DBRProgressMonitor monitor, @NotNull String purpose) throws DBException {
        return dataSource.openIsolatedContext(monitor, purpose);
    }

    @Override
    public void close() {

    }

    ///////////////////////////////////////////////
    // Object container

    @Association
    public Collection<PostgreSchema> getSchemas(DBRProgressMonitor monitor) throws DBException {
        if (this != dataSource.getDefaultInstance()) {
            throw new DBException("Can't access non-default database");
        }
        cacheDataTypes(monitor);
        // Get all schemas
        return schemaCache.getAllObjects(monitor, this);
    }

    private void cacheDataTypes(DBRProgressMonitor monitor) throws DBException {
        // Cache data types
        datatypeCache.getAllObjects(monitor, this);
    }

    public PostgreSchema getSchema(DBRProgressMonitor monitor, String name) throws DBException {
        if (this != dataSource.getDefaultInstance()) {
            throw new DBException("Can't access non-default database");
        }
        cacheDataTypes(monitor);
        return schemaCache.getObject(monitor, this, name);
    }

    public PostgreSchema getSchema(DBRProgressMonitor monitor, int oid) throws DBException {
        if (this != dataSource.getDefaultInstance()) {
            throw new DBException("Can't access non-default database");
        }
        for (PostgreSchema schema : schemaCache.getAllObjects(monitor, this)) {
            if (schema.getObjectId() == oid) {
                return schema;
            }
        }
        return null;
    }

    PostgreTableBase findTable(DBRProgressMonitor monitor, String catalogName, String tableName)
        throws DBException
    {
        if (CommonUtils.isEmpty(catalogName)) {
            return null;
        }
        PostgreSchema schema = getSchema(monitor, catalogName);
        if (schema == null) {
            log.error("Catalog " + catalogName + " not found");
            return null;
        }
        return schema.getTable(monitor, tableName);
    }

    PostgreTableBase findTable(DBRProgressMonitor monitor, int schemaId, int tableId)
        throws DBException
    {
        PostgreSchema schema = getSchema(monitor, schemaId);
        if (schema == null) {
            log.error("Catalog " + schemaId + " not found");
            return null;
        }
        return schema.getTable(monitor, tableId);
    }

    @Override
    public Collection<? extends DBSObject> getChildren(@NotNull DBRProgressMonitor monitor) throws DBException {
        return getSchemas(monitor);
    }

    @Override
    public DBSObject getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) throws DBException {
        return getSchema(monitor, childName);
    }

    @Override
    public Class<? extends DBSObject> getChildType(@NotNull DBRProgressMonitor monitor) throws DBException {
        return PostgreSchema.class;
    }

    @Override
    public void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope) throws DBException {

    }

    @Override
    public DBSObjectState getObjectState() {
        if (this == dataSource.getDefaultInstance()) {
            return DBSObjectState.NORMAL;
        } else {
            return PostgreConstants.STATE_UNAVAILABLE;
        }
    }

    @Override
    public void refreshObjectState(@NotNull DBRProgressMonitor monitor) throws DBCException {

    }

    static class SchemaCache extends JDBCObjectCache<PostgreDatabase, PostgreSchema>
    {
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull PostgreDatabase owner) throws SQLException
        {
/*
            // Do not apply filters
            // We need all schemas to have access to types
            return session.prepareStatement(
                "SELECT n.oid,n.* FROM pg_catalog.pg_namespace n ORDER BY nspname");
*/
            StringBuilder catalogQuery = new StringBuilder("SELECT n.oid,n.* FROM pg_catalog.pg_namespace n ORDER BY nspname");
            DBSObjectFilter catalogFilters = owner.getDataSource().getContainer().getObjectFilter(PostgreSchema.class, null, false);
            if (catalogFilters != null) {
                JDBCUtils.appendFilterClause(catalogQuery, catalogFilters, PostgreConstants.COL_SCHEMA_NAME, true);
            }
            JDBCPreparedStatement dbStat = session.prepareStatement(catalogQuery.toString());
            if (catalogFilters != null) {
                JDBCUtils.setFilterParameters(dbStat, 1, catalogFilters);
            }
            return dbStat;
        }

        @Override
        protected PostgreSchema fetchObject(@NotNull JDBCSession session, @NotNull PostgreDatabase owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException
        {
            String name = JDBCUtils.safeGetString(resultSet, "nspname");
            if (name == null || name.startsWith("pg_toast") || name.startsWith("pg_temp")) {
                return null;
            }
            return new PostgreSchema(owner, name, resultSet);
        }

    }
}
