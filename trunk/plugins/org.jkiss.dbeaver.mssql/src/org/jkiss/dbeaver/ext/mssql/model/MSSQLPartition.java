/*
 * Copyright (C) 2010-2013 Serge Rieder
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
package org.jkiss.dbeaver.ext.mssql.model;

import org.jkiss.dbeaver.ext.mssql.MSSQLConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableObject;
import org.jkiss.dbeaver.model.meta.Property;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * MSSQLPartition
 */
public class MSSQLPartition extends JDBCTableObject<MSSQLTable>
{
    private MSSQLPartition parent;
    private List<MSSQLPartition> subPartitions;
    private int position;
    private String method;
    private String expression;
    private String description;
    private long tableRows;
    private long avgRowLength;
    private long dataLength;
    private long maxDataLength;
    private long indexLength;
    private long dataFree;
    private Date createTime;
    private Date updateTime;
    private Date checkTime;
    private long checksum;
    private String comment;
    private String nodegroup;

    protected MSSQLPartition(MSSQLTable mySQLTable, MSSQLPartition parent, String name, ResultSet dbResult)
    {
        super(mySQLTable, name, true);
        this.parent = parent;
        if (parent != null) {
            parent.addSubPartitions(this);
        }
        this.position = JDBCUtils.safeGetInt(dbResult,
            parent == null ?
                MSSQLConstants.COL_PARTITION_ORDINAL_POSITION :
                MSSQLConstants.COL_SUBPARTITION_ORDINAL_POSITION);
        this.method = JDBCUtils.safeGetString(dbResult,
            parent == null ?
                MSSQLConstants.COL_PARTITION_METHOD :
                MSSQLConstants.COL_SUBPARTITION_METHOD);
        this.expression = JDBCUtils.safeGetString(dbResult,
            parent == null ?
                MSSQLConstants.COL_PARTITION_EXPRESSION :
                MSSQLConstants.COL_SUBPARTITION_EXPRESSION);
        this.description = JDBCUtils.safeGetString(dbResult, MSSQLConstants.COL_PARTITION_DESCRIPTION);
        this.tableRows = JDBCUtils.safeGetLong(dbResult, MSSQLConstants.COL_TABLE_ROWS);
        this.avgRowLength = JDBCUtils.safeGetLong(dbResult, MSSQLConstants.COL_AVG_ROW_LENGTH);
        this.dataLength = JDBCUtils.safeGetLong(dbResult, MSSQLConstants.COL_DATA_LENGTH);
        this.maxDataLength = JDBCUtils.safeGetLong(dbResult, MSSQLConstants.COL_MAX_DATA_LENGTH);
        this.indexLength = JDBCUtils.safeGetLong(dbResult, MSSQLConstants.COL_INDEX_LENGTH);
        this.dataFree = JDBCUtils.safeGetLong(dbResult, MSSQLConstants.COL_DATA_FREE);
        this.createTime = JDBCUtils.safeGetTimestamp(dbResult, MSSQLConstants.COL_CREATE_TIME);
        this.updateTime = JDBCUtils.safeGetTimestamp(dbResult, MSSQLConstants.COL_UPDATE_TIME);
        this.checkTime = JDBCUtils.safeGetTimestamp(dbResult, MSSQLConstants.COL_CHECK_TIME);
        this.checksum = JDBCUtils.safeGetLong(dbResult, MSSQLConstants.COL_CHECKSUM);
        this.comment = JDBCUtils.safeGetString(dbResult, MSSQLConstants.COL_PARTITION_COMMENT);
        this.nodegroup = JDBCUtils.safeGetString(dbResult, MSSQLConstants.COL_NODEGROUP);
    }

    protected MSSQLPartition(JDBCTableObject<MSSQLTable> source)
    {
        super(source);
    }

    private void addSubPartitions(MSSQLPartition partition)
    {
        if (subPartitions == null) {
            subPartitions = new ArrayList<MSSQLPartition>();
        }
        subPartitions.add(partition);
    }

    public MSSQLPartition getParent()
    {
        return parent;
    }

    public List<MSSQLPartition> getSubPartitions()
    {
        return subPartitions;
    }

    @Override
    public MSSQLDataSource getDataSource()
    {
        return getTable().getDataSource();
    }

    @Override
    @Property(viewable = true, order = 1)
    public String getName()
    {
        return super.getName();
    }

    @Property(viewable = true, order = 2)
    public int getPosition()
    {
        return position;
    }

    @Property(viewable = true, order = 3)
    public String getMethod()
    {
        return method;
    }

    @Property(viewable = true, order = 4)
    public String getExpression()
    {
        return expression;
    }

    @Override
    @Property(viewable = true, order = 5)
    public String getDescription()
    {
        return description;
    }

    @Property(viewable = true, order = 6)
    public long getTableRows()
    {
        return tableRows;
    }

    @Property(viewable = true, order = 7)
    public long getAvgRowLength()
    {
        return avgRowLength;
    }

    @Property(viewable = true, order = 8)
    public long getDataLength()
    {
        return dataLength;
    }

    @Property(viewable = true, order = 9)
    public long getMaxDataLength()
    {
        return maxDataLength;
    }

    @Property(viewable = true, order = 10)
    public long getIndexLength()
    {
        return indexLength;
    }

    @Property(viewable = true, order = 11)
    public long getDataFree()
    {
        return dataFree;
    }

    @Property(viewable = false, order = 12)
    public Date getCreateTime()
    {
        return createTime;
    }

    @Property(viewable = false, order = 13)
    public Date getUpdateTime()
    {
        return updateTime;
    }

    @Property(viewable = false, order = 14)
    public Date getCheckTime()
    {
        return checkTime;
    }

    @Property(viewable = true, order = 15)
    public long getChecksum()
    {
        return checksum;
    }

    @Property(viewable = true, order = 16)
    public String getComment()
    {
        return comment;
    }

    @Property(viewable = true, order = 17)
    public String getNodegroup()
    {
        return nodegroup;
    }

}
