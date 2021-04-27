/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.IPropertyValueTransformer;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.format.SQLFormatUtils;
import org.jkiss.dbeaver.model.struct.DBSActionTiming;
import org.jkiss.dbeaver.model.struct.DBSEntityElement;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.struct.rdb.DBSManipulationType;
import org.jkiss.dbeaver.model.struct.rdb.DBSTrigger;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * PostgreTrigger
 */
public class PostgreTrigger implements DBSTrigger, DBSEntityElement, DBPQualifiedObject, PostgreObject, PostgreScriptObject, DBPStatefulObject, DBPScriptObjectExt2
{
    private static final Log log = Log.getLog(PostgreTrigger.class);

    /* Bits within tgtype */
    public static final int TRIGGER_TYPE_ROW        = (1 << 0);
    public static final int TRIGGER_TYPE_BEFORE     = (1 << 1);
    public static final int TRIGGER_TYPE_INSERT     = (1 << 2);
    public static final int TRIGGER_TYPE_DELETE     = (1 << 3);
    public static final int TRIGGER_TYPE_UPDATE     = (1 << 4);
    public static final int TRIGGER_TYPE_TRUNCATE   = (1 << 5);
    public static final int TRIGGER_TYPE_INSTEAD    = (1 << 6);

    private PostgreTableReal table;
    private long objectId;
    private String enabledState;
    private String whenExpression;
    private long functionSchemaId;
    private long functionId;
    private DBSActionTiming actionTiming;
    private DBSManipulationType[] manipulationTypes;
    private PostgreTriggerType type;
    private boolean persisted;
    private PostgreTableColumn[] columnRefs;
    protected String description;
    protected String name;
    private String body;

    public PostgreTrigger(
        DBRProgressMonitor monitor,
        PostgreTableReal table,
        ResultSet dbResult) throws DBException {
        this.persisted = true;
        this.name = JDBCUtils.safeGetString(dbResult, "tgname");
        this.table = table;
        this.objectId = JDBCUtils.safeGetLong(dbResult, "oid");
        this.enabledState = JDBCUtils.safeGetString(dbResult, "tgenabled");
        this.whenExpression = JDBCUtils.safeGetString(dbResult, "tgqual");

        // Get procedure
        this.functionSchemaId = JDBCUtils.safeGetLong(dbResult, "func_schema_id");
        this.functionId = JDBCUtils.safeGetLong(dbResult, "tgfoid");

        // Parse trigger type bits
        int tgType = JDBCUtils.safeGetInt(dbResult, "tgtype");
        if (CommonUtils.isBitSet(tgType, TRIGGER_TYPE_BEFORE)) {
            actionTiming = DBSActionTiming.BEFORE;
        } else if (CommonUtils.isBitSet(tgType, TRIGGER_TYPE_INSTEAD)) {
            actionTiming = DBSActionTiming.INSTEAD;
        } else {
            actionTiming = DBSActionTiming.AFTER;
        }
        List<DBSManipulationType> mt = new ArrayList<>(1);
        if (CommonUtils.isBitSet(tgType, TRIGGER_TYPE_INSERT)) {
            mt.add(DBSManipulationType.INSERT);
        }
        if (CommonUtils.isBitSet(tgType, TRIGGER_TYPE_DELETE)) {
            mt.add(DBSManipulationType.DELETE);
        }
        if (CommonUtils.isBitSet(tgType, TRIGGER_TYPE_UPDATE)) {
            mt.add(DBSManipulationType.UPDATE);
        }
        if (CommonUtils.isBitSet(tgType, TRIGGER_TYPE_TRUNCATE)) {
            mt.add(DBSManipulationType.TRUNCATE);
        }
        this.manipulationTypes = mt.toArray(new DBSManipulationType[0]);

        if (CommonUtils.isBitSet(tgType, TRIGGER_TYPE_ROW)) {
            type = PostgreTriggerType.ROW;
        } else {
            type = PostgreTriggerType.STATEMENT;
        }

        Object attrNumbersObject = JDBCUtils.safeGetObject(dbResult, "tgattr");
        if (attrNumbersObject != null) {
            int[] attrNumbers = PostgreUtils.getIntVector(attrNumbersObject);
            if (attrNumbers != null) {
                int attrCount = attrNumbers.length;
                columnRefs = new PostgreTableColumn[attrCount];
                for (int i = 0; i < attrCount; i++) {
                    int colNumber = attrNumbers[i];
                    final PostgreTableColumn attr = PostgreUtils.getAttributeByNum(getTable().getAttributes(monitor), colNumber);
                    if (attr == null) {
                        log.warn("Bad trigger attribute ref index: " + colNumber);
                        continue;
                    }
                    columnRefs[i] = attr;
                }
            }
        }

        this.description = JDBCUtils.safeGetString(dbResult, "description");
    }

    public PostgreTrigger(DBRProgressMonitor monitor, PostgreTableReal parent) {
        this.table = parent;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Property(viewable = true, order = 2)
    public DBSActionTiming getActionTiming() {
        return actionTiming;
    }

    @Property(viewable = true, order = 3)
    public DBSManipulationType[] getManipulationTypes() {
        return manipulationTypes;
    }

    @Property(viewable = true, order = 4)
    public PostgreTriggerType getType() {
        return type;
    }

    @Property(viewable = true, order = 5, valueRenderer = ColumnNameTransformer.class)
    public PostgreTableColumn[] getColumnRefs() {
        return columnRefs;
    }

    @Property(viewable = true, order = 6)
    public String getEnabledState() {
        return enabledState;
    }

    @Override
    public boolean isPersisted() {
        return persisted;
    }

    @Override
    public PostgreTableBase getTable()
    {
        return table;
    }

    @Override
    @Property(viewable = true, order = 10)
    public long getObjectId() {
        return objectId;
    }

    @Property(viewable = true, order = 11)
    public String getWhenExpression()
    {
        return whenExpression;
    }

    @Property(viewable = true, order = 12)
    public PostgreProcedure getFunction(DBRProgressMonitor monitor) throws DBException {
        if (functionId == 0) {
            return null;
        }
        return getDatabase().getProcedure(monitor, functionSchemaId, functionId);
    }

    public void setFunction(PostgreProcedure function) {
        if (function == null){
            this.functionId = 0;
            this.functionSchemaId = 0;
        } else{
            this.functionId = function.getObjectId();
            this.functionSchemaId = function.getSchema().getObjectId();
        }
    }

    @Property(viewable = true, editable = true, updatable = true, length = PropertyLength.MULTILINE, order = 100)
    @Nullable
    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public PostgreTableReal getParentObject()
    {
        return table;
    }

    @NotNull
    @Override
    public PostgreDataSource getDataSource()
    {
        return table.getDataSource();
    }

    @NotNull
    @Override
    public PostgreDatabase getDatabase() {
        return table.getDatabase();
    }

    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        if (body != null) {
            return body;
        }

        if (persisted) {
            try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Read trigger definition")) {
                body = JDBCUtils.queryString(session, "SELECT pg_catalog.pg_get_triggerdef(?)", objectId);
                if (body != null) {
                    body = SQLFormatUtils.formatSQL(getDataSource(), body);
                }
            } catch (SQLException e) {
                throw new DBException(e, getDataSource());
            }
        } else {
            body = "CREATE TRIGGER " + DBUtils.getQuotedIdentifier(this)
                       + "\n    AFTER INSERT"
                       + "\n    ON " + table.getFullyQualifiedName(DBPEvaluationContext.DDL)
                       + "\n    FOR EACH ROW"
                       + "\n    EXECUTE PROCEDURE " + getFunction(monitor).getFullyQualifiedName(DBPEvaluationContext.DDL) + "();\n";
        }

        return body;
    }

    @Override
    public void setObjectDefinitionText(String sourceText) {
        this.body = sourceText;
    }

    public String getBody() {
        return body;
    }

    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context) {
        return DBUtils.getFullQualifiedName(getDataSource(),
                getParentObject(),
                this);
    }

    @Override
    public DBSObjectState getObjectState() {
        if ("D".equals(enabledState)) {
            return DBSObjectState.INVALID;
        }
        return DBSObjectState.NORMAL;
    }

    @Override
    public void refreshObjectState(DBRProgressMonitor monitor) throws DBCException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Refresh triggers state")) {
            try {
                enabledState = JDBCUtils.queryString(session, "SELECT tgenabled FROM pg_catalog.pg_trigger WHERE oid=?", getObjectId());
            } catch (SQLException e) {
                throw new DBCException(e, session.getExecutionContext());
            }
        }

    }

    @Override
    public boolean supportsObjectDefinitionOption(String option) {
        return DBPScriptObject.OPTION_INCLUDE_COMMENTS.equals(option);
    }

    @Override
    public String toString() {
        return getFullyQualifiedName(DBPEvaluationContext.UI);
    }

    public static class ColumnNameTransformer implements IPropertyValueTransformer {
        @Override
        public Object transform(Object object, Object value) throws IllegalArgumentException {
            if (value instanceof PostgreTableColumn[]) {
                StringBuilder sb = new StringBuilder();
                for (PostgreTableColumn col : (PostgreTableColumn[])value) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(col.getName());
                }
                return sb.toString();
            }
            return value;
        }
    }

}
