package org.jkiss.dbeaver.ext.yashandb.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.AbstractExecutionSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.meta.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableConstraint;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @Author: donghy
 * @Date: 2022/08
 * @Description:
 */
public abstract class YashanDBTablePhysical extends YashanDBTableBase implements DBSObjectLazy<YashanDBDataSource> {
    private static final Log log = Log.getLog(YashanDBTablePhysical.class);

    private long rowCount;
    private Long realRowCount;
    private Object tablespace;
    private boolean partitioned;
    private PartitionInfo partitionInfo;
    private PartitionCache partitionCache;

    protected YashanDBTablePhysical(YashanDBSchema schema, String name) {
        super(schema, name, false);
    }


    /**
     * the constructor function.
     */
    protected YashanDBTablePhysical(YashanDBSchema schema, ResultSet dbResult) {
        super(schema, dbResult);
        this.rowCount = JDBCUtils.safeGetLong(dbResult, "NUM_ROWS");
        this.tablespace = JDBCUtils.safeGetString(dbResult, "TABLESPACE_NAME");
        this.partitioned = JDBCUtils.safeGetBoolean(dbResult, "PARTITIONED", "Y");
        this.partitionCache = partitioned ? new PartitionCache() : null;
    }

    /**
     * rowCount will be showed in Statistics.
     */
    @Property(category = DBConstants.CAT_STATISTICS, viewable = true, order = 20)
    public long getRowCount() {
        return rowCount;
    }

    /**
     * getRealRowCount is the newest value by synchronized method.
     */
    @Property(category = DBConstants.CAT_STATISTICS, viewable = false, expensive = true, order = 21)
    public synchronized Long getRealRowCount(DBRProgressMonitor monitor) {
        if (realRowCount != null) {
            return realRowCount;
        }
        if (!isPersisted()) {
            // Do not count rows for views
            return null;
        }

        // Query row count
        try (DBCSession session = DBUtils.openMetaSession(monitor, this, "Read row count")) {
            realRowCount = countData(new AbstractExecutionSource(this, session.getExecutionContext(), this), session, null, DBSDataContainer.FLAG_NONE);
        } catch (DBException e) {
            log.debug("Can't fetch row count", e);
        }
        if (realRowCount == null) {
            realRowCount = -1L;
        }

        return realRowCount;
    }

    @Override
    public Object getLazyReference(Object propertyId) {
        return tablespace;
    }

    /**
     * get table space list which will be showed in right UI.
     * Setting updatable false, which tablespace cannot be updated and edited.
     */
    @Property(viewable = true, order = 22, editable = true, length = PropertyLength.MULTILINE, updatable = false, listProvider = TablespaceListProvider.class)
    @LazyProperty(cacheValidator = YashanDBTablespace.TablespaceReferenceValidator.class)
    public Object getTablespace(DBRProgressMonitor monitor) throws DBException {
        return YashanDBTablespace.resolveTablespaceReference(monitor, this, null);
    }

    public Object getTablespace() {
        return tablespace;
    }

    public void setTablespace(YashanDBTablespace tablespace) {
        this.tablespace = tablespace;
    }

    @Override
    @Association
    public Collection<YashanDBTableIndex> getIndexes(DBRProgressMonitor monitor)
            throws DBException {
        // Read indexes using cache.
        return this.getContainer().indexCache.getObjects(monitor, getContainer(), this);
    }

    public YashanDBTableIndex getIndex(DBRProgressMonitor monitor, String name)
            throws DBException {
        return this.getContainer().indexCache.getObject(monitor, getContainer(), this, name);
    }

    /**
     * the association of Partition.
     */
    @PropertyGroup
    @LazyProperty(cacheValidator = PartitionInfoValidator.class)
    public PartitionInfo getPartitionInfo(DBRProgressMonitor monitor) throws DBException {
        if (partitionInfo == null && partitioned) {
            try (final JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load partitioning info")) {
                try (JDBCPreparedStatement dbStat = session.prepareStatement("SELECT * FROM ALL_PART_TABLES WHERE OWNER=? AND TABLE_NAME=?")) {
                    dbStat.setString(1, getContainer().getName());
                    dbStat.setString(2, getName());
                    try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                        if (dbResult.next()) {
                            partitionInfo = new PartitionInfo(monitor, this.getDataSource(), dbResult);
                        }
                    }
                }
            } catch (SQLException e) {
                throw new DBException(e, getDataSource());
            }
        }
        return partitionInfo;
    }

    @Association
    public Collection<YashanDBTablePartition> getPartitions(DBRProgressMonitor monitor)
            throws DBException {
        if (partitionCache == null) {
            return null;
        } else {
            this.partitionCache.getAllObjects(monitor, this);
            this.partitionCache.loadChildren(monitor, this, null);
            return this.partitionCache.getAllObjects(monitor, this);
        }
    }

    @Association
    public Collection<YashanDBTablePartition> getSubPartitions(DBRProgressMonitor monitor, YashanDBTablePartition partition)
            throws DBException {
        if (partitionCache == null) {
            return null;
        } else {
            this.partitionCache.getAllObjects(monitor, this);
            return this.partitionCache.getChildren(monitor, this, partition);
        }
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        this.getContainer().indexCache.clearObjectCache(this);
        return super.refreshObject(monitor);
    }

//    @Override
//    public boolean isFeatureSupported(String feature) {
//        return false;
//    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public DBSObjectState getObjectState() {
        return null;
    }

    @Override
    public void refreshObjectState(@NotNull DBRProgressMonitor monitor) throws DBCException {
        this.valid = YashanDBUtils.getObjectStatus(monitor, this, YashanDBObjectType.TABLE);
    }


    @Override
    public String getDescription(DBRProgressMonitor monitor) {
        return null;
    }

    ////////////////////////////////

    /**
     * Partitioning showed in Statistics.
     */

    private static class PartitionCache extends JDBCStructCache<YashanDBTablePhysical, YashanDBTablePartition, YashanDBTablePartition> {

        protected PartitionCache() {
            super("PARTITION_NAME");
        }

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull YashanDBTablePhysical table) throws SQLException {
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT * FROM ALL_TAB_PARTITIONS " +
                            "WHERE TABLE_OWNER=? AND TABLE_NAME=? " +
                            "ORDER BY PARTITION_POSITION");
            dbStat.setString(1, table.getContainer().getName());
            dbStat.setString(2, table.getName());
            return dbStat;
        }

        @Override
        protected YashanDBTablePartition fetchObject(@NotNull JDBCSession session, @NotNull YashanDBTablePhysical table, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new YashanDBTablePartition(table, false, resultSet);
        }

        @Override
        protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session, @NotNull YashanDBTablePhysical table, @Nullable YashanDBTablePartition forObject) throws SQLException {
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT * FROM ALL_TAB_PARTITIONS " +
                            "WHERE TABLE_OWNER=? AND TABLE_NAME=? " +
                            (forObject == null ? "" : "AND PARTITION_NAME=? ") +
                            "ORDER BY PARTITION_POSITION");
            dbStat.setString(1, table.getContainer().getName());
            dbStat.setString(2, table.getName());
            if (forObject != null) {
                dbStat.setString(3, forObject.getName());
            }
            return dbStat;
        }

        @Override
        protected YashanDBTablePartition fetchChild(@NotNull JDBCSession session, @NotNull YashanDBTablePhysical table, @NotNull YashanDBTablePartition parent, @NotNull JDBCResultSet dbResult) throws SQLException, DBException {
            return new YashanDBTablePartition(table, true, dbResult);
        }

    }

    public static class PartitionInfo extends YashanDBPartitionBase.PartitionInfoBase {

        private String partitionNames;

        //TMP
        private String partitionType;

        private List<YashanDBTableColumn> columns;


        public String getPartitionNames() {
            return partitionNames;
        }

        public void setPartitionNames(String partitionNames) {
            this.partitionNames = partitionNames;
        }

        @Override
        public YashanDBPartitionBase.PartitionType getPartitionType() {
            return null;
        }

        public void setPartitionType(String partitionType) {
            this.partitionType = partitionType;
        }

        public List<YashanDBTableColumn> getColumns() {
            return columns;
        }

        public void setColumns(List<YashanDBTableColumn> columns) {
            this.columns = columns;
        }

        public PartitionInfo(DBRProgressMonitor monitor, YashanDBDataSource dataSource, ResultSet dbResult,
                             String partitionNames, String partitionType, List<YashanDBTableColumn> columns) throws DBException {
            super(monitor, dataSource, dbResult);
            this.partitionNames = partitionNames;
            this.partitionType = partitionType;
            this.columns = columns;
        }

        public PartitionInfo(DBRProgressMonitor monitor, YashanDBDataSource dataSource, ResultSet dbResult)
                throws DBException {
            super(monitor, dataSource, dbResult);
        }
    }

    public static class PartitionInfoValidator implements IPropertyCacheValidator<YashanDBTablePhysical> {
        @Override
        public boolean isPropertyCached(YashanDBTablePhysical object, Object propertyId) {
            return object.partitioned && object.partitionInfo != null;
        }
    }

    public static class TablespaceListProvider implements IPropertyValueListProvider<YashanDBTablePhysical> {
        @Override
        public boolean allowCustomValue() {
            return false;
        }

        @Override
        public Object[] getPossibleValues(YashanDBTablePhysical object) {
            final List<YashanDBTablespace> tablespaces = new ArrayList<>();
            try {
                tablespaces.addAll(object.getDataSource().getTablespaces(new VoidProgressMonitor()));
            } catch (DBException e) {
                log.error(e);
            }
            tablespaces.sort(DBUtils.<YashanDBTablespace>nameComparator());
            return tablespaces.toArray(new YashanDBTablespace[tablespaces.size()]);
        }
    }


}
