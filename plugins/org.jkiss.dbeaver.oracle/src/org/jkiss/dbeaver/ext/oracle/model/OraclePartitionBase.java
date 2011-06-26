/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;

/**
 * Oracle abstract partition
 */
public abstract class OraclePartitionBase<PARENT extends DBSObject> extends OracleObject<PARENT>
{
    public enum PartitionType {
        NONE,
        RANGE,
        HASH,
        SYSTEM,
        LIST,
    }

    public static class PartitionInfoBase {
        private PartitionType type;
        private PartitionType subType;
        private String tablespaceName;

        @Property(category = "Partitioning", name = "Partition Type", order = 120)
        public PartitionType getType()
        {
            return type;
        }

        @Property(category = "Partitioning", name = "Subpartition Type", order = 121)
        public PartitionType getSubType()
        {
            return subType;
        }

        @Property(category = "Partitioning", name = "Tablespace Name", order = 122)
        public String getTablespaceName()
        {
            return tablespaceName;
        }

        public PartitionInfoBase(ResultSet dbResult)
        {
            this.type = CommonUtils.valueOf(PartitionType.class, JDBCUtils.safeGetStringTrimmed(dbResult, "PARTITIONING_TYPE"));
            this.subType = CommonUtils.valueOf(PartitionType.class, JDBCUtils.safeGetStringTrimmed(dbResult, "SUBPARTITIONING_TYPE"));
            this.tablespaceName = JDBCUtils.safeGetStringTrimmed(dbResult, "DEF_TABLESPACE_NAME");
        }
    }

    protected OraclePartitionBase(PARENT parent, String name, boolean persisted)
    {
        super(parent, name, persisted);
    }

}
