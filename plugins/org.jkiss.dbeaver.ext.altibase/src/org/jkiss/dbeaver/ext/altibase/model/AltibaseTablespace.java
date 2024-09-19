/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.altibase.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.altibase.AltibaseUtils;
import org.jkiss.dbeaver.model.DBPObjectStatistics;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.IPropertyCacheValidator;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectLazy;
import org.jkiss.utils.ByteNumberFormat;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

public class AltibaseTablespace extends AltibaseGlobalObject implements DBPRefreshableObject, DBPObjectStatistics {

    public enum TbsType {
        MEMORY_SYSTEM_DICTIONARY(0),
        MEMORY_SYSTEM_DATA(1),
        MEMORY_USER_DATA(2),
        DISK_SYSTEM_DATA(3),
        DISK_USER_DATA(4),
        DISK_SYSTEM_TEMP(5),
        DISK_USER_TEMP(6),
        DISK_SYSTEM_UNDO(7),
        VOLATILE_USER_DATA(8),
        UNKNOWN(-1);
        
        private final int stateIdx;
        
        TbsType(int stateIdx) {
            this.stateIdx = stateIdx;
        }
        
        /**
         * Get TBS type get its index value. 
         */
        public static TbsType getTbsTypeByIdx(int stateIdx) {
            for (TbsType type : TbsType.values()) {
                if (stateIdx == type.stateIdx) {
                    return type;
                }
            }
            
            return UNKNOWN;
        }
    }
    
    public enum State {
        OFFLINE(1),
        ONLINE(2),
        BACKUP_OFFLINE(5),
        BACKUP_ONLINE(6),
        DROPPPED(128),
        DISCARDED(1024),
        BACKUP_DISCARDED(1028),
        UNKNOWN(-1);
        
        private final int stateIdx;
        
        State(int stateIdx) {
            this.stateIdx = stateIdx;
        }
        
        /**
         * Get TBS status by its state index value. 
         */
        public static State getStateByIdx(int stateIdx) {
            for (State status : State.values()) {
                if (stateIdx == status.stateIdx) {
                    return status;
                }
            }
            
            return UNKNOWN;
        }
    }
    
    private final int id;
    private final String name;
    private final String extentManagement;
    private final String segmentManagement;
    private final int dataFileCount;
    private final boolean isLogCompression;
    private final int pageSizeInBytes;
    
    private final TbsType tbsType;
    private final State state;
    
    private volatile Long availableSize;
    private volatile Long usedSize;
    private String qry4Size;
    
    final FileCache fileCache = new FileCache();
    final TablePartnCache tablePartnCache = new TablePartnCache();
    final IndexPartnCache indexPartnCache = new IndexPartnCache();
    
    protected AltibaseTablespace(AltibaseDataSource dataSource, ResultSet dbResult) {
        super(dataSource, true);
        
        this.id                 = JDBCUtils.safeGetInt(dbResult, "ID");
        this.name               = JDBCUtils.safeGetString(dbResult, "NAME");
        this.tbsType            = TbsType.getTbsTypeByIdx(JDBCUtils.safeGetInt(dbResult, "TYPE"));
        this.state              = State.getStateByIdx(JDBCUtils.safeGetInt(dbResult, "STATE"));
        this.extentManagement   = JDBCUtils.safeGetString(dbResult, "EXTENT_MANAGEMENT");
        this.segmentManagement  = JDBCUtils.safeGetString(dbResult, "SEGMENT_MANAGEMENT");    
        this.dataFileCount      = JDBCUtils.safeGetInt(dbResult, "DATAFILE_COUNT");
        this.pageSizeInBytes    = JDBCUtils.safeGetInt(dbResult, "PAGE_SIZE");
        this.isLogCompression   = (JDBCUtils.safeGetInt(dbResult, "ATTR_LOG_COMPRESS") == 1);
        
        setQry4Size();
    }

    private void setQry4Size() {
        switch (this.tbsType) {
            case MEMORY_SYSTEM_DICTIONARY:
            case MEMORY_SYSTEM_DATA:
            case MEMORY_USER_DATA:
                qry4Size = "SELECT "
                            + " d.mem_max_db_size TOTAL_SIZE "
                            + " , NVL(mt.used , 0) USED_SIZE"
                        + " FROM v$database d, v$tablespaces t left outer join"
                            + " (SELECT tablespace_id, round(sum(fixed_used_mem + var_used_mem),2) used"
                            + " FROM v$memtbl_info"
                            + " GROUP by tablespace_id) mt on t.id = mt.tablespace_id"
                        + " WHERE t.id = ?";
                break;
            case VOLATILE_USER_DATA:
                qry4Size = "SELECT"
                        + " m.max_size TOTAL_SIZE"
                        + " , NVL(mt.used, 0) used_size"
                    + " FROM"
                        + " (SELECT"
                            + " space_id"
                            + " , DECODE(max_sizE, 0, (SELECT VALUE1"
                                                  + " FROM V$PROPERTY"
                                                  + " WHERE NAME = 'VOLATILE_MAX_DB_SIZE'), MAX_SIZE) AS MAX_SIZE"
                      + " FROM V$VOL_TABLESPACES) M LEFT OUTER JOIN "
                            + " (SELECT tablespace_id, SUM((fixed_used_mem + var_used_mem)) USED"
                            + " FROM V$MEMTBL_INFO"
                            + " GROUP BY tablespace_id ) "
                            + " MT ON m.space_id = mt.tablespace_id"
                    + " WHERE m.space_id = ?";
                break;
            case DISK_SYSTEM_DATA:
            case DISK_USER_DATA:
                qry4Size = "SELECT "
                            + " (d.max * t.page_size) TOTAL_SIZE,"
                            + " nvl(ds.used, 0) 'USED_SIZE'"
                       + " FROM v$tablespaces t LEFT OUTER JOIN "
                           + " (SELECT space_id, sum(total_used_size) USED"
                           + " FROM x$segment"
                           + " GROUP by space_id) ds on ds.space_id = t.id"
                           + ", (SELECT SPACEID"
                           + " , SUM(DECODE(MAXSIZE, 0, CURRSIZE, MAXSIZE)) AS MAX"
                            + " FROM V$DATAFILES"
                           + " GROUP BY SPACEID) D"
                       + " WHERE t.id = D.spaceid AND t.id = ?";
                break;
            case DISK_SYSTEM_TEMP:
            case DISK_USER_TEMP:
                qry4Size = "SELECT "
                            + "(d.max * t.page_size) TOTAL_SIZE,"
                            + " nvl(xts.used_SIZE, 0) USED_SIZE"
                       + " FROM v$tablespaces t LEFT OUTER JOIN "
                               + " (SELECT tbs_id, sum(normal_area_size) used_SIZE "
                           + " FROM x$temptable_stats "
                           + " GROUP BY tbs_id) xts ON t.id = xts.tbs_id"
                           + ", (SELECT SPACEID"
                           + " , SUM(DECODE(MAXSIZE, 0, CURRSIZE, MAXSIZE)) AS MAX"
                            + " FROM V$DATAFILES"
                           + " GROUP BY SPACEID) D"
                       + " WHERE t.id = D.spaceid AND t.id = ?";
                break;
            case DISK_SYSTEM_UNDO:
                qry4Size = "SELECT "
                            + " (d.max * t.page_size) TOTAL_SIZE,"
                            + " ((u.tx_ext_cnt+u.used_ext_cnt+u.unstealable_ext_cnt) * prop.extent_size) USED_SIZE"
                       + " FROM v$tablespaces t, v$disk_undo_usage u,"
                           + " (select value1 extent_size from v$property where name = 'SYS_UNDO_TBS_EXTENT_SIZE') prop"
                           + ", (SELECT SPACEID"
                               + " , SUM(DECODE(MAXSIZE, 0, CURRSIZE, MAXSIZE)) AS MAX"
                            + " FROM V$DATAFILES"
                           + " GROUP BY SPACEID) D"
                       + " WHERE t.id = D.spaceid AND t.id = ?";
                break;
            default:
                qry4Size = "";
                    
        }
    }
    
    public String getQry4Size() {
        return qry4Size;
    }
    
    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }
    
    @Property(viewable = true, order = 2)
    public int getId() {
        return id;
    }
    
    @Property(viewable = true, order = 3)
    public String getTbsType() {
        return tbsType.name();
    }
    
    @Property(viewable = true, order = 4)
    public String getState() {
        return state.name();
    }
    
    @Property(viewable = true, order = 5)
    public String getExtentManagement() {
        return extentManagement;
    }
    
    @Property(viewable = true, order = 6)
    public String getSegmentManagement() {
        return segmentManagement;
    }
    
    @Property(viewable = true, order = 7)
    public int getDataFileCount() {
        return dataFileCount;
    }
    
    @Property(viewable = true, order = 8, formatter = ByteNumberFormat.class)
    public int getPageSizeInKBytes() {
        return pageSizeInBytes;
    }
    
    @Property(viewable = true, order = 9)
    public boolean getIsLogCompression() {
        return isLogCompression;
    }
    
    /**
     * Return avaiable size (byte) in tablespace
     */
    @Property(viewable = true, order = 10, formatter = ByteNumberFormat.class)
    public Long getAvailableSize(DBRProgressMonitor monitor) throws DBException {
        if (availableSize == null) {
            loadSizes(monitor);
        }
        return availableSize;
    }

    /**
     * Return used size (byte) in tablespace
     */
    @Property(viewable = true, order = 11, formatter = ByteNumberFormat.class)
    public Long getUsedSize(DBRProgressMonitor monitor) throws DBException {
        if (usedSize == null) {
            loadSizes(monitor);
        }
        return usedSize;
    }

    /**
     * Return Tablespace type as enumeration
     */
    public TbsType getTbsTypeEnum() {
        return tbsType;
    }
    
    /**
     * Return page size in bytes as int
     */
    public int getPageSizeInBytes() {
        return pageSizeInBytes;
    }
    
    /**
     * Return page size in bytes as String
     */
    public String getPageSizeInBytesStr() {
        return String.valueOf(pageSizeInBytes);
    }
    
    /**
     * Whether this is memory tablespace or not. 
     */
    public boolean isMemTbs() {
        switch (this.tbsType) {
            case MEMORY_SYSTEM_DICTIONARY:
            case MEMORY_SYSTEM_DATA:
            case MEMORY_USER_DATA:
                return true;
            default:
                return false;
        }
    }
    
    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        usedSize = null;
        availableSize = null;
        
        fileCache.clearCache();
        tablePartnCache.clearCache();
        indexPartnCache.clearCache();
        
        getDataSource().resetStatistics();
        return this;
    }

    ///////////////////////////////////////////////
    // Statistics

    @Override
    public boolean hasStatistics() {
        return usedSize != null;
    }

    @Override
    public long getStatObjectSize() {
        return usedSize == null ? 0 : usedSize;
    }

    @Nullable
    @Override
    public DBPPropertySource getStatProperties() {
        return null;
    }

    /**
     * Load tablespace size
     */
    public void loadSizes(DBRProgressMonitor monitor) throws DBException {
        String qry = getQry4Size();
        
        if (qry.length() < 1) {
            availableSize = 0L;
            usedSize = 0L;
            return;
        }
        
        try (final JDBCSession session = DBUtils.openMetaSession(
                monitor, this, "Load tablespace '" + getName() + "' statistics")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(qry)) {
                dbStat.setInt(1, getId());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        fetchSizes(dbResult);
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBDatabaseException("Can't read tablespace statistics", e, getDataSource());
        }
    }

    void fetchSizes(JDBCResultSet dbResult) throws SQLException {
        long totalSize = dbResult.getLong("TOTAL_SIZE");
        usedSize = dbResult.getLong("USED_SIZE");
        availableSize = totalSize - usedSize;
    }
    
    /**
     * Return file collection belongs to this tablespace.
     */
    @Association
    public Collection<AltibaseDataFile> getFiles(DBRProgressMonitor monitor) throws DBException {
        return fileCache.getAllObjects(monitor, this);
    }
    
    /**
     * Returns AltibaseDataFile matches to fileId
     */
    public AltibaseDataFile getFile(DBRProgressMonitor monitor, int fileId) throws DBException {
        for (AltibaseDataFile file : fileCache.getAllObjects(monitor, this)) {
            if (file.getId() == fileId) {
                return file;
            }
        }
        return null;
    }
    
    public FileCache getFileCache() {
        return fileCache;
    }
    
    /**
     *  Returns data files  
     */
    static class FileCache extends JDBCObjectCache<AltibaseTablespace, AltibaseDataFile> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(
                @NotNull JDBCSession session, 
                @NotNull AltibaseTablespace owner) throws SQLException {
            
            String qry = null;
            
            if (owner.isMemTbs()) {
                /*
                 * The memory data file consists of two sets, xxx-0-0 and xxx-1-0.
                 */
                qry =  " SELECT"
                            + " mt.ID ID"
                            + " , mt.space_id SPACEID"
                            + " , p.checkpoint_path || '/' || dbfile_name NAME"
                            + " , mt.current_size "
                            + " , mt.dbfile_size DBFILE_SIZE"
                        + " FROM"
                            + " (SELECT "
                                + " 0 ID"
                                + " , space_name || '-0-0' dbfile_name"
                                + " , space_id"
                                + " , current_size "
                                + " , dbfile_size"
                            + " FROM v$mem_tablespaces"
                            + " UNION ALL"
                            + " SELECT "
                                + " 1 ID"
                                + " , space_name || '-1-0' dbfile_name"
                                + " , space_id"
                                + " , current_size "
                                + " , dbfile_size"
                            + " FROM v$mem_tablespaces"
                            + " ) mt,"
                             + " v$mem_tablespace_checkpoint_paths p"
                        + " WHERE"
                            + " p.space_id = mt.space_id AND mt.space_id = ?"
                        + " ORDER BY ID ASC";
            } else {
                qry = "SELECT * FROM V$DATAFILES WHERE SPACEID = ? ORDER BY NAME";
            }
            
            final JDBCPreparedStatement dbStat = session.prepareStatement(qry);
            dbStat.setInt(1, owner.id);
            return dbStat;
        }

        @Override
        protected AltibaseDataFile fetchObject(
                @NotNull JDBCSession session, 
                @NotNull AltibaseTablespace owner, 
                @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            if (owner.isMemTbs()) {
                return new AltibaseDataFile4Mem(owner, resultSet);
            } else {
                return new AltibaseDataFile4Disk(owner, resultSet);
            }
        }
    }

    /**
     * Return tables belongs to this tablespace.
     */
    @Association
    public Collection<AltibaseTablespaceObj4Table> getAltibaseTablespaceObj4Tables(DBRProgressMonitor monitor) throws DBException {
        return tablePartnCache.getAllObjects(monitor, this);
    }

    /**
     *  Returns Table and partition belongs to this partition  
     */
    static class TablePartnCache extends JDBCObjectCache<AltibaseTablespace, AltibaseTablespaceObj4Table> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(
                @NotNull JDBCSession session, 
                @NotNull AltibaseTablespace owner) throws SQLException {
            
            String qry = 
                    "SELECT * FROM"
                    + " ("
                    + " SELECT"
                        + " u.user_name, t.table_name AS obj_name, null as partition_name"
                    + " FROM"
                        + " system_.sys_users_ u, system_.sys_tables_ t"
                    + " WHERE"
                        + " u.user_id = t.user_id AND (t.table_type = 'T' OR t.table_type = 'Q') AND "
                        + " t.is_partitioned = 'F' AND t.tbs_id = ?"
                    + " UNION ALL"
                    + " SELECT"
                        + " u.user_name, t.table_name AS obj_name, tp.partition_name"
                    + " FROM"
                        + " system_.sys_users_ u, system_.sys_tables_ t, system_.sys_table_partitions_ tp"
                    + " WHERE"
                        + " u.user_id = t.user_id AND (t.table_type = 'T' OR t.table_type = 'Q') AND "
                        + " t.is_partitioned = 'T' AND t.table_id = tp.table_id AND tp.tbs_id = ?"
                + " )"
                + " ORDER BY 1,2,3";

            
            final JDBCPreparedStatement dbStat = session.prepareStatement(qry);
            dbStat.setInt(1, owner.id);
            dbStat.setInt(2, owner.id);
            return dbStat;
        }

        @Override
        protected AltibaseTablespaceObj4Table fetchObject(
                @NotNull JDBCSession session, 
                @NotNull AltibaseTablespace owner, 
                @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new AltibaseTablespaceObj4Table(owner, resultSet);
        }
    }
    
    /**
     *  Returns Table and partition belongs to this partition  
     */

    @Association
    public Collection<AltibaseTablespaceObj4Index> getAltibaseTablespaceObj4Indexes(DBRProgressMonitor monitor) throws DBException {
        return indexPartnCache.getAllObjects(monitor, this);
    }

    static class IndexPartnCache extends JDBCObjectCache<AltibaseTablespace, AltibaseTablespaceObj4Index> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(
                @NotNull JDBCSession session, 
                @NotNull AltibaseTablespace owner) throws SQLException {
            
            String qry = 
                    "SELECT * FROM"
                    + " ("
                    + " SELECT"
                        + " u.user_name"
                        + " ,i.index_name AS obj_name"
                        + " ,NULL AS partition_name"
                        + " , ut.user_name AS table_schema"
                        + " , t.table_name"
                    + " FROM"
                        + " system_.sys_users_ u, system_.sys_indices_ i, system_.sys_users_ ut, system_.sys_tables_ t"
                    + " WHERE"
                        + " u.user_id = i.user_id AND i.is_partitioned = 'F'"
                        + " AND i.table_id = t.table_id AND ut.user_id = t.user_id"
                        + " AND i.tbs_id = ?"
                    + " UNION ALL "
                    + " SELECT"
                        + " u.user_name"
                        + " ,i.index_name AS obj_name"
                        + " ,ip.index_partition_name"
                        + " , ut.user_name AS table_schema"
                        + " , t.table_name"
                    + " FROM"
                        + " system_.sys_users_ u, system_.sys_indices_ i, system_.sys_index_partitions_ ip, "
                        + " system_.sys_users_ ut, system_.sys_tables_ t"
                    + " WHERE"
                        + " u.user_id = ip.user_id"
                        + " AND i.index_id = ip.index_id"
                        + " AND i.is_partitioned = 'T'"
                        + " AND i.table_id = t.table_id AND ut.user_id = t.user_id"
                        + " AND ip.tbs_id = ?"
                    + " )"
                    + " ORDER BY 1, 2, 3";

            final JDBCPreparedStatement dbStat = session.prepareStatement(qry);
            dbStat.setInt(1, owner.id);
            dbStat.setInt(2, owner.id);
            return dbStat;
        }

        @Override
        protected AltibaseTablespaceObj4Index fetchObject(
                @NotNull JDBCSession session, 
                @NotNull AltibaseTablespace owner, 
                @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new AltibaseTablespaceObj4Index(owner, resultSet);
        }
    }

    static Object resolveTablespaceReference(DBRProgressMonitor monitor, DBSObjectLazy<AltibaseDataSource> referrer, 
            @Nullable Object propertyId) throws DBException {
        final AltibaseDataSource dataSource = referrer.getDataSource();
        return AltibaseUtils.resolveLazyReference(monitor, dataSource, dataSource.tablespaceCache, referrer, propertyId);
    }

    public static class TablespaceReferenceValidator implements IPropertyCacheValidator<DBSObjectLazy<AltibaseDataSource>> {
        @Override
        public boolean isPropertyCached(DBSObjectLazy<AltibaseDataSource> object, Object propertyId) {
            return object.getLazyReference(propertyId) instanceof AltibaseTablespace ||
                    object.getLazyReference(propertyId) == null ||
                    object.getDataSource().tablespaceCache.isFullyCached();
        }
    }
}
