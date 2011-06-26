/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;

/**
 * Oracle segments (objects)
 */
public class OracleSegment<PARENT extends DBSObject> extends OracleObject<PARENT> {

    private String segmentType;
    private String partitionName;
    private long bytes;
    private long blocks;
    private OracleSchema schema;
    private OracleDataFile file;

    protected OracleSegment(DBRProgressMonitor monitor, PARENT parent, ResultSet dbResult) throws DBException
    {
        super(
            parent,
            JDBCUtils.safeGetStringTrimmed(dbResult, "SEGMENT_NAME"),
            true);
        this.segmentType = JDBCUtils.safeGetStringTrimmed(dbResult, "SEGMENT_TYPE");
        this.partitionName = JDBCUtils.safeGetStringTrimmed(dbResult, "PARTITION_NAME");
        this.bytes = JDBCUtils.safeGetLong(dbResult, "BYTES");
        this.blocks = JDBCUtils.safeGetLong(dbResult, "BLOCKS");
        final long fileNo = JDBCUtils.safeGetInt(dbResult, "RELATIVE_FNO");
        final Object tablespace = getTablespace(monitor);
        if (tablespace instanceof OracleTablespace) {
            this.file = ((OracleTablespace)tablespace).getFile(monitor, fileNo);
        }
        if (getDataSource().isAdmin()) {
            String ownerName = JDBCUtils.safeGetStringTrimmed(dbResult, "OWNER");
            if (!CommonUtils.isEmpty(ownerName)) {
                schema = getDataSource().getSchema(monitor, ownerName);
            }
        }
    }

    public Object getTablespace(DBRProgressMonitor monitor) throws DBException
    {
        if (parent instanceof OracleTablespace) {
            return parent;
        } else if (parent instanceof OraclePartitionBase) {
            return ((OraclePartitionBase) parent).getTablespace(monitor);
        } else {
            return null;
        }
    }

    @Property(name = "Owner", viewable = true, editable = true, order = 2)
    public OracleSchema getSchema()
    {
        return schema;
    }

    @Property(name = "Type", viewable = true, editable = true, order = 3)
    public String getSegmentType()
    {
        return segmentType;
    }

    @Property(name = "Partition", viewable = true, editable = true, order = 4)
    public String getPartitionName()
    {
        return partitionName;
    }

    @Property(name = "Bytes", viewable = true, editable = true, order = 5)
    public long getBytes()
    {
        return bytes;
    }

    @Property(name = "Blocks", viewable = true, editable = true, order = 6)
    public long getBlocks()
    {
        return blocks;
    }

    @Property(name = "File", order = 7)
    public OracleDataFile getFile()
    {
        return file;
    }
}
