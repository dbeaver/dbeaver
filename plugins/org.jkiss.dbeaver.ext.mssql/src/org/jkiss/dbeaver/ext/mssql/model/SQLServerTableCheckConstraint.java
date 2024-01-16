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
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractTableConstraint;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableCheckConstraint;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableConstraintColumn;

import java.util.List;
import java.util.Map;

/**
 * SQLServerTableCheckConstraint
 */
public class SQLServerTableCheckConstraint extends AbstractTableConstraint<SQLServerTable, DBSTableConstraintColumn>
    implements SQLServerObject, DBPScriptObject, DBSTableCheckConstraint
{
    private boolean disabled;
    private String definition;
    private long objectId;

    public SQLServerTableCheckConstraint(SQLServerTable table, JDBCResultSet dbResult) {
        super(table, JDBCUtils.safeGetString(dbResult, "name"), null, DBSEntityConstraintType.CHECK);
        this.objectId = JDBCUtils.safeGetLong(dbResult, "object_id");
        this.disabled = JDBCUtils.safeGetInt(dbResult, "is_disabled") != 0;
        this.definition = JDBCUtils.safeGetString(dbResult, "definition");
    }

    public SQLServerTableCheckConstraint(SQLServerTable table) {
        super(table, null, null, DBSEntityConstraintType.CHECK);
        this.objectId = -1;
        this.disabled = false;
        this.definition = null;
    }

    @Property(viewable = false, editable = true, order = 10)
    public boolean isDisabled() {
        return disabled;
    }

    @NotNull
    @Override
    public SQLServerDataSource getDataSource() {
        return getTable().getDataSource();
    }

    @Property(viewable = false, editable = true, order = 80)
    @Override
    public long getObjectId() {
        return objectId;
    }

    @Override
    @Property(viewable = true, editable = true, length = PropertyLength.MULTILINE, order = 20)
    public String getCheckConstraintDefinition() {
        return definition;
    }

    @Override
    public void setCheckConstraintDefinition(String expression) {
        this.definition = expression;
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        return getCheckConstraintDefinition();
    }

    @Nullable
    @Override
    public List<DBSTableConstraintColumn> getAttributeReferences(DBRProgressMonitor monitor) throws DBException {
        return null;
    }

    @Override
    public void setAttributeReferences(List<DBSTableConstraintColumn> dbsTableConstraintColumns) throws DBException {

    }

    @Nullable
    @Override
    public SQLServerDatabase getDatabase() {
        return getTable().getDatabase();
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context) {
        return DBUtils.getFullQualifiedName(getDataSource(), getTable(), this);
    }
}
