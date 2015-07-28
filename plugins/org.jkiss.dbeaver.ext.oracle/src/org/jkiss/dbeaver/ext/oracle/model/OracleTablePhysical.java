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
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.*;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.meta.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectLazy;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

/**
 * Oracle physical table
 */
public abstract class OracleTablePhysical extends OracleTableBase implements DBSObjectLazy<OracleDataSource>
{

    //private boolean valid;
    private long rowCount;
    private Long realRowCount;
    private Object tablespace;
    private boolean partitioned;
    private PartitionInfo partitionInfo;
    private PartitionCache partitionCache;

    public OracleTablePhysical(OracleSchema schema, String name)
    {
        super(schema, name, false);
    }

    public OracleTablePhysical(
        OracleSchema schema,
        ResultSet dbResult)
    {
        super(schema, dbResult);
        this.rowCount = JDBCUtils.safeGetLong(dbResult, "NUM_ROWS");
        //this.valid = "VALID".equals(JDBCUtils.safeGetString(dbResult, "STATUS"));
        this.tablespace = JDBCUtils.safeGetString(dbResult, "TABLESPACE_NAME");

        this.partitioned = JDBCUtils.safeGetBoolean(dbResult, "PARTITIONED", "Y");
        this.partitionCache = partitioned ? new PartitionCache() : null;
    }

    @Property(viewable = true, order = 20)
    public long getRowCount()
    {
        return rowCount;
    }

    @Property(viewable = false, expensive = true, order = 21)
    public synchronized Long getRealRowCount(DBRProgressMonitor monitor)
    {
        if (realRowCount != null) {
            return realRowCount;
        }
        if (!isPersisted()) {
            // Do not count rows for views
            return null;
        }

        if (realRowCount == null) {
            // Query row count
            DBCSession session = getDataSource().getDefaultContext(false).openSession(monitor, DBCExecutionPurpose.META, "Read row count");
            try {
                realRowCount = countData(session, null);
            }
            catch (DBException e) {
                log.debug("Can't fetch row count", e);
            }
            finally {
                session.close();
            }
        }
        if (realRowCount == null) {
            realRowCount = -1L;
        }

        return realRowCount;
    }

    @Override
    public Object getLazyReference(Object propertyId)
    {
        return tablespace;
    }

    @Property(viewable = true, order = 22)
    @LazyProperty(cacheValidator = OracleTablespace.TablespaceReferenceValidator.class)
    public Object getTablespace(DBRProgressMonitor monitor) throws DBException
    {
        return OracleTablespace.resolveTablespaceReference(monitor, this, null);
    }

    @Override
    @Association
    public Collection<OracleTableIndex> getIndexes(DBRProgressMonitor monitor)
        throws DBException
    {
        // Read indexes using cache
        return this.getContainer().indexCache.getObjects(monitor, getContainer(), this);
    }

    public OracleTableIndex getIndex(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return this.getContainer().indexCache.getObject(monitor, getContainer(), this, name);
    }

    @PropertyGroup
    @LazyProperty(cacheValidator = PartitionInfoValidator.class)
    public PartitionInfo getPartitionInfo(DBRProgressMonitor monitor) throws DBException
    {
        if (partitionInfo == null && partitioned) {
            final JDBCSession session = getDataSource().getDefaultContext(true).openSession(monitor, DBCExecutionPurpose.META, "Load partitioning info");
            try {
                final JDBCPreparedStatement dbStat = session.prepareStatement("SELECT * FROM ALL_PART_TABLES WHERE OWNER=? AND TABLE_NAME=?");
                try {
                    dbStat.setString(1, getContainer().getName());
                    dbStat.setString(2, getName());
                    final JDBCResultSet dbResult = dbStat.executeQuery();
                    try {
                        if (dbResult.next()) {
                            partitionInfo = new PartitionInfo(monitor, this.getDataSource(), dbResult);
                        }
                    } finally {
                        dbResult.close();
                    }
                } finally {
                    dbStat.close();
                }
            } catch (SQLException e) {
                throw new DBException(e, session.getDataSource());
            } finally {
                session.close();
            }
        }
        return partitionInfo;
    }

    @Association
    public Collection<OracleTablePartition> getPartitions(DBRProgressMonitor monitor)
        throws DBException
    {
        if (partitionCache == null) {
            return null;
        } else {
            this.partitionCache.getAllObjects(monitor, this);
            this.partitionCache.loadChildren(monitor, this, null);
            return this.partitionCache.getAllObjects(monitor, this);
        }
    }

    @Override
    public boolean refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        super.refreshObject(monitor);
        this.getContainer().indexCache.clearObjectCache(this);

        partitionInfo = null;
        if (partitionCache != null) {
            partitionCache.clearCache();
        }
        realRowCount = null;
        return true;
    }

    @Override
    public void refreshObjectState(DBRProgressMonitor monitor) throws DBCException
    {
        this.valid = OracleUtils.getObjectStatus(monitor, this, OracleObjectType.TABLE);
    }

    private static class PartitionCache extends JDBCStructCache<OracleTablePhysical, OracleTablePartition, OracleTablePartition> {

        protected PartitionCache()
        {
            super("PARTITION_NAME");
        }

        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull OracleTablePhysical table) throws SQLException
        {
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT * FROM SYS.ALL_TAB_PARTITIONS " +
                "WHERE TABLE_OWNER=? AND TABLE_NAME=? " +
                "ORDER BY PARTITION_POSITION");
            dbStat.setString(1, table.getContainer().getName());
            dbStat.setString(2, table.getName());
            return dbStat;
        }

        @Override
        protected OracleTablePartition fetchObject(@NotNull JDBCSession session, @NotNull OracleTablePhysical table, @NotNull ResultSet resultSet) throws SQLException, DBException
        {
            return new OracleTablePartition(table, false, resultSet);
        }

        @Override
        protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session, @NotNull OracleTablePhysical table, @Nullable OracleTablePartition forObject) throws SQLException
        {
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT * FROM SYS.ALL_TAB_SUBPARTITIONS " +
                "WHERE TABLE_OWNER=? AND TABLE_NAME=? " +
                (forObject == null ? "" : "AND PARTITION_NAME=?") +
                "ORDER BY SUBPARTITION_POSITION");
            dbStat.setString(1, table.getContainer().getName());
            dbStat.setString(2, table.getName());
            if (forObject != null) {
                dbStat.setString(2, forObject.getName());
            }
            return dbStat;
        }

        @Override
        protected OracleTablePartition fetchChild(@NotNull JDBCSession session, @NotNull OracleTablePhysical table, @NotNull OracleTablePartition parent, @NotNull ResultSet dbResult) throws SQLException, DBException
        {
            return new OracleTablePartition(table, true, dbResult);
        }

    }

    public static class PartitionInfo extends OraclePartitionBase.PartitionInfoBase {

        public PartitionInfo(DBRProgressMonitor monitor, OracleDataSource dataSource, ResultSet dbResult)
            throws DBException
        {
            super(monitor, dataSource, dbResult);
        }
    }

    public static class PartitionInfoValidator implements IPropertyCacheValidator<OracleTablePhysical> {
        @Override
        public boolean isPropertyCached(OracleTablePhysical object, Object propertyId)
        {
            return object.partitioned && object.partitionInfo != null;
        }
    }

}
