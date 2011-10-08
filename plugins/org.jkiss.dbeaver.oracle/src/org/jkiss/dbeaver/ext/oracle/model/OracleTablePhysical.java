/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.meta.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectLazy;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

/**
 * Oracle physical table
 */
public abstract class OracleTablePhysical extends OracleTableBase implements DBSObjectLazy<OracleDataSource>
{

    //private boolean valid;
    private long rowCount;
    private Object tablespace;
    private List<OracleIndex> indexes;
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

    @Property(name = "Row Count", viewable = true, order = 20, description = "Number of rows in the table (populated only if you collect statistics)")
    public long getRowCount()
    {
        return rowCount;
    }

//    @Property(name = "Valid", viewable = true, order = 21, description = "If a previous DROP TABLE operation failed, indicates whether the table is unusable")
//    public boolean isValid()
//    {
//        return valid;
//    }

    public Object getLazyReference(Object propertyId)
    {
        return tablespace;
    }

    @Property(name = "Tablespace", viewable = true, order = 22)
    @LazyProperty(cacheValidator = OracleTablespace.TablespaceReferenceValidator.class)
    public Object getTablespace(DBRProgressMonitor monitor) throws DBException
    {
        return OracleTablespace.resolveTablespaceReference(monitor, this, null);
    }

    @Association
    public List<OracleIndex> getIndexes(DBRProgressMonitor monitor)
        throws DBException
    {
        if (indexes == null) {
            // Read indexes using cache
            this.getContainer().indexCache.getObjects(monitor, getContainer(), this);
        }
        return indexes;
    }

    public OracleIndex getIndexe(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return DBUtils.findObject(getIndexes(monitor), name);
    }

    Collection<OracleIndex> getIndexesCache()
    {
        return indexes;
    }

    void setIndexes(List<OracleIndex> indexes)
    {
        this.indexes = indexes;
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
    public boolean refreshEntity(DBRProgressMonitor monitor) throws DBException
    {
        super.refreshEntity(monitor);

        indexes = null;
        partitionInfo = null;
        if (partitionCache != null) {
            partitionCache.clearCache();
        }
        return true;
    }

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
        protected JDBCPreparedStatement prepareObjectsStatement(JDBCExecutionContext context, OracleTablePhysical table) throws SQLException
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
        protected JDBCPreparedStatement prepareChildrenStatement(JDBCExecutionContext context, OracleTablePhysical table, OracleTablePartition forObject) throws SQLException
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

        @Override
        protected boolean isChildrenCached(OracleTablePartition parent)
        {
            return parent.getSubPartitions() != null;
        }

        @Override
        protected void cacheChildren(OracleTablePartition parent, List<OracleTablePartition> subpartitions)
        {
            parent.setSubPartitions(subpartitions);
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
        public boolean isPropertyCached(OracleTablePhysical object, Object propertyId)
        {
            return object.partitioned && object.partitionInfo != null;
        }
    }

}
