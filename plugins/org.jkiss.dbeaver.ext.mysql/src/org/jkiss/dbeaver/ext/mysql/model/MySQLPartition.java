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
package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableObject;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * MySQLPartition
 */
public class MySQLPartition extends JDBCTableObject<MySQLTable>
{
    private MySQLPartition parent;
    private List<MySQLPartition> subPartitions;
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

    protected MySQLPartition(MySQLTable mySQLTable, MySQLPartition parent, String name, ResultSet dbResult)
    {
        super(mySQLTable, name, true);
        this.parent = parent;
        if (parent != null) {
            parent.addSubPartitions(this);
        }
        this.position = JDBCUtils.safeGetInt(dbResult,
            parent == null ?
                MySQLConstants.COL_PARTITION_ORDINAL_POSITION :
                MySQLConstants.COL_SUBPARTITION_ORDINAL_POSITION);
        this.method = JDBCUtils.safeGetString(dbResult,
            parent == null ?
                MySQLConstants.COL_PARTITION_METHOD :
                MySQLConstants.COL_SUBPARTITION_METHOD);
        this.expression = JDBCUtils.safeGetString(dbResult,
            parent == null ?
                MySQLConstants.COL_PARTITION_EXPRESSION :
                MySQLConstants.COL_SUBPARTITION_EXPRESSION);
        this.description = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_PARTITION_DESCRIPTION);
        this.tableRows = JDBCUtils.safeGetLong(dbResult, MySQLConstants.COL_TABLE_ROWS);
        this.avgRowLength = JDBCUtils.safeGetLong(dbResult, MySQLConstants.COL_AVG_ROW_LENGTH);
        this.dataLength = JDBCUtils.safeGetLong(dbResult, MySQLConstants.COL_DATA_LENGTH);
        this.maxDataLength = JDBCUtils.safeGetLong(dbResult, MySQLConstants.COL_MAX_DATA_LENGTH);
        this.indexLength = JDBCUtils.safeGetLong(dbResult, MySQLConstants.COL_INDEX_LENGTH);
        this.dataFree = JDBCUtils.safeGetLong(dbResult, MySQLConstants.COL_DATA_FREE);
        this.createTime = JDBCUtils.safeGetTimestamp(dbResult, MySQLConstants.COL_CREATE_TIME);
        this.updateTime = JDBCUtils.safeGetTimestamp(dbResult, MySQLConstants.COL_UPDATE_TIME);
        this.checkTime = JDBCUtils.safeGetTimestamp(dbResult, MySQLConstants.COL_CHECK_TIME);
        this.checksum = JDBCUtils.safeGetLong(dbResult, MySQLConstants.COL_CHECKSUM);
        this.comment = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_PARTITION_COMMENT);
        this.nodegroup = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_NODEGROUP);
    }

    protected MySQLPartition(JDBCTableObject<MySQLTable> source)
    {
        super(source);
    }

    // Copy constructor
    protected MySQLPartition(DBRProgressMonitor monitor, MySQLTable table, MySQLPartition source)
    {
        super(table, source.getName(), false);
        this.position = source.position;
        this.method = source.method;
        this.expression = source.expression;
        this.description = source.description;
        this.comment = source.comment;
        this.nodegroup = source.nodegroup;
    }

    private void addSubPartitions(MySQLPartition partition)
    {
        if (subPartitions == null) {
            subPartitions = new ArrayList<>();
        }
        subPartitions.add(partition);
    }

    public MySQLPartition getParent()
    {
        return parent;
    }

    public List<MySQLPartition> getSubPartitions()
    {
        return subPartitions;
    }

    @NotNull
    @Override
    public MySQLDataSource getDataSource()
    {
        return getTable().getDataSource();
    }

    @NotNull
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

    @Nullable
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
