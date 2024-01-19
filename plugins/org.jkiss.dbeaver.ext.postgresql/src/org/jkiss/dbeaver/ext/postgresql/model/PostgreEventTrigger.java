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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.Map;

public class PostgreEventTrigger extends PostgreTriggerBase {

    public enum TriggerEventTypes {
        ddl_command_start,
        ddl_command_end,
        table_rewrite,
        sql_drop
    }

    private long objectId;
    private long routineId;
    private TriggerEventTypes eventType;
    private String enabledState;
    private String description;
    private String body;

    PostgreEventTrigger(@NotNull PostgreDatabase database, @NotNull String name, @NotNull JDBCResultSet dbResult) {
        super(database, name, true);
        this.objectId = JDBCUtils.safeGetLong(dbResult, "oid");
        this.routineId = JDBCUtils.safeGetLong(dbResult, "evtfoid");
        String eventTypeName = JDBCUtils.safeGetString(dbResult, "evtevent");
        this.eventType = CommonUtils.valueOf(TriggerEventTypes.class, eventTypeName);
        this.enabledState = JDBCUtils.safeGetString(dbResult, "evtenabled");
        this.description = JDBCUtils.safeGetString(dbResult, "description");
    }

    public PostgreEventTrigger(@NotNull PostgreDatabase database, String name) {
        super(database, name, false);
    }

    @Override
    @Property(viewable = true, order = 2)
    public long getObjectId() {
        return objectId;
    }

    @Property(viewable = true, order = 3)
    public TriggerEventTypes getEventType() {
        return eventType;
    }

    public void setEventType(TriggerEventTypes eventType) {
        this.eventType = eventType;
    }

    @Override
    @Property(viewable = true, order = 4)
    public PostgreProcedure getFunction(DBRProgressMonitor monitor) throws DBException {
        if (routineId == 0) {
            return null;
        }
        return getDatabase().getProcedure(monitor, routineId);
    }

    public void setFunction(PostgreProcedure function) {
        this.routineId = function != null ? function.getObjectId() : 0;
    }

    @Override
    @Property(viewable = true, order = 5)
    public String getEnabledState() {
        return enabledState;
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context) {
        return DBUtils.getQuotedIdentifier(this);
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        StringBuilder ddl = new StringBuilder();
        PostgreProcedure function = getFunction(monitor);
        if (function == null) {
            return "-- Event trigger definition is not available - can't read trigger function";
        }
        if (CommonUtils.isEmpty(body)) {
            body = "CREATE EVENT TRIGGER " + getFullyQualifiedName(DBPEvaluationContext.DDL) + " ON " +
                eventType + "\n\tEXECUTE " + function.getProcedureTypeName() + " " + function.getFullQualifiedSignature();
        }
        ddl.append(body); // Body is the main part of the trigger DDL. It doesn't include comments

        if (!CommonUtils.isEmpty(getDescription()) && CommonUtils.getOption(options, DBPScriptObject.OPTION_INCLUDE_COMMENTS)) {
            ddl.append(";\n\nCOMMENT ON EVENT TRIGGER ").append(DBUtils.getQuotedIdentifier(this))
                .append(" IS ")
                .append(SQLUtils.quoteString(this, getDescription())).append(";");
        }

        return ddl.toString();
    }

    @Override
    public void setObjectDefinitionText(String sourceText) {
        this.body = sourceText;
    }

    @Override
    public String getBody() {
        return body;
    }

    @Nullable
    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        return getDatabase().getEventTriggersCache().refreshObject(monitor, getDatabase(), this);
    }

    @NotNull
    @Override
    public DBSObjectState getObjectState() {
        if ("D".equals(enabledState)) {
            return DBSObjectState.INVALID;
        }
        return DBSObjectState.NORMAL;
    }

    @Override
    public void refreshObjectState(@NotNull DBRProgressMonitor monitor) throws DBCException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Refresh triggers state")) {
            try {
                enabledState = JDBCUtils.queryString(session, "SELECT evtenabled FROM pg_catalog.pg_event_trigger WHERE oid=?", getObjectId());
            } catch (SQLException e) {
                throw new DBCException(e, session.getExecutionContext());
            }
        }
    }

    @NotNull
    @Override
    public DBSObject getParentObject() {
        return getDatabase();
    }

    @Override
    public DBSTable getTable() {
        // Event triggers belong to databases
        return null;
    }

    @Nullable
    @Override
    @Property(viewable = true, editable = true, updatable = true, length = PropertyLength.MULTILINE, order = 100)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
