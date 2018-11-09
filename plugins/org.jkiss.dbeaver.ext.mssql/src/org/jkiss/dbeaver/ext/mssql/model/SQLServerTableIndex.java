/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.mssql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableIndex;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndexColumn;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * SQLServerTableIndex
 */
public class SQLServerTableIndex extends JDBCTableIndex<SQLServerSchema, SQLServerTable> implements SQLServerObject, DBPNamedObject2
{
    private boolean nonUnique;
    private String additionalInfo;
    private String indexComment;
    private long cardinality;
    private List<SQLServerTableIndexColumn> columns;
    private long objectId;

    public SQLServerTableIndex(
        SQLServerTable table,
        boolean nonUnique,
        String indexName,
        DBSIndexType indexType,
        String comment,
        boolean persisted)
    {
        super(table.getContainer(), table, indexName, indexType, persisted);
        this.nonUnique = nonUnique;
        this.indexComment = comment;
    }

    // Copy constructor
    SQLServerTableIndex(DBRProgressMonitor monitor, SQLServerTable table, DBSTableIndex source) throws DBException {
        super(table.getContainer(), table, source, false);
        this.nonUnique = !source.isUnique();
        this.indexComment = source.getDescription();
        if (source instanceof SQLServerTableIndex) {
            this.cardinality = ((SQLServerTableIndex)source).cardinality;
            this.additionalInfo = ((SQLServerTableIndex)source).additionalInfo;
        }
        List<? extends DBSTableIndexColumn> columns = source.getAttributeReferences(monitor);
        if (columns != null) {
            this.columns = new ArrayList<>(columns.size());
            for (DBSTableIndexColumn sourceColumn : columns) {
                this.columns.add(new SQLServerTableIndexColumn(monitor, this, sourceColumn));
            }
        }
    }

    public SQLServerTableIndex(SQLServerTable parent, String indexName, DBSIndexType indexType, ResultSet dbResult) {
        super(
            parent.getContainer(),
            parent,
            indexName,
            indexType,
            true);
        this.objectId = JDBCUtils.safeGetLong(dbResult, "index_id");
//        this.nonUnique = JDBCUtils.safeGetInt(dbResult, MySQLConstants.COL_NON_UNIQUE) != 0;
//        this.cardinality = JDBCUtils.safeGetLong(dbResult, "cardinality");
//        this.indexComment = JDBCUtils.safeGetString(dbResult, "INDEX_COMMENT");
//        this.additionalInfo = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_COMMENT);
    }

    @NotNull
    @Override
    public SQLServerDataSource getDataSource()
    {
        return getTable().getDataSource();
    }

    @Override
    @Property(viewable = true, order = 5)
    public boolean isUnique()
    {
        return !nonUnique;
    }

    @Nullable
    @Override
    @Property(viewable = true, multiline = true, order = 100)
    public String getDescription()
    {
        return indexComment;
    }

    @Property(viewable = true, order = 20)
    public long getCardinality() {
        return cardinality;
    }

    @Property(viewable = false, order = 30)
    public String getAdditionalInfo() {
        return additionalInfo;
    }

    @Override
    public List<SQLServerTableIndexColumn> getAttributeReferences(DBRProgressMonitor monitor)
    {
        return columns;
    }

    public SQLServerTableIndexColumn getColumn(String columnName)
    {
        return DBUtils.findObject(columns, columnName);
    }

    void setColumns(List<SQLServerTableIndexColumn> columns)
    {
        this.columns = columns;
    }

    public void addColumn(SQLServerTableIndexColumn column)
    {
        if (columns == null) {
            columns = new ArrayList<>();
        }
        columns.add(column);
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context)
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getTable().getContainer(),
            this);
    }

    @Override
    public boolean isPrimary() {
        return false;////MySQLConstants.INDEX_PRIMARY.equals(getName());
    }

    @Override
    public long getObjectId() {
        return objectId;
    }
}
