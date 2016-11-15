/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
