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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.altibase.AltibaseConstants;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.ext.generic.model.GenericTableColumn;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBPObjectStatistics;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDPseudoAttribute;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.ByteNumberFormat;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

public class AltibaseTable extends GenericTable implements DBPNamedObject2, DBPObjectStatistics {

    private static final Log log = Log.getLog(AltibaseTable.class);
    
    private transient volatile Long[] tableSize;
    private static final int SIZE_IDX_MEM = 0;
    private static final int SIZE_IDX_DISK = 1;
    
    private String tablespace;
    private boolean partitioned;
    
    private final TablePrivCache tablePrivCache = new TablePrivCache();
    
    public AltibaseTable(GenericStructContainer container, String tableName, String tableType, JDBCResultSet dbResult) {
        super(container, tableName, tableType, dbResult);
    }

    @Override
    protected boolean isTruncateSupported() {
        return true;
    }

    /*
     * In order to use a conversion function for geometry type.
     */
    @Override
    protected void appendSelectSource(
        DBRProgressMonitor monitor,
        StringBuilder query,
        String tableAlias,
        DBDPseudoAttribute rowIdAttribute
    ) {
        String tableAliasName;
        try {
            tableAliasName = (tableAlias != null) ? tableAlias + "." : "";

            List<? extends GenericTableColumn> attributes = getAttributes(monitor);
            boolean hasGeometryColumns = CommonUtils.safeCollection(attributes).stream()
                .anyMatch(e -> AltibaseConstants.TYPE_NAME_GEOMETRY.equalsIgnoreCase(e.getTypeName()));
            if (!hasGeometryColumns) {
                super.appendSelectSource(monitor, query, tableAlias, rowIdAttribute);
                return;
            }
            // Use this hack for geometry columns data reading
            int i = 0;
            for (GenericTableColumn col : CommonUtils.safeCollection(attributes)) {
                String columnName = DBUtils.getQuotedIdentifier(col);
                if (i++ > 0) {
                    query.append(",");
                }

                if (AltibaseConstants.TYPE_NAME_GEOMETRY.equalsIgnoreCase(col.getTypeName())) {
                    query.append("ASEWKT(").append(tableAliasName).append(columnName).append(", 32000) as ").append(columnName);
                } else {
                    query.append(tableAliasName).append(columnName).append(" as ").append(columnName);
                }
            }
        } catch (DBException e) {
            log.warn("Can't read table attributes.", e);
        }
    }

    @Property(viewable = false, hidden = true)
    public String getTableType() {
        return super.getTableType();
    }
    
    @Property(viewable = true, order = 20, editable = false, formatter = ByteNumberFormat.class, category = DBConstants.CAT_STATISTICS)
    public Long getTableSize(DBRProgressMonitor monitor) throws DBCException {
        if (!hasStatistics()) {
            loadSize(monitor);
        }

        return tableSize[SIZE_IDX_MEM] + tableSize[SIZE_IDX_DISK];
    }
    
    @Property(viewable = true, order = 22, editable = false, formatter = ByteNumberFormat.class)
    public Long getTableSizeInMemory(DBRProgressMonitor monitor) throws DBCException {
        if (!hasStatistics()) {
            loadSize(monitor);
        }

        return tableSize[SIZE_IDX_MEM];
    }
    
    @Property(viewable = true, order = 23, editable = false, formatter = ByteNumberFormat.class)
    public Long getTableSizeInDisk(DBRProgressMonitor monitor) throws DBCException {
        if (!hasStatistics()) {
            loadSize(monitor);
        }

        return tableSize[SIZE_IDX_DISK];
    }
    
    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        tableSize = null;
        getTableSize(monitor);
        getTablespace(monitor);

        tablePrivCache.clearCache();
        
        return super.refreshObject(monitor);
    }
    
    ///////////////////////////////////
    // Tablespace
    @Property(viewable = true, order = 15, editable = false)
    public String getTablespace(DBRProgressMonitor monitor) throws DBException {
        
        if (tablespace != null) {
            return tablespace;
        }
        
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load tablespace")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT tbs_name, is_partitioned FROM system_.sys_tables_ t, system_.sys_users_ u "
                + "WHERE u.user_id = t.user_id AND u.user_name = ? AND t.table_name = ?")) {
                dbStat.setString(1, getSchema().getName());
                dbStat.setString(2, getName());

                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        tablespace = JDBCUtils.safeGetString(dbResult, 1);
                        partitioned = JDBCUtils.safeGetBoolean(dbResult, 2, AltibaseConstants.RESULT_T_VALUE);
                    }
                }
            }
        } catch (Exception e) {
            throw new DBCException("Error reading tablespace name", e);
        }
        
        return tablespace;
    }
    
    @Property(viewable = true, order = 16, editable = false)
    public boolean getPartitionedTable(DBRProgressMonitor monitor) throws DBException {
        if (tablespace == null) {
            getTablespace(monitor);
        }
        
        return partitioned;
    }
    
    ///////////////////////////////////
    // Statistics
    
    @Override
    public boolean hasStatistics() {
        return tableSize != null;
    }

    @Override
    public long getStatObjectSize() {
        return (!hasStatistics()) ? 0 : tableSize[SIZE_IDX_MEM] + tableSize[SIZE_IDX_DISK];
    }
    
    private void loadSize(DBRProgressMonitor monitor) throws DBCException {
        resetSize();
        
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load table status")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT memory_size, disk_size FROM system_.sys_table_size_ WHERE USER_NAME = ? AND TABLE_NAME = ?")) {
                dbStat.setString(1, getSchema().getName());
                dbStat.setString(2, getName());

                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        fetchTableSize(dbResult);
                    }
                }
            }
        } catch (Exception e) {
            throw new DBCException("Error reading table statistics", e);
        } finally {
            if (!hasStatistics()) {
                resetSize();
            }
        }
    }
    
    protected void fetchTableSize(JDBCResultSet dbResult) throws SQLException {
        if (this.tableSize == null) {
            resetSize();
        }
        tableSize[SIZE_IDX_MEM] = (long) JDBCUtils.safeGetLong(dbResult, "MEMORY_SIZE");
        tableSize[SIZE_IDX_DISK] = (long) JDBCUtils.safeGetLong(dbResult, "DISK_SIZE");
    }

    protected void resetSize() {
        tableSize = new Long[2];
        for (int i = 0; i < 2; i++) {
            tableSize[i] = 0L; 
        }
    }
    
    @Nullable
    @Override
    public DBPPropertySource getStatProperties() {
        return null;
    }
    
    /**
     * Returns object privileges on a table: SELECT, UPDATE, INSERT, DELETE
     */
    @Association
    public Collection<AltibasePrivTable> getTablePrivs(DBRProgressMonitor monitor) throws DBException {
        return tablePrivCache.getAllObjects(monitor, this);
    }
    
    static class TablePrivCache extends JDBCObjectCache<AltibaseTable, AltibasePrivTable> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, 
                @NotNull AltibaseTable tableBase) throws SQLException {
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT"
                    + " grantor.user_name AS grantor_name"
                    + " , grantee.user_name AS grantee_name"
                    + " , DECODE(grantee.user_type, 'U', 'User', 'R', 'Role') as grantee_type"
                    + " ,p.priv_name AS priv_name"
                    + " ,g.with_grant_option AS with_grant_option"
                + " FROM"
                    + " system_.sys_users_ schema"
                    + " ,system_.sys_users_ grantor"
                    + " ,system_.sys_users_ grantee"
                    + " ,system_.sys_grant_object_ g"
                    + " ,system_.sys_privileges_ p"
                    + " ,system_.sys_tables_ t"
                + " WHERE"
                    + " schema.user_name = ? AND t.table_name = ?"
                    + " AND schema.user_id = t.user_id"
                    + " AND g.grantee_id = grantee.user_id"
                    + " AND g.grantor_id = grantor.user_id"
                    + " AND g.priv_id = p.priv_id"
                    + " AND p.priv_type = 1"
                    + " AND g.obj_id = t.table_id"
                + " ORDER BY priv_name, grantor_name, grantee_name");
            dbStat.setString(1, tableBase.getSchema().getName());
            dbStat.setString(2, tableBase.getName());
            return dbStat;
        }
        
        @Override
        protected AltibasePrivTable fetchObject(@NotNull JDBCSession session, @NotNull AltibaseTable tableBase, 
                @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new AltibasePrivTable(tableBase, resultSet);
        }
    }
}
