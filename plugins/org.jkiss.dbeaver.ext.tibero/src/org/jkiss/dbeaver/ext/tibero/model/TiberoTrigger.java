/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.tibero.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.tibero.model.source.TiberoSourceObject;
import org.jkiss.dbeaver.ext.tibero.model.source.TiberoSourcePersistAction;
import org.jkiss.dbeaver.ext.tibero.model.source.TiberoSourceType;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBPOverloadedObject;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.struct.rdb.DBSTrigger;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.Map;

public abstract class TiberoTrigger<PARENT extends DBSObject> implements DBSTrigger, DBPQualifiedObject, DBPOverloadedObject, TiberoSourceObject {

    public enum BaseObjectType {
        TABLE,
        VIEW,
        SCHEMA,
        DATABASE
    }

    public enum ActionType implements DBPNamedObject {
        PLSQL("PL/SQL"),
        CALL("CALL");

        private final String title;

        ActionType(String title) {
            this.title = title;
        }

        @NotNull
        @Override
        public String getName() {
            return title;
        }
    }

    private final PARENT parent;
    private final String name;
    private final boolean persisted;
    private final BaseObjectType objectType;
    private final String triggerType;
    private final String triggeringEvent;
    private final String columnName;
    private final String refNames;
    private final String whenClause;
    private final String description;
    private final ActionType actionType;
    private String status;
    private String sourceDeclaration;

    protected TiberoTrigger(@NotNull PARENT parent, @NotNull String name) {
        this.parent = parent;
        this.name = name;
        this.persisted = false;
        this.objectType = null;
        this.triggerType = null;
        this.triggeringEvent = null;
        this.columnName = null;
        this.refNames = null;
        this.whenClause = null;
        this.status = null;
        this.description = null;
        this.actionType = ActionType.PLSQL;
    }

    protected TiberoTrigger(@NotNull PARENT parent, @NotNull JDBCResultSet dbResult) {
        this.parent = parent;
        this.name = JDBCUtils.safeGetString(dbResult, "TRIGGER_NAME");
        this.persisted = true;
        this.objectType = CommonUtils.valueOf(BaseObjectType.class, JDBCUtils.safeGetStringTrimmed(dbResult, "BASE_OBJECT_TYPE"));
        this.triggerType = JDBCUtils.safeGetString(dbResult, "TRIGGER_TYPE");
        this.triggeringEvent = JDBCUtils.safeGetString(dbResult, "TRIGGERING_EVENT");
        this.columnName = JDBCUtils.safeGetString(dbResult, "COLUMN_NAME");
        this.refNames = JDBCUtils.safeGetString(dbResult, "REFERENCING_NAMES");
        this.whenClause = JDBCUtils.safeGetString(dbResult, "WHEN_CLAUSE");
        this.status = JDBCUtils.safeGetStringTrimmed(dbResult, "STATUS");
        this.description = JDBCUtils.safeGetString(dbResult, "DESCRIPTION");
        this.actionType = "CALL".equalsIgnoreCase(JDBCUtils.safeGetStringTrimmed(dbResult, "ACTION_TYPE"))
            ? ActionType.CALL
            : ActionType.PLSQL;
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = true, order = 1)
    public String getName() {
        return name;
    }

    @NotNull
    @Override
    public String getOverloadedName() {
        return getName();
    }

    @Nullable
    @Override
    public DBSObject getParentObject() {
        return parent;
    }

    @NotNull
    @Override
    public TiberoDataSource getDataSource() {
        return getSchema().getDataSource();
    }

    @Override
    public boolean isPersisted() {
        return persisted;
    }

    @Property(viewable = true, order = 5)
    public BaseObjectType getObjectType() {
        return objectType;
    }

    @Property(viewable = true, order = 6)
    public String getTriggerType() {
        return triggerType;
    }

    @Property(viewable = true, order = 7)
    public String getTriggeringEvent() {
        return triggeringEvent;
    }

    @Property(viewable = true, order = 8)
    public String getColumnName() {
        return columnName;
    }

    @Property(order = 9)
    public String getRefNames() {
        return refNames;
    }

    @Property(order = 10)
    public String getWhenClause() {
        return whenClause;
    }

    @Property(viewable = true, order = 11)
    public String getStatus() {
        return status;
    }

    @Nullable
    @Override
    @Property(length = PropertyLength.MULTILINE, order = 12)
    public String getDescription() {
        return description;
    }

    @Property(viewable = true, order = 13)
    public ActionType getActionType() {
        return actionType;
    }

    @NotNull
    @Override
    public TiberoSourceType getSourceType() {
        return TiberoSourceType.TRIGGER;
    }

    @NotNull
    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(
        @NotNull DBRProgressMonitor monitor,
        @Nullable Map<String, Object> options
    ) throws DBException {
        if (sourceDeclaration == null || (options != null && Boolean.TRUE.equals(options.get(OPTION_REFRESH)))) {
            sourceDeclaration = TiberoUtils.loadObjectSource(monitor, this, "TRIGGER", getSchema().getName());
        }
        return sourceDeclaration;
    }

    @Override
    public void setObjectDefinitionText(String source) {
        this.sourceDeclaration = source;
    }

    @NotNull
    @Override
    public DBEPersistAction[] getCompileActions(@NotNull DBRProgressMonitor monitor) throws DBCException {
        return new DBEPersistAction[] {
            new TiberoSourcePersistAction(
                TiberoSourceType.TRIGGER.name(),
                "Compile trigger",
                "ALTER TRIGGER " + getFullyQualifiedName(DBPEvaluationContext.DDL) + " COMPILE"
            )
        };
    }

    @NotNull
    @Override
    public DBSObjectState getObjectState() {
        return "ENABLED".equalsIgnoreCase(status) ? DBSObjectState.NORMAL : DBSObjectState.INVALID;
    }

    @Override
    public void refreshObjectState(@NotNull DBRProgressMonitor monitor) throws DBCException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Refresh Tibero trigger state")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT STATUS FROM ALL_TRIGGERS WHERE OWNER = ? AND TRIGGER_NAME = ?"
            )) {
                dbStat.setString(1, getSchema().getName());
                dbStat.setString(2, getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        this.status = JDBCUtils.safeGetStringTrimmed(dbResult, "STATUS");
                    }
                }
            } catch (SQLException e) {
                throw new DBCException(e, session.getExecutionContext());
            }
        }
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(@NotNull DBPEvaluationContext context) {
        return DBUtils.getFullQualifiedName(getDataSource(), getSchema(), this);
    }

    @Override
    public String toString() {
        return getFullyQualifiedName(DBPEvaluationContext.DDL);
    }
}
