/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.nosql.cassandra.model;

import org.jkiss.dbeaver.core.Log;
import org.eclipse.core.runtime.IAdaptable;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabaseTermProvider;
import org.jkiss.dbeaver.ext.nosql.cassandra.model.jdbc.CasConnection;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCBasicDataTypeCache;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCConnectionImpl;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCSQLDialect;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * CassandraDataSource
 */
public class CassandraDataSource extends JDBCDataSource
    implements DBSObjectSelector, IDatabaseTermProvider, IAdaptable
{
    static final Log log = Log.getLog(CassandraDataSource.class);

    private final JDBCBasicDataTypeCache dataTypeCache;
    private List<CassandraKeyspace> keyspaces;
    private String selectedKeyspace;

    public CassandraDataSource(DBRProgressMonitor monitor, DBSDataSourceContainer container)
        throws DBException
    {
        super(monitor, container);
        this.dataTypeCache = new JDBCBasicDataTypeCache(container);
    }

    @Override
    protected JDBCConnectionImpl createConnection(DBRProgressMonitor monitor, JDBCExecutionContext context, DBCExecutionPurpose purpose, String taskTitle)
    {
        return new CasConnection(context, monitor, purpose, taskTitle);
    }

    @Override
    protected SQLDialect createSQLDialect(JDBCDatabaseMetaData metaData) {
        return new JDBCSQLDialect(this, "Cassandra", metaData) {
            @Override
            public boolean supportsAliasInSelect() {
                return false;
            }
        };
    }

    public Collection<CassandraKeyspace> getKeyspaces()
    {
        return keyspaces;
    }

    public CassandraKeyspace getSchema(String name)
    {
        return DBUtils.findObject(getKeyspaces(), name);
    }

    @NotNull
    @Override
    public CassandraDataSource getDataSource() {
        return this;
    }

    @Override
    public void initialize(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        super.initialize(monitor);
        try {
            dataTypeCache.getObjects(monitor, this);
        } catch (DBException e) {
            log.warn("Can't fetch data types", e);
        }
        JDBCSession session = openSession(monitor, DBCExecutionPurpose.META, "Read cassandra metadata");
        try {
            // Read metadata
            JDBCDatabaseMetaData metaData = session.getMetaData();
            // Catalogs not supported - try to read root keyspaces
            monitor.subTask("Extract keyspaces");
            monitor.worked(1);
            List<CassandraKeyspace> tmpSchemas = loadKeyspaces(session);
            if (tmpSchemas != null) {
                this.keyspaces = tmpSchemas;
            }
            // Get selected entity (catalog or schema)
            selectedKeyspace = session.getSchema();

        } catch (SQLException ex) {
            throw new DBException("Error reading metadata", ex, this);
        }
        finally {
            session.close();
        }
    }

    List<CassandraKeyspace> loadKeyspaces(JDBCSession session)
        throws DBException
    {
        try {
            DBSObjectFilter ksFilters = getContainer().getObjectFilter(CassandraKeyspace.class, null);

            List<CassandraKeyspace> tmpKeyspaces = new ArrayList<CassandraKeyspace>();
            JDBCResultSet dbResult;
            try {
                dbResult = session.getMetaData().getSchemas(
                    null,
                    ksFilters != null && ksFilters.hasSingleMask() ? ksFilters.getSingleMask() : null);
            } catch (Throwable e) {
                // This method not supported (may be old driver version)
                // Use general schema reading method
                dbResult = session.getMetaData().getSchemas();
            }

            try {
                while (dbResult.next()) {
                    if (session.getProgressMonitor().isCanceled()) {
                        break;
                    }
                    String ksName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TABLE_SCHEM);
                    if (CommonUtils.isEmpty(ksName)) {
                        continue;
                    }
                    if (ksFilters != null && !ksFilters.matches(ksName)) {
                        // Doesn't match filter
                        continue;
                    }
                    session.getProgressMonitor().subTask("Keyspace " + ksName);

                    CassandraKeyspace keyspace = new CassandraKeyspace(this, ksName, dbResult);
                    tmpKeyspaces.add(keyspace);
                }
            } finally {
                dbResult.close();
            }
            return tmpKeyspaces;
        } catch (Exception ex) {
            log.error("Could not read keyspace list", ex);
            return null;
        }
    }

    @Override
    public boolean refreshObject(DBRProgressMonitor monitor)
        throws DBException
    {
        super.refreshObject(monitor);

        this.keyspaces = null;

        this.initialize(monitor);

        return true;
    }

    CassandraColumnFamily findTable(DBRProgressMonitor monitor, String schemaName, String tableName)
        throws DBException
    {
        if (!CommonUtils.isEmpty(schemaName)) {
            CassandraKeyspace container = this.getSchema(schemaName);
            if (container == null) {
                log.error("Schema '" + schemaName + "' not found");
            } else {
                return container.getChild(monitor, tableName);
            }
        }
        return null;
    }

    @Override
    public Collection<? extends DBSObject> getChildren(DBRProgressMonitor monitor)
        throws DBException
    {
        return getKeyspaces();
    }

    @Override
    public DBSObject getChild(DBRProgressMonitor monitor, String childName)
        throws DBException
    {
        return getSchema(childName);
    }

    @Override
    public Class<? extends DBSObject> getChildType(DBRProgressMonitor monitor)
        throws DBException
    {
        return CassandraKeyspace.class;
    }

    @Override
    public void cacheStructure(DBRProgressMonitor monitor, int scope) throws DBException {
        for (CassandraKeyspace schema : keyspaces) {
            schema.cacheStructure(monitor, scope);
        }
    }

    public boolean isChild(DBSObject object)
        throws DBException
    {
        return !CommonUtils.isEmpty(keyspaces) &&
            object instanceof CassandraKeyspace &&
            keyspaces.contains(CassandraKeyspace.class.cast(object));
    }

    @Override
    public boolean supportsObjectSelect()
    {
        return true;
    }

    @Override
    public DBSObject getSelectedObject()
    {
        return getSchema(selectedKeyspace);
    }

    @Override
    public void selectObject(DBRProgressMonitor monitor, DBSObject object)
        throws DBException
    {
        final DBSObject oldSelectedEntity = getSelectedObject();
        if (!isChild(object)) {
            throw new DBException("Bad child object specified as active: " + object);
        }

        setActiveEntityName(monitor, object);

        if (oldSelectedEntity != null) {
            DBUtils.fireObjectSelect(oldSelectedEntity, false);
        }
        DBUtils.fireObjectSelect(object, true);
    }

    void setActiveEntityName(DBRProgressMonitor monitor, DBSObject entity) throws DBException
    {
        if (entity instanceof CassandraKeyspace) {
            JDBCSession session = openSession(monitor, DBCExecutionPurpose.UTIL, "Set active catalog");
            try {
                session.setSchema(entity.getName());
                selectedKeyspace = entity.getName();
            } catch (SQLException e) {
                throw new DBException(e, session.getDataSource());
            }
            finally {
                session.close();
            }
        }
    }

    @Override
    public Object getAdapter(Class adapter)
    {
        if (adapter == DBSStructureAssistant.class) {
            return new CassandraStructureAssistant(this);
        } else {
            return null;
        }
    }

    @Override
    public String getObjectTypeTerm(String path, String objectType, boolean multiple)
    {
        String term = null;
        if ("cluster".equals(objectType)) {
            term = "Cluster";
        } else if ("keypace".equals(objectType)) {
            term = "Keyspace";
        }
        if (term != null && multiple) {
            term += "s";
        }
        return term;
    }

    @Override
    public Collection<? extends DBSDataType> getDataTypes()
    {
        return dataTypeCache.getCachedObjects();
    }

    @Override
    public DBSDataType getDataType(String typeName)
    {
        return dataTypeCache.getCachedObject(typeName);
    }

}
