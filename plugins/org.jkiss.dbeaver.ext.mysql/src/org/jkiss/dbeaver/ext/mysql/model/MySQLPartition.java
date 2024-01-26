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
package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.data.DBDPseudoAttribute;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.model.struct.rdb.DBSTablePartition;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * MySQLPartition
 */
public class MySQLPartition extends MySQLTable implements DBSTablePartition {

    private MySQLTable table;
    private MySQLPartition parent;
    private List<MySQLPartition> subPartitions;
    private int position;
    private String name;
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
        super(mySQLTable.getContainer(), dbResult);
        this.name = name;
        this.table = mySQLTable;
        setPartition(true);
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

    // Copy constructor
    protected MySQLPartition(
        DBRProgressMonitor monitor,
        MySQLTable table,
        MySQLPartition source,
        DBSEntity sourceEntity
    ) throws DBException {
        super(monitor, table.getContainer(), sourceEntity);
        setPartition(true);
        this.name = source.name;
        this.table = source.table;
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

    @NotNull
    @Override
    public DBSTable getParentTable() {
        return table;
    }

    @Nullable
    @Override
    public MySQLPartition getPartitionParent() {
        return parent;
    }

    @Association
    public List<MySQLPartition> getSubPartitions()
    {
        return subPartitions;
    }

    @Override
    public boolean isSubPartition() {
        return parent != null;
    }



    @NotNull
    @Override
    @Property(viewable = true, editable = false, updatable = false, order = 1)
    public String getName()
    {
        return name;
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

    @Property(viewable = true, length = PropertyLength.MULTILINE, order = 16)
    public String getComment()
    {
        return comment;
    }

    @Property(viewable = true, order = 17)
    public String getNodegroup()
    {
        return nodegroup;
    }

    @Property(category = DBConstants.CAT_STATISTICS, viewable = true, order = 6)
    public long getTableRows()
    {
        return tableRows;
    }

    @Property(category = DBConstants.CAT_STATISTICS, viewable = true, order = 7)
    public long getAvgRowLength()
    {
        return avgRowLength;
    }

    @Property(category = DBConstants.CAT_STATISTICS, viewable = true, order = 8)
    public long getDataLength()
    {
        return dataLength;
    }

    @Property(category = DBConstants.CAT_STATISTICS, viewable = true, order = 9)
    public long getMaxDataLength()
    {
        return maxDataLength;
    }

    @Property(category = DBConstants.CAT_STATISTICS, viewable = true, order = 10)
    public long getIndexLength()
    {
        return indexLength;
    }

    @Property(category = DBConstants.CAT_STATISTICS, viewable = true, order = 11)
    public long getDataFree()
    {
        return dataFree;
    }

    @Property(category = DBConstants.CAT_STATISTICS, viewable = false, order = 12)
    public Date getCreateTime()
    {
        return createTime;
    }

    @Property(category = DBConstants.CAT_STATISTICS, viewable = false, order = 13)
    public Date getUpdateTime()
    {
        return updateTime;
    }

    @Property(category = DBConstants.CAT_STATISTICS, viewable = false, order = 14)
    public Date getCheckTime()
    {
        return checkTime;
    }

    @Property(category = DBConstants.CAT_STATISTICS, viewable = true, order = 15)
    public long getChecksum()
    {
        return checksum;
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        return table.getObjectDefinitionText(monitor, options);
    }

    @Override
    protected boolean needAliasInSelect(
        @Nullable DBDDataFilter dataFilter,
        @Nullable DBDPseudoAttribute rowIdAttribute,
        @NotNull DBPDataSource dataSource
    ) {
        return false;
    }

    @NotNull
    @Override
    protected String getTableName() {
        return table.getFullyQualifiedName(DBPEvaluationContext.DML);
    }

    @Override
    protected void appendExtraSelectParameters(@NotNull StringBuilder query) {
        query.append(" PARTITION (").append(DBUtils.getQuotedIdentifier(this)).append(")");
    }
}
