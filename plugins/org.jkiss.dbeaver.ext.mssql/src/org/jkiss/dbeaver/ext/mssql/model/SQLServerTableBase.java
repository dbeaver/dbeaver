/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.AbstractExecutionSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableColumn;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.util.*;

/**
 * MySQLTable base
 */
public abstract class SQLServerTableBase extends JDBCTable<SQLServerDataSource, SQLServerSchema>
    implements SQLServerObject, DBPNamedObject2,DBPRefreshableObject, DBPSystemObject
{
    private static final Log log = Log.getLog(SQLServerTableBase.class);

    private static final String CAT_STATISTICS = "Statistics";

    private long objectId;
    private String type;
    private String description;
    private Long rowCount;

    protected SQLServerTableBase(SQLServerSchema schema)
    {
        super(schema, false);
    }

    // Copy constructor
    protected SQLServerTableBase(DBRProgressMonitor monitor, SQLServerSchema catalog, SQLServerTableBase source) throws DBException {
        super(catalog, source, false);
    }

    protected SQLServerTableBase(
        SQLServerSchema catalog,
        ResultSet dbResult)
    {
        super(catalog, JDBCUtils.safeGetString(dbResult, "name"), true);

        this.objectId = JDBCUtils.safeGetLong(dbResult, "object_id");
        this.description = JDBCUtils.safeGetString(dbResult, "description");
        this.type = JDBCUtils.safeGetStringTrimmed(dbResult, "type");
    }

    public SQLServerDatabase getDatabase() {
        return getSchema().getDatabase();
    }

    public SQLServerSchema getSchema() {
        return getContainer();
    }

    @Override
    public JDBCStructCache<SQLServerSchema, ? extends JDBCTable, ? extends JDBCTableColumn> getCache()
    {
        return getContainer().getTableCache();
    }

    @Override
    @Property(viewable = false, editable = false, order = 5)
    public long getObjectId() {
        return objectId;
    }

    @Property(order = 6)
    public String getType() {
        return type;
    }

    @Override
    public boolean isSystem() {
        return SQLServerObjectType.S.name().equals(type);
    }

    @Override
    @Property(viewable = true, editable = true, updatable = true, multiline = true, order = 100)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public Collection<SQLServerTableColumn> getAttributes(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        List<SQLServerTableColumn> childColumns = getContainer().getTableCache().getChildren(monitor, getContainer(), this);
        if (childColumns == null) {
            return Collections.emptyList();
        }
        List<SQLServerTableColumn> columns = new ArrayList<>(childColumns);
        columns.sort(DBUtils.orderComparator());
        return columns;
    }

    @Override
    public SQLServerTableColumn getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName)
        throws DBException
    {
        return getContainer().getTableCache().getChild(monitor, getContainer(), this, attributeName);
    }

    public SQLServerTableColumn getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull long columnId)
        throws DBException
    {
        for (SQLServerTableColumn col : getAttributes(monitor)) {
            if (col.getObjectId() == columnId) {
                return col;
            }
        }
        log.error("Column '" + columnId + "' not found in table '" + getFullyQualifiedName(DBPEvaluationContext.DML) + "'");
        return null;
    }

    @Override
    @Association
    public Collection<SQLServerTableIndex> getIndexes(DBRProgressMonitor monitor)
        throws DBException
    {
        return this.getContainer().getIndexCache().getObjects(monitor, getSchema(), this);
    }

    public SQLServerTableIndex getIndex(DBRProgressMonitor monitor, long indexId) throws DBException {
        for (SQLServerTableIndex index : getIndexes(monitor)) {
            if (index.getObjectId() == indexId) {
                return index;
            }
        }
        log.error("Index '" + indexId + "' not found in table '" + getFullyQualifiedName(DBPEvaluationContext.DML) + "'");
        return null;
    }

    public SQLServerTableIndex getIndex(DBRProgressMonitor monitor, String name) throws DBException {
        for (SQLServerTableIndex index : getIndexes(monitor)) {
            if (CommonUtils.equalObjects(name, index.getName())) {
                return index;
            }
        }
        log.error("Index '" + name + "' not found in table '" + getFullyQualifiedName(DBPEvaluationContext.DML) + "'");
        return null;
    }

    @Property(category = CAT_STATISTICS, viewable = false, expensive = true, order = 23)
    public Long getRowCount(DBRProgressMonitor monitor)
    {
        if (rowCount != null || !isPersisted()) {
            return rowCount;
        }
        // Query row count
        try (DBCSession session = DBUtils.openMetaSession(monitor, this, "Read row count")) {
            rowCount = countData(new AbstractExecutionSource(this, session.getExecutionContext(), this), session, null, DBSDataContainer.FLAG_NONE);
        } catch (DBException e) {
            log.debug("Can't fetch row count", e);
        }
        if (rowCount == null) {
            rowCount = -1L;
        }
        return rowCount;
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context)
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getDatabase(),
            getSchema(),
            this);
    }

}
