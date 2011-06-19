/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTrigger;

import java.sql.ResultSet;
import java.util.Collection;
import java.util.List;

/**
 * GenericProcedure
 */
public class OracleTrigger extends OracleSchemaObject implements DBSTrigger, OracleSourceObject
{
    static final Log log = LogFactory.getLog(OracleTrigger.class);

    public static enum BaseObjectType {
        TABLE,
        VIEW,
        SCHEMA,
        DATABASE
    }

    public static enum ActionType implements DBPNamedObject {
        PLSQL("PL/SQL"),
        CALL("CALL");

        private final String title;

        ActionType(String title)
        {
            this.title = title;
        }

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

    public OracleTrigger(
        OracleSchema schema,
        OracleTableBase table,
        ResultSet dbResult)
    {
        super(schema, JDBCUtils.safeGetString(dbResult, "TRIGGER_NAME"), true);
        this.objectType = BaseObjectType.valueOf(JDBCUtils.safeGetStringTrimmed(dbResult, "BASE_OBJECT_TYPE"));
        this.triggerType = JDBCUtils.safeGetString(dbResult, "TRIGGER_TYPE");
        this.triggeringEvent = JDBCUtils.safeGetString(dbResult, "TRIGGERING_EVENT");
        this.columnName = JDBCUtils.safeGetString(dbResult, "COLUMN_NAME");
        this.refNames = JDBCUtils.safeGetString(dbResult, "REFERENCING_NAMES");
        this.whenClause = JDBCUtils.safeGetString(dbResult, "WHEN_CLAUSE");
        this.status = OracleObjectStatus.getByName(JDBCUtils.safeGetStringTrimmed(dbResult, "STATUS"));
        this.description = JDBCUtils.safeGetString(dbResult, "DESCRIPTION");
        this.actionType = "CALL".equals(JDBCUtils.safeGetString(dbResult, "ACTION_TYPE")) ? ActionType.CALL : ActionType.PLSQL;
        this.table = table;
    }

    @Property(name = "Name", viewable = true, editable = true, order = 1)
    public String getName()
    {
        return super.getName();
    }

    @Property(name = "Table", viewable = true, order = 4)
    public OracleTableBase getTable()
    {
        return table;
    }

    @Property(name = "Object Type", viewable = true, order = 5)
    public BaseObjectType getObjectType()
    {
        return objectType;
    }

    @Property(name = "Trigger Type", viewable = true, order = 5)
    public String getTriggerType()
    {
        return triggerType;
    }

    @Property(name = "Event", viewable = true, order = 6)
    public String getTriggeringEvent()
    {
        return triggeringEvent;
    }

    @Property(name = "Column", viewable = true, order = 7)
    public String getColumnName()
    {
        return columnName;
    }

    @Property(name = "Ref Names", order = 8)
    public String getRefNames()
    {
        return refNames;
    }

    @Property(name = "When Clause", order = 9)
    public String getWhenClause()
    {
        return whenClause;
    }

    @Property(name = "Status", viewable = true, order = 10)
    public OracleObjectStatus getStatus()
    {
        return status;
    }

    @Property(name = "Description", order = 11)
    public String getDescription()
    {
        return description;
    }

    @Property(name = "Action Type", viewable = true, order = 12)
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

    public OracleSchema getSourceOwner()
    {
        return getSchema();
    }

    public OracleSourceType getSourceType()
    {
        return OracleSourceType.TRIGGER;
    }

}