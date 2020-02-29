/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.LazyProperty;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectLazy;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.Timestamp;

/**
 * Oracle abstract partition
 */
public abstract class OraclePartitionBase<PARENT extends DBSObject> extends OracleObject<PARENT> implements DBSObjectLazy
{
    public enum PartitionType {
        NONE,
        RANGE,
        HASH,
        SYSTEM,
        LIST,
    }

    public static final String CAT_PARTITIONING = "Partitioning";

    public static class PartitionInfoBase {
        private PartitionType partitionType;
        private PartitionType subpartitionType;
        private Object partitionTablespace;

        @Property(category = CAT_PARTITIONING, order = 120)
        public PartitionType getPartitionType()
        {
            return partitionType;
        }

        @Property(category = CAT_PARTITIONING, order = 121)
        public PartitionType getSubpartitionType()
        {
            return subpartitionType;
        }

        @Property(category = CAT_PARTITIONING, order = 122)
        public Object getPartitionTablespace()
        {
            return partitionTablespace;
        }

        public PartitionInfoBase(DBRProgressMonitor monitor, OracleDataSource dataSource, ResultSet dbResult) throws DBException
        {
            this.partitionType = CommonUtils.valueOf(PartitionType.class, JDBCUtils.safeGetStringTrimmed(dbResult, "PARTITIONING_TYPE"));
            this.subpartitionType = CommonUtils.valueOf(PartitionType.class, JDBCUtils.safeGetStringTrimmed(dbResult, "SUBPARTITIONING_TYPE"));
            this.partitionTablespace = JDBCUtils.safeGetStringTrimmed(dbResult, "DEF_TABLESPACE_NAME");
            if (dataSource.isAdmin()) {
                this.partitionTablespace = dataSource.tablespaceCache.getObject(monitor, dataSource, (String) partitionTablespace);
            }
        }
    }

    private int position;
    private String highValue;
    private boolean usable;
    private Object tablespace;
    private long numRows;
    private long sampleSize;
    private Timestamp lastAnalyzed;

    protected OraclePartitionBase(PARENT parent, boolean subpartition, ResultSet dbResult)
    {
        super(
            parent,
            subpartition ?
                JDBCUtils.safeGetString(dbResult, "SUBPARTITION_NAME") :
                JDBCUtils.safeGetString(dbResult, "PARTITION_NAME"),
            true);
        this.highValue = JDBCUtils.safeGetString(dbResult, "HIGH_VALUE");
        this.position = subpartition ?
            JDBCUtils.safeGetInt(dbResult, "SUBPARTITION_POSITION") :
            JDBCUtils.safeGetInt(dbResult, "PARTITION_POSITION");
        this.usable = "USABLE".equals(JDBCUtils.safeGetString(dbResult, "STATUS"));
        this.tablespace = JDBCUtils.safeGetStringTrimmed(dbResult, "TABLESPACE_NAME");

        this.numRows = JDBCUtils.safeGetLong(dbResult, "NUM_ROWS");
        this.sampleSize = JDBCUtils.safeGetLong(dbResult, "SAMPLE_SIZE");
        this.lastAnalyzed = JDBCUtils.safeGetTimestamp(dbResult, "LAST_ANALYZED");
    }

    @Override
    public Object getLazyReference(Object propertyId)
    {
        return tablespace;
    }

    @Property(viewable = true, order = 10)
    public int getPosition()
    {
        return position;
    }

    @Property(viewable = true, order = 11)
    public boolean isUsable()
    {
        return usable;
    }

    @Property(viewable = true, order = 12)
    @LazyProperty(cacheValidator = OracleTablespace.TablespaceReferenceValidator.class)
    public Object getTablespace(DBRProgressMonitor monitor) throws DBException
    {
        return OracleTablespace.resolveTablespaceReference(monitor, this, null);
    }

    @Property(viewable = true, order = 30)
    public String getHighValue()
    {
        return highValue;
    }

    @Property(viewable = true, order = 40)
    public long getNumRows()
    {
        return numRows;
    }

    @Property(viewable = true, order = 41)
    public long getSampleSize()
    {
        return sampleSize;
    }

    @Property(viewable = true, order = 42)
    public Timestamp getLastAnalyzed()
    {
        return lastAnalyzed;
    }
}
