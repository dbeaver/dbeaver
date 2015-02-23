/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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

    @Property(viewable = true, editable = true, order = 2)
    public OracleSchema getSchema()
    {
        return schema;
    }

    @Property(viewable = true, editable = true, order = 3)
    public String getSegmentType()
    {
        return segmentType;
    }

    @Property(viewable = true, editable = true, order = 4)
    public String getPartitionName()
    {
        return partitionName;
    }

    @Property(viewable = true, editable = true, order = 5)
    public long getBytes()
    {
        return bytes;
    }

    @Property(viewable = true, editable = true, order = 6)
    public long getBlocks()
    {
        return blocks;
    }

    @Property(order = 7)
    public OracleDataFile getFile()
    {
        return file;
    }
}
