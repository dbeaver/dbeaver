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
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.model.source.OracleSourceObject;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.struct.rdb.DBSTrigger;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.util.Collection;
import java.util.List;

/**
 * GenericProcedure
 */
public class OracleTrigger extends OracleSchemaObject implements DBSTrigger, OracleSourceObject
{
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

        ActionType(String title)
        {
            this.title = title;
        }

        @NotNull
        @Override
        public String getName()
        {
            return title;
        }
    }

    private OracleTableBase table;
    private BaseObjectType objectType;
    private String triggerType;
    private String triggeringEvent;
    private String columnName;
    private String refNames;
    private String whenClause;
    private OracleObjectStatus status;
    private String description;
    private ActionType actionType;
    private List<OracleTriggerColumn> columns;
    private String sourceDeclaration;

    public OracleTrigger(OracleSchema schema, OracleTableBase table, String name)
    {
        super(schema, name, false);
        this.table = table;
    }

    public OracleTrigger(
        OracleSchema schema,
        OracleTableBase table,
        ResultSet dbResult)
    {
        super(schema, JDBCUtils.safeGetString(dbResult, "TRIGGER_NAME"), true);
        this.objectType = CommonUtils.valueOf(BaseObjectType.class, JDBCUtils.safeGetStringTrimmed(dbResult, "BASE_OBJECT_TYPE"));
        this.triggerType = JDBCUtils.safeGetString(dbResult, "TRIGGER_TYPE");
        this.triggeringEvent = JDBCUtils.safeGetString(dbResult, "TRIGGERING_EVENT");
        this.columnName = JDBCUtils.safeGetString(dbResult, "COLUMN_NAME");
        this.refNames = JDBCUtils.safeGetString(dbResult, "REFERENCING_NAMES");
        this.whenClause = JDBCUtils.safeGetString(dbResult, "WHEN_CLAUSE");
        this.status = CommonUtils.valueOf(OracleObjectStatus.class, JDBCUtils.safeGetStringTrimmed(dbResult, "STATUS"));
        this.description = JDBCUtils.safeGetString(dbResult, "DESCRIPTION");
        this.actionType = "CALL".equals(JDBCUtils.safeGetString(dbResult, "ACTION_TYPE")) ? ActionType.CALL : ActionType.PLSQL;
        this.table = table;
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = true, order = 1)
    public String getName()
    {
        return super.getName();
    }

    @Override
    @Property(viewable = true, order = 4)
    public OracleTableBase getTable()
    {
        return table;
    }

    @Property(viewable = true, order = 5)
    public BaseObjectType getObjectType()
    {
        return objectType;
    }

    @Property(viewable = true, order = 5)
    public String getTriggerType()
    {
        return triggerType;
    }

    @Property(viewable = true, order = 6)
    public String getTriggeringEvent()
    {
        return triggeringEvent;
    }

    @Property(viewable = true, order = 7)
    public String getColumnName()
    {
        return columnName;
    }

    @Property(order = 8)
    public String getRefNames()
    {
        return refNames;
    }

    @Property(order = 9)
    public String getWhenClause()
    {
        return whenClause;
    }

    @Property(viewable = true, order = 10)
    public OracleObjectStatus getStatus()
    {
        return status;
    }

    @Nullable
    @Override
    @Property(order = 11)
    public String getDescription()
    {
        return description;
    }

    @Property(viewable = true, order = 12)
    public ActionType getActionType()
    {
        return actionType;
    }

    @Association
    public Collection<OracleTriggerColumn> getColumns(DBRProgressMonitor monitor) throws DBException
    {
        if (columns == null) {
            getSchema().triggerCache.loadChildren(monitor, getSchema(), this);
        }
        return columns;
    }

    boolean isColumnsCached()
    {
        return columns != null;
    }

    void setColumns(List<OracleTriggerColumn> columns)
    {
        this.columns = columns;
    }

    @Override
    public OracleSourceType getSourceType()
    {
        return OracleSourceType.TRIGGER;
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(DBRProgressMonitor monitor) throws DBException
    {
        if (sourceDeclaration == null && monitor != null) {
            sourceDeclaration = OracleUtils.getSource(monitor, this, false, false);
        }
        return sourceDeclaration;
    }

    public void setObjectDefinitionText(String source)
    {
        this.sourceDeclaration = source;
    }

    @NotNull
    @Override
    public DBSObjectState getObjectState()
    {
        return status != OracleObjectStatus.ERROR ? DBSObjectState.NORMAL : DBSObjectState.INVALID;
    }

    @Override
    public void refreshObjectState(@NotNull DBRProgressMonitor monitor) throws DBCException
    {
        this.status = (OracleUtils.getObjectStatus(monitor, this, OracleObjectType.TRIGGER) ? OracleObjectStatus.ENABLED : OracleObjectStatus.ERROR);
    }

    @Override
    public DBEPersistAction[] getCompileActions()
    {
        return new DBEPersistAction[] {
            new OracleObjectPersistAction(
                OracleObjectType.TRIGGER,
                "Compile trigger",
                "ALTER TRIGGER " + getFullQualifiedName() + " COMPILE"
            )};
    }

}
