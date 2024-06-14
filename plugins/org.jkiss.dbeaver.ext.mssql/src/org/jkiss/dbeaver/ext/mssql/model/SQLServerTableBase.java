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
package org.jkiss.dbeaver.ext.mssql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mssql.SQLServerUtils;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionSource;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.AbstractExecutionSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableColumn;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.dbeaver.model.struct.rdb.DBSManipulationType;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.Pair;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * MySQLTable base
 */
public abstract class SQLServerTableBase extends JDBCTable<SQLServerDataSource, SQLServerSchema>
    implements SQLServerObject, SQLServerExtendedPropertyOwner, DBPNamedObject2, DBPRefreshableObject, DBSObjectWithScript, DBPScriptObjectExt2, DBPSystemObject, DBSDataManipulatorExt
{
    private static final Log log = Log.getLog(SQLServerTableBase.class);

    private long objectId;
    private String type;
    private String typeDescription;
    private Date createDate;
    private Date lastUpdate;
    private String description;
    protected Long rowCount;

    private final SQLServerExtendedPropertyCache extendedPropertyCache = new SQLServerExtendedPropertyCache();

    protected SQLServerTableBase(SQLServerSchema schema)
    {
        super(schema, false);
    }

    // Copy constructor
    protected SQLServerTableBase(DBRProgressMonitor monitor, SQLServerSchema catalog, SQLServerTableBase source) throws DBException {
        super(catalog, source, false);
    }

    protected SQLServerTableBase(
        @NotNull SQLServerSchema catalog,
        @NotNull ResultSet dbResult,
        @NotNull String name)
    {
        super(catalog, name, true);

        this.objectId = JDBCUtils.safeGetLong(dbResult, "object_id");
        this.description = JDBCUtils.safeGetString(dbResult, "description");
        this.type = JDBCUtils.safeGetStringTrimmed(dbResult, "type");
        this.typeDescription = JDBCUtils.safeGetString(dbResult, "type_desc");
        this.createDate = JDBCUtils.safeGetTimestamp(dbResult, "create_date");
        this.lastUpdate = JDBCUtils.safeGetTimestamp(dbResult, "modify_date");
    }

    @Override
    @NotNull
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

    public String getType() {
        return type;
    }

    @Property(viewable = true, order = 6)
    public String getTypeDescription() {
        return typeDescription;
    }

    @Property(viewable = true, order = 7)
    public Date getCreateDate() {
        return createDate;
    }

    @Property(viewable = true, order = 8)
    public Date getLastUpdate() {
        return lastUpdate;
    }

    @Override
    public boolean isSystem() {
        return SQLServerObjectType.S.name().equals(type);
    }

    @Override
    @Property(viewable = true, editable = true, updatable = true, length = PropertyLength.MULTILINE, order = 100)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public List<SQLServerTableColumn> getAttributes(@NotNull DBRProgressMonitor monitor)
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
    public Collection<SQLServerTableIndex> getIndexes(@NotNull DBRProgressMonitor monitor)
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

    @Property(category = DBConstants.CAT_STATISTICS, viewable = false, expensive = true, order = 23)
    public Long getRowCount(DBRProgressMonitor monitor) throws DBCException
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
        if (!SQLServerUtils.supportsCrossDatabaseQueries(getDataSource())) {
            // Older Azure doesn't support database name in queries
            return DBUtils.getFullQualifiedName(getDataSource(),
                getSchema(),
                this);
        }
        if (isView() && context == DBPEvaluationContext.DDL) {
            return DBUtils.getFullQualifiedName(getDataSource(),
                getSchema(),
                this);
        } else {
            return DBUtils.getFullQualifiedName(getDataSource(),
                getDatabase(),
                getSchema(),
                this);
        }
    }

    ////////////////////////////////////////////////////////
    // Data manipulation handler

    @Override
    public void beforeDataChange(@NotNull DBCSession session, @NotNull DBSManipulationType type, @NotNull DBSAttributeBase[] attributes, @NotNull DBCExecutionSource source) throws DBCException {
        if (hasIdentityInsert(type, attributes)) {
            enableIdentityInsert(session, true);
        }
    }

    @Override
    public void afterDataChange(@NotNull DBCSession session, @NotNull DBSManipulationType type, @NotNull DBSAttributeBase[] attributes, @NotNull DBCExecutionSource source) throws DBCException {
        if (hasIdentityInsert(type, attributes)) {
            enableIdentityInsert(session, false);
        }
    }

    private void enableIdentityInsert(DBCSession session, boolean enable) throws DBCException {
        try {
            JDBCUtils.executeStatement(
                (JDBCSession)session,
                "SET IDENTITY_INSERT " + getFullyQualifiedName(DBPEvaluationContext.DML) + " " + (enable ? " ON" : "OFF"));
        } catch (SQLException e) {
            throw new DBCException(e, session.getExecutionContext());
        }
    }

    private boolean hasIdentityInsert(@NotNull DBSManipulationType type, @NotNull DBSAttributeBase[] attributes) {
        if (type == DBSManipulationType.INSERT) {
            for (DBSAttributeBase attr : attributes) {
                if (attr instanceof SQLServerTableColumn && ((SQLServerTableColumn) attr).isIdentity()) {
                    return true;
                } else if (attr.isAutoGenerated()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Nullable
    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        rowCount = null;
        if (supportsTriggers()) {
            getContainer().getTriggerCache().clearChildrenOf(this);
        }
        extendedPropertyCache.clearCache();
        return getContainer().getTableCache().refreshObject(monitor, getContainer(), this);
    }

    abstract boolean supportsTriggers();

    /**
     * Returns true only in case the table is a table and has clustered columnstore index
     */
    boolean isClustered(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (isView()) {
            return false;
        }
        Collection<SQLServerTableIndex> indexes = getIndexes(monitor);
        if (!CommonUtils.isEmpty(indexes)) {
            for (SQLServerTableIndex index : indexes) {
                if (index.getIndexType() == DBSIndexType.CLUSTERED && index.isColumnStore()) {
                    return true;
                }
            }
        }
        return false;
    }

    @NotNull
    @Association
    public List<SQLServerTableTrigger> getTriggers(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (!supportsTriggers()) {
            return Collections.emptyList();
        }
        SQLServerSchema schema = getSchema();
        List<SQLServerTableTrigger> triggers = new ArrayList<>();
        for (SQLServerTableTrigger trigger: schema.getTriggerCache().getAllObjects(monitor, schema)) {
            if (this == trigger.getTable()) {
                triggers.add(trigger);
            }
        }
        return triggers;
    }

    //////////////////////////////////////////////////
    // Extended Properties

    @Association
    @NotNull
    public Collection<SQLServerExtendedProperty> getExtendedProperties(@NotNull DBRProgressMonitor monitor) throws DBException {
        return extendedPropertyCache.getAllObjects(monitor, this);
    }

    @Override
    public long getMajorObjectId() {
        return getObjectId();
    }

    @Override
    public long getMinorObjectId() {
        return 0;
    }

    @Override
    public Pair<String, SQLServerObject> getExtendedPropertyObject(@NotNull DBRProgressMonitor monitor, int level) {
        switch (level) {
            case 0:
                return new Pair<>("Schema", getSchema());
            case 1:
                return new Pair<>("Table", this);
            default:
                return null;
        }
    }

    @NotNull
    @Override
    public SQLServerObjectClass getExtendedPropertyObjectClass() {
        return SQLServerObjectClass.OBJECT_OR_COLUMN;
    }

    @NotNull
    @Override
    public SQLServerExtendedPropertyCache getExtendedPropertyCache() {
        return extendedPropertyCache;
    }
}
