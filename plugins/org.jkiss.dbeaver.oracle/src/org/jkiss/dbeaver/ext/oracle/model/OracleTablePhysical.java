/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

/**
 * Oracle physical table
 */
public abstract class OracleTablePhysical extends OracleTableBase
{

    private boolean valid;
    private long rowCount;
    private volatile String tablespaceName;
    private volatile OracleTablespace tablespace;
    private List<OracleIndex> indexes;
    private boolean partitioned;
    private PartitionInfo partitionInfo;
    private PartitionCache partitionCache;

    public OracleTablePhysical(OracleSchema schema)
    {
        super(schema, false);
    }

    public OracleTablePhysical(
        OracleSchema schema,
        ResultSet dbResult)
    {
        super(schema, dbResult);
        this.rowCount = JDBCUtils.safeGetLong(dbResult, "NUM_ROWS");
        this.valid = "VALID".equals(JDBCUtils.safeGetString(dbResult, "STATUS"));
        this.tablespaceName = JDBCUtils.safeGetString(dbResult, "TABLESPACE_NAME");

        this.partitioned = JDBCUtils.safeGetBoolean(dbResult, "PARTITIONED", "Y");
        this.partitionCache = partitioned ? new PartitionCache() : null;
    }

    @Property(name = "Row Count", viewable = true, order = 20)
    public long getRowCount()
    {
        return rowCount;
    }

    @Property(name = "Valid", viewable = true, order = 21)
    public boolean isValid()
    {
        return valid;
    }

    @Property(name = "Tablespace", viewable = true, order = 22)
    @LazyProperty(cacheValidator = TablespaceRetrieveValidator.class)
    public Object getTablespace(DBRProgressMonitor monitor) throws DBException
    {
        final OracleDataSource dataSource = getDataSource();
        if (!dataSource.isAdmin()) {
            return tablespaceName;
        } else if (tablespace == null && !CommonUtils.isEmpty(tablespaceName)) {
            tablespace = dataSource.tablespaceCache.getObject(monitor, dataSource, tablespaceName);
            if (tablespace != null) {
                tablespaceName = null;
            } else {
                log.warn("Tablespace '" + tablespaceName + "' not found");
            }
        }
        return tablespace;
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

    boolean isIndexesCached()
    {
        return indexes != null;
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
                            partitionInfo = new PartitionInfo(dbResult);
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
        return partitionCache == null ? null : this.partitionCache.getObjects(monitor, this);
    }

    @Override
    public boolean refreshEntity(DBRProgressMonitor monitor) throws DBException
    {
        super.refreshEntity(monitor);

        indexes = null;
        return true;
    }

    private static class PartitionCache extends JDBCObjectCache<OracleTablePhysical, OracleTablePartition> {

        @Override
        protected JDBCPreparedStatement prepareObjectsStatement(JDBCExecutionContext context, OracleTablePhysical oracleTablePhysical) throws SQLException, DBException
        {
            final JDBCPreparedStatement dbStat = context.prepareStatement(
                "");
            return dbStat;
        }

        @Override
        protected OracleTablePartition fetchObject(JDBCExecutionContext context, OracleTablePhysical oracleTablePhysical, ResultSet resultSet) throws SQLException, DBException
        {
            return null;
        }
    }

    public static class PartitionInfo extends OraclePartitionBase.PartitionInfoBase {
        public PartitionInfo(ResultSet dbResult)
        {
            super(dbResult);
        }
    }

    public static class PartitionInfoValidator implements IPropertyCacheValidator<OracleTablePhysical> {
        public boolean isPropertyCached(OracleTablePhysical object)
        {
            return object.partitioned && object.partitionInfo != null;
        }
    }

    public static class TablespaceRetrieveValidator implements IPropertyCacheValidator<OracleTablePhysical> {
        public boolean isPropertyCached(OracleTablePhysical object)
        {
            return
                object.tablespace instanceof OracleTablespace ||
                CommonUtils.isEmpty(object.tablespaceName) ||
                object.getDataSource().tablespaceCache.isCached() ||
                !object.getDataSource().isAdmin();
        }
    }

}
