/*
 * Copyright (C) 2010-2012 Serge Rieder
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
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
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
            DBCExecutionContext context = getDataSource().openContext(monitor, DBCExecutionPurpose.META, "Read row count");
            try {
                realRowCount = countData(context, null);
            }
            catch (DBException e) {
                log.debug("Can't fetch row count", e);
            }
            finally {
                context.close();
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
            final JDBCExecutionContext context = getDataSource().openContext(monitor, DBCExecutionPurpose.META, "Load partitioning info");
            try {
                final JDBCPreparedStatement dbStat = context.prepareStatement("SELECT * FROM ALL_PART_TABLES WHERE OWNER=? AND TABLE_NAME=?");
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
                throw new DBException(e);
            } finally {
                context.close();
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
            this.partitionCache.loadObjects(monitor, this);
            this.partitionCache.loadChildren(monitor, this, null);
            return this.partitionCache.getObjects(monitor, this);
        }
    }

    @Override
    public boolean refreshObject(DBRProgressMonitor monitor) throws DBException
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
        protected JDBCStatement prepareObjectsStatement(JDBCExecutionContext context, OracleTablePhysical table) throws SQLException
        {
            final JDBCPreparedStatement dbStat = context.prepareStatement(
                "SELECT * FROM SYS.ALL_TAB_PARTITIONS " +
                "WHERE TABLE_OWNER=? AND TABLE_NAME=? " +
                "ORDER BY PARTITION_POSITION");
            dbStat.setString(1, table.getContainer().getName());
            dbStat.setString(2, table.getName());
            return dbStat;
        }

        @Override
        protected OracleTablePartition fetchObject(JDBCExecutionContext context, OracleTablePhysical table, ResultSet resultSet) throws SQLException, DBException
        {
            return new OracleTablePartition(table, false, resultSet);
        }

        @Override
        protected JDBCStatement prepareChildrenStatement(JDBCExecutionContext context, OracleTablePhysical table, OracleTablePartition forObject) throws SQLException
        {
            final JDBCPreparedStatement dbStat = context.prepareStatement(
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
        protected OracleTablePartition fetchChild(JDBCExecutionContext context, OracleTablePhysical table, OracleTablePartition parent, ResultSet dbResult) throws SQLException, DBException
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
