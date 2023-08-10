/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.ByteNumberFormat;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;

public class AltibaseTable extends GenericTable implements DBPNamedObject2, DBPObjectStatistics {

    private static final Log log = Log.getLog(AltibaseTable.class);
    
    private transient volatile Long[] tableSize;
    private static final int SIZE_IDX_MEM = 0;
    private static final int SIZE_IDX_DISK = 1;
    
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
    protected void appendSelectSource(DBRProgressMonitor monitor, StringBuilder query, 
            String tableAlias, DBDPseudoAttribute rowIdAttribute) {
        int i = 0;
        String tableAliasName = null;
        
        try {
            tableAliasName = (tableAlias != null) ? tableAlias + "." : "";
            
            for (GenericTableColumn col : CommonUtils.safeCollection(getAttributes(monitor))) {
                if (i++ > 0) {
                    query.append(",");
                }

                if (col.getTypeName().equalsIgnoreCase(AltibaseConstants.TYPE_NAME_GEOMETRY)) {
                    query.append("ASEWKT(").append(tableAliasName).append(col.getName()).append(", 32000) as ").append(col.getName());
                } else {
                    query.append(tableAliasName).append(col.getName()).append(" as ").append(col.getName());
                }
            }
        } catch (DBException e) {
            log.warn(e);
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

        return super.refreshObject(monitor);
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
            if (hasStatistics() == false) {
                resetSize();
            }
        }
    }
    
    protected void fetchTableSize(JDBCResultSet dbResult) throws SQLException {
        if (this.tableSize == null) {
            resetSize();
        }
        tableSize[SIZE_IDX_MEM] = (long) JDBCUtils.safeGetInt(dbResult, "MEMORY_SIZE");
        tableSize[SIZE_IDX_DISK] = (long) JDBCUtils.safeGetInt(dbResult, "DISK_SIZE");
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
}
