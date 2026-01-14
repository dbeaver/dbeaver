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
package org.jkiss.dbeaver.ext.hana.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.model.struct.rdb.DBSTablePartition;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.ByteNumberFormat;
import java.sql.Timestamp;
import java.math.BigDecimal;

public class HANAPartition extends HANATable implements DBSTablePartition {

    private final HANATable parentTable;
    private int partId;
    private int recordCount;
    private String rangeStart;
    private String rangeEnd;
    private BigDecimal totalSize;
    private BigDecimal mainSize;
    private BigDecimal deltaSize;
    private Timestamp creationTime;
    private Timestamp lastDeltaMerge;
    private Timestamp lastLogReplay;
    private String loaded;
    private String loadUnit;

    public HANAPartition(
        @NotNull HANATable table,
        @NotNull int partId,
        @NotNull JDBCResultSet dbResult
    ) {
        super(table.getContainer(), String.valueOf(partId), table.getTableType(), dbResult);
        this.partId = partId;
        this.parentTable = table;

        this.recordCount = JDBCUtils.safeGetInt(dbResult, "RECORD_COUNT");
        this.rangeStart = JDBCUtils.safeGetString(dbResult, "LEVEL_1_RANGE_MIN_VALUE");
        this.rangeEnd = JDBCUtils.safeGetString(dbResult, "LEVEL_1_RANGE_MAX_VALUE");
        if (CommonUtils.isEmpty(this.rangeStart)) {
            this.rangeStart = JDBCUtils.safeGetString(dbResult, "LEVEL_2_RANGE_MIN_VALUE");
            this.rangeEnd = JDBCUtils.safeGetString(dbResult, "LEVEL_2_RANGE_MAX_VALUE");
        }
        if (CommonUtils.isEmpty(this.rangeStart)) {
            this.rangeStart = JDBCUtils.safeGetString(dbResult, "LEVEL_3_RANGE_MIN_VALUE");
            this.rangeEnd = JDBCUtils.safeGetString(dbResult, "LEVEL_3_RANGE_MAX_VALUE");
        }

        this.totalSize = JDBCUtils.safeGetBigDecimal(dbResult, "MEMORY_SIZE_IN_TOTAL");
        this.mainSize = JDBCUtils.safeGetBigDecimal(dbResult, "MEMORY_SIZE_IN_MAIN");
        this.deltaSize = JDBCUtils.safeGetBigDecimal(dbResult, "MEMORY_SIZE_IN_DELTA");        
        this.creationTime = JDBCUtils.safeGetTimestamp(dbResult, "CREATE_TIME");
        this.lastDeltaMerge = JDBCUtils.safeGetTimestamp(dbResult, "LAST_MERGE_TIME");
        this.lastLogReplay = JDBCUtils.safeGetTimestamp(dbResult, "LAST_REPLAY_LOG_TIME");
        this.loaded = JDBCUtils.safeGetString(dbResult, "LOADED");
        this.loadUnit = JDBCUtils.safeGetString(dbResult, "LOAD_UNIT");
    }

    @Property(viewable = true, order = 10)
    public int getPartId() {
        return partId;
    }

    @Property(viewable = true, order = 30)
    public String getRangeStart() {
        return rangeStart;
    }

    @Property(viewable = true, order = 35)
    public String getRangeEnd() {
        return rangeEnd;
    }

    @Property(viewable = true, order = 40)
    public int getRecordCount() {
        return recordCount;
    }

    @Property(viewable = true, order = 45, formatter = ByteNumberFormat.class)
    public BigDecimal getTotalSize() {
        return totalSize;
    }

    @Property(viewable = true, order = 50, formatter = ByteNumberFormat.class)
    public BigDecimal getMainSize() {
        return mainSize;
    }

    @Property(viewable = true, order = 55, formatter = ByteNumberFormat.class)
    public BigDecimal getDeltaSize() {
        return deltaSize;
    }

    @Property(viewable = true, order = 60)
    public Timestamp getCreationTime() {
        return creationTime;
    }

    @Property(viewable = true, order = 65)
    public Timestamp getLastDeltaMerge() {
        return lastDeltaMerge;
    }

    @Property(viewable = true, order = 70)
    public Timestamp getLastLogReplay() {
        return lastLogReplay;
    }

    @Property(viewable = true, order = 75)
    public String getLoaded() {
        return loaded;
    }

    @Property(viewable = true, order = 80)
    public String getLoadUnit() {
        return loadUnit;
    }

    @Override
    public boolean hasStatistics() {
        return false;
    }

    @Override
    public long getStatObjectSize() {
        return 0;
    }    

    @Nullable
    public DBPPropertySource getStatProperties() {
        return null;
    }

    @Override 
    public DBSTable getParentTable() {
        return this.parentTable;
    }

    @Override 
    public boolean isSubPartition() {
        return false;
    }
    
    @Override 
    public DBSTablePartition getPartitionParent() {
        return null;
    }

}
