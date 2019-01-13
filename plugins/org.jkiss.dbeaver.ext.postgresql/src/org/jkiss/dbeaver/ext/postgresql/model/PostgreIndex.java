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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableIndex;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * PostgreIndex
 */
public class PostgreIndex extends JDBCTableIndex<PostgreSchema, PostgreTableBase> implements PostgreObject, PostgreScriptObject
{
    private long indexId;
    private boolean isUnique;
    private boolean isPrimary; // Primary index - implicit
    private boolean isExclusion;
    private boolean isImmediate;
    private boolean isClustered;
    private boolean isValid;
    private boolean isCheckXMin;
    private boolean isReady;
    private String description;
    private List<PostgreIndexColumn> columns = new ArrayList<>();
    private long amId;
    private long tablespaceId;
    private String predicateExpression;
    private long indexRelSize;
    private long indexNumScans;

    private transient boolean isPrimaryKeyIndex;
    private transient String indexDDL;

    public PostgreIndex(DBRProgressMonitor monitor, PostgreTableBase parent, String indexName, ResultSet dbResult) throws DBException {
        super(
            parent.getContainer(),
            parent,
            indexName,
            DBSIndexType.UNKNOWN,
            true);
        this.indexId = JDBCUtils.safeGetLong(dbResult, "indexrelid");
        this.isUnique = JDBCUtils.safeGetBoolean(dbResult, "indisunique");
        this.isPrimary = JDBCUtils.safeGetBoolean(dbResult, "indisprimary");
        this.isExclusion = JDBCUtils.safeGetBoolean(dbResult, "indisexclusion");
        this.isImmediate = JDBCUtils.safeGetBoolean(dbResult, "indimmediate");
        this.isClustered = JDBCUtils.safeGetBoolean(dbResult, "indisclustered");
        this.isValid = JDBCUtils.safeGetBoolean(dbResult, "indisvalid");
        this.isCheckXMin = JDBCUtils.safeGetBoolean(dbResult, "indcheckxmin");
        this.isReady = JDBCUtils.safeGetBoolean(dbResult, "indisready");

        this.description = JDBCUtils.safeGetString(dbResult, "description");
        this.amId = JDBCUtils.safeGetLong(dbResult, "relam");
        this.tablespaceId = JDBCUtils.safeGetLong(dbResult, "reltablespace");

        if (getDataSource().isServerVersionAtLeast(7, 4)) {
            this.predicateExpression = JDBCUtils.safeGetString(dbResult, "pred_expr");
        }
        if (getDataSource().isServerVersionAtLeast(8, 1)) {
            this.indexRelSize = JDBCUtils.safeGetLong(dbResult, "index_rel_size");
        }
        this.indexNumScans = JDBCUtils.safeGetLong(dbResult, "index_num_scans");

        // Unique key indexes (including PK) are implicit. We don't want to show them separately
        if (this.isUnique) {
            PostgreTableConstraintBase ownerConstraint = parent.getConstraint(monitor, getName());
            if (ownerConstraint != null && ownerConstraint.getConstraintType().isUnique()) {
                this.isPrimaryKeyIndex = true;
            }
        }
    }

    public PostgreIndex(PostgreTableBase parent, String name, DBSIndexType indexType, boolean unique) {
        super(parent.getContainer(), parent, name, indexType, false);
        this.isUnique = unique;
    }

    @NotNull
    @Override
    public PostgreDataSource getDataSource()
    {
        return getTable().getDataSource();
    }

    @Override
    public PostgreDatabase getDatabase() {
        return getTable().getDatabase();
    }

    @Override
    public long getObjectId() {
        return indexId;
    }

    @Override
    @Property(viewable = true, order = 5)
    public boolean isUnique()
    {
        return isUnique;
    }

    @Override
    @Property(viewable = false, order = 20)
    public boolean isPrimary() {
        return isPrimary;
    }

    @Property(viewable = false, order = 21)
    public boolean isExclusion() {
        return isExclusion;
    }

    @Property(viewable = false, order = 22)
    public boolean isImmediate() {
        return isImmediate;
    }

    @Property(viewable = false, order = 23)
    public boolean isClustered() {
        return isClustered;
    }

    @Property(viewable = false, order = 24)
    public boolean isValid() {
        return isValid;
    }

    @Property(viewable = false, order = 25)
    public boolean isCheckXMin() {
        return isCheckXMin;
    }

    @Property(viewable = false, order = 26)
    public boolean isReady() {
        return isReady;
    }

    public DBSIndexType getIndexType()
    {
        return super.getIndexType();
    }

    @Property(viewable = true, order = 27)
    public String getPredicateExpression() {
        return predicateExpression;
    }

    @Property(viewable = true, order = 28)
    public long getIndexRelSize() {
        return indexRelSize;
    }

    @Property(viewable = false, order = 29)
    public long getIndexNumScans() {
        return indexNumScans;
    }

    @Nullable
    @Override
    @Property(viewable = true, multiline = true, order = 100)
    public String getDescription()
    {
        return description;
    }

    @Nullable
    @Property(viewable = true, order = 30)
    public PostgreAccessMethod getAccessMethod(DBRProgressMonitor monitor) throws DBException {
        if (amId <= 0) {
            return null;
        }
        return PostgreUtils.getObjectById(monitor, getTable().getDatabase().accessMethodCache, getTable().getDatabase(), amId);
    }

    @Nullable
    @Property(viewable = true, order = 31)
    public PostgreTablespace getTablespace(DBRProgressMonitor monitor) throws DBException {
        if (tablespaceId <= 0) {
            return getDatabase().getDefaultTablespace(monitor);
        }
        return getDatabase().getTablespace(monitor, tablespaceId);
    }

    @Override
    public List<PostgreIndexColumn> getAttributeReferences(DBRProgressMonitor monitor)
    {
        return columns;
    }

    public PostgreIndexColumn getColumn(String columnName)
    {
        return DBUtils.findObject(columns, columnName);
    }

    void setColumns(List<PostgreIndexColumn> columns)
    {
        this.columns = columns;
    }

    public void addColumn(PostgreIndexColumn column)
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

    public boolean isPrimaryKeyIndex() {
        return isPrimaryKeyIndex;
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException
    {
        if (indexDDL == null && isPersisted()) {
            try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Read index definition")) {
                indexDDL = JDBCUtils.queryString(session, "SELECT pg_catalog.pg_get_indexdef(?)", indexId);
            } catch (SQLException e) {
                throw new DBException(e, getDataSource());
            }
        }
        return indexDDL;
    }

    @Override
    public void setObjectDefinitionText(String sourceText) {

    }

    @Override
    public String toString() {
        return getName() + "(" + columns +")";
    }

}
