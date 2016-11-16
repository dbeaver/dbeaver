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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractTrigger;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSActionTiming;
import org.jkiss.dbeaver.model.struct.rdb.DBSManipulationType;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * PostgreTrigger
 */
public class PostgreTrigger extends AbstractTrigger implements PostgreObject, PostgreScriptObject
{

    /* Bits within tgtype */
    public static final int TRIGGER_TYPE_ROW        = (1 << 0);
    public static final int TRIGGER_TYPE_BEFORE     = (1 << 1);
    public static final int TRIGGER_TYPE_INSERT     = (1 << 2);
    public static final int TRIGGER_TYPE_DELETE     = (1 << 3);
    public static final int TRIGGER_TYPE_UPDATE     = (1 << 4);
    public static final int TRIGGER_TYPE_TRUNCATE   = (1 << 5);
    public static final int TRIGGER_TYPE_INSTEAD    = (1 << 6);

    private PostgreTableBase table;
    private long objectId;
    private String whenExpression;
    private long functionSchemaId;
    private long functionId;
    private String body;

    public PostgreTrigger(
        PostgreTableBase table,
        ResultSet dbResult)
    {
        super(JDBCUtils.safeGetString(dbResult, "tgname"), null, true);
        this.table = table;
        this.objectId = JDBCUtils.safeGetLong(dbResult, "oid");
        this.whenExpression = JDBCUtils.safeGetString(dbResult, "tgqual");

        // Get procedure
        this.functionSchemaId = JDBCUtils.safeGetLong(dbResult, "func_schema_id");
        this.functionId = JDBCUtils.safeGetLong(dbResult, "tgfoid");

        // Parse trigger type bits
        int tgType = JDBCUtils.safeGetInt(dbResult, "tgtype");
        if (CommonUtils.isBitSet(tgType, TRIGGER_TYPE_BEFORE)) {
            setActionTiming(DBSActionTiming.BEFORE);
        } else if (CommonUtils.isBitSet(tgType, TRIGGER_TYPE_INSTEAD)) {
            setActionTiming(DBSActionTiming.INSTEAD);
        } else {
            setActionTiming(DBSActionTiming.AFTER);
        }
        if (CommonUtils.isBitSet(tgType, TRIGGER_TYPE_INSERT)) {
            setManipulationType(DBSManipulationType.INSERT);
        } else if (CommonUtils.isBitSet(tgType, TRIGGER_TYPE_DELETE)) {
            setManipulationType(DBSManipulationType.DELETE);
        } else if (CommonUtils.isBitSet(tgType, TRIGGER_TYPE_UPDATE)) {
            setManipulationType(DBSManipulationType.UPDATE);
        } else if (CommonUtils.isBitSet(tgType, TRIGGER_TYPE_TRUNCATE)) {
            setManipulationType(DBSManipulationType.TRUNCATE);
        }
    }

    @Override
    @Property(viewable = true, order = 4)
    public PostgreTableBase getTable()
    {
        return table;
    }

    @Override
    @Property(viewable = true, order = 5)
    public long getObjectId() {
        return objectId;
    }

    @Property(viewable = true, order = 6)
    public String getWhenExpression()
    {
        return whenExpression;
    }

    @Property(viewable = true, order = 7)
    public PostgreProcedure getFunction(DBRProgressMonitor monitor) throws DBException {
        if (functionId == 0) {
            return null;
        }
        return getDatabase().getProcedure(monitor, functionSchemaId, functionId);
    }

    @Override
    public PostgreTableBase getParentObject()
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

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(DBRProgressMonitor monitor) throws DBException
    {
        if (body == null) {
            try (JDBCSession session = DBUtils.openMetaSession(monitor, getDataSource(), "Read trigger definition")) {
                body = JDBCUtils.queryString(session, "SELECT pg_catalog.pg_get_triggerdef(?)", objectId);
            } catch (SQLException e) {
                throw new DBException(e, getDataSource());
            }
            body = SQLUtils.formatSQL(getDataSource(), body);
        }
        return body;
    }

    @Override
    public void setObjectDefinitionText(String sourceText) throws DBException
    {
        body = sourceText;
    }

    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context) {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getParentObject(),
            this);
    }

}
