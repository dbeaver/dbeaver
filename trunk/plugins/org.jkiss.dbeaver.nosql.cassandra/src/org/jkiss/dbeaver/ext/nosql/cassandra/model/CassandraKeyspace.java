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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

/**
 * CassandraKeyspace
 */
public class CassandraKeyspace implements DBSSchema
{
    private CassandraDataSource dataSource;
    private String keyspaceName;
    private String strategyClass;
    private Object strategyOptions;
    private int replicationFactor;

    private TableCache tableCache = new TableCache();

    public CassandraKeyspace(CassandraDataSource dataSource, String keyspaceName, JDBCResultSet dbResult)
    {
        this.dataSource = dataSource;
        this.keyspaceName = keyspaceName;
        this.strategyClass = JDBCUtils.safeGetStringTrimmed(dbResult, "STRATEGY_CLASS");
        this.strategyOptions = JDBCUtils.safeGetObject(dbResult, "STRATEGY_OPTIONS");
        this.replicationFactor = JDBCUtils.safeGetInt(dbResult, "REPLICATION_FACTOR");
    }

    public TableCache getTableCache()
    {
        return tableCache;
    }

    @NotNull
    @Override
    public CassandraDataSource getDataSource()
    {
        return dataSource;
    }

    @Override
    @Property(viewable = true, order = 1)
    public String getName()
    {
        return keyspaceName;
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return null;
    }

    @Property(viewable = false, order = 10)
    public String getStrategyClass()
    {
        return strategyClass;
    }

    @Property(viewable = false, order = 11)
    public Object getStrategyOptions()
    {
        return strategyOptions;
    }

    @Property(viewable = false, order = 12)
    public int getReplicationFactor()
    {
        return replicationFactor;
    }

    @Override
    public DBSObject getParentObject()
    {
        return dataSource.getContainer();
    }

    @Association
    public Collection<CassandraColumnFamily> getColumnFamilies(DBRProgressMonitor monitor) throws DBException
    {
        return getChildren(monitor);
    }

    @Override
    public Collection<CassandraColumnFamily> getChildren(DBRProgressMonitor monitor) throws DBException
    {
        return tableCache.getObjects(monitor, this);
    }

    @Override
    public CassandraColumnFamily getChild(DBRProgressMonitor monitor, String childName) throws DBException
    {
        return tableCache.getObject(monitor, this, childName);
    }

    @Override
    public Class<? extends DBSEntity> getChildType(DBRProgressMonitor monitor)
        throws DBException
    {
        return CassandraColumnFamily.class;
    }

    @Override
    public void cacheStructure(DBRProgressMonitor monitor, int scope) throws DBException
    {
        getChildren(monitor);
    }

    @Override
    public boolean isPersisted()
    {
        return true;
    }

    public class TableCache extends JDBCStructCache<CassandraKeyspace, CassandraColumnFamily, CassandraColumn> {

        TableCache()
        {
            super(JDBCConstants.TABLE_NAME);
            setListOrderComparator(DBUtils.<CassandraColumnFamily>nameComparator());
        }

        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCSession session, CassandraKeyspace owner)
            throws SQLException
        {
            return session.getMetaData().getTables(
                null,
                owner.getName(),
                null,
                null).getSourceStatement();
        }

        @Override
        protected CassandraColumnFamily fetchObject(JDBCSession session, CassandraKeyspace owner, ResultSet dbResult)
            throws SQLException, DBException
        {
            boolean isSystemTable = owner.getName().equals("system");
            if (isSystemTable && !owner.getDataSource().getContainer().isShowSystemObjects()) {
                return null;
            }
            return new CassandraColumnFamily(
                owner,
                dbResult);
        }

        @Override
        protected JDBCStatement prepareChildrenStatement(JDBCSession session, CassandraKeyspace owner, CassandraColumnFamily forTable)
            throws SQLException
        {
            return session.getMetaData().getColumns(
                null,
                owner.getName(),
                forTable == null ? null : forTable.getName(),
                null).getSourceStatement();
        }

        @Override
        protected CassandraColumn fetchChild(JDBCSession session, CassandraKeyspace owner, CassandraColumnFamily columnFamily, ResultSet dbResult)
            throws SQLException, DBException
        {
            return new CassandraColumn(
                columnFamily,
                dbResult);
        }
    }

}
