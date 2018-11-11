/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.ext.mssql.SQLServerUtils;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractProcedure;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.sql.ResultSet;
import java.util.Collection;
import java.util.Map;

/**
 * SQLServerProcedure
 */
public class SQLServerProcedure extends AbstractProcedure<SQLServerDataSource, SQLServerSchema> implements DBPRefreshableObject, DBPScriptObject, SQLServerObject
{
    private static final Log log = Log.getLog(SQLServerProcedure.class);

    private DBSProcedureType procedureType;
    private String body;
    private long objectId;
    private SQLServerObjectType objectType;

    public SQLServerProcedure(SQLServerSchema schema)
    {
        super(schema, false);
        this.procedureType = DBSProcedureType.PROCEDURE;
    }

    public SQLServerProcedure(
        SQLServerSchema catalog,
        ResultSet dbResult)
    {
        super(catalog, true);
        loadInfo(dbResult);
    }

    private void loadInfo(ResultSet dbResult)
    {
        this.objectId = JDBCUtils.safeGetLong(dbResult, "object_id");
        this.name = JDBCUtils.safeGetString(dbResult, "name");
        this.objectType = SQLServerObjectType.P;
        try {
            objectType = SQLServerObjectType.valueOf(JDBCUtils.safeGetStringTrimmed(dbResult, "type"));
        } catch (IllegalArgumentException e) {
            log.debug("Bad procedure type", e);
        }
        switch (objectType) {
            case P:
            case PC:
            case X:
                this.procedureType = DBSProcedureType.PROCEDURE;
                break;
            default:
                this.procedureType = DBSProcedureType.FUNCTION;
                break;
        }
        this.description = JDBCUtils.safeGetString(dbResult, "description");
    }

    @Override
    @Property(viewable = false, order = 2)
    public long getObjectId() {
        return objectId;
    }

    @Property(viewable = false, order = 3)
    public SQLServerObjectType getObjectType() {
        return objectType;
    }

    @Override
    @Property(order = 5)
    public DBSProcedureType getProcedureType()
    {
        return procedureType ;
    }

    public void setProcedureType(DBSProcedureType procedureType)
    {
        this.procedureType = procedureType;
    }

/*
    @Property(order = 2)
    public String getResultType()
    {
        return resultType;
    }
*/

    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getDeclaration(DBRProgressMonitor monitor)
        throws DBException
    {
        if (body == null) {
            if (!persisted) {
                this.body =
                    "CREATE " + getProcedureType().name() + " " + getFullyQualifiedName(DBPEvaluationContext.DDL) + "()" + GeneralUtils.getDefaultLineSeparator() +
                        (procedureType == DBSProcedureType.FUNCTION ? "RETURNS INT" + GeneralUtils.getDefaultLineSeparator() : "") +
                    "BEGIN" + GeneralUtils.getDefaultLineSeparator() +
                    "END";
            } else {
                this.body = SQLServerUtils.extractSource(monitor, getContainer().getDatabase(), getContainer(), getName());
            }
/*
            StringBuilder cb = new StringBuilder(getBody().length() + 100);
            cb.append("CREATE ").append(procedureType).append(' ').append(getFullyQualifiedName()).append(" (");

            int colIndex = 0;
            for (SQLServerProcedureParameter column : CommonUtils.safeCollection(getParameters(monitor))) {
                if (column.getParameterKind() == DBSProcedureParameterKind.RETURN) {
                    continue;
                }
                if (colIndex > 0) {
                    cb.append(", ");
                }
                if (getProcedureType() == DBSProcedureType.PROCEDURE) {
                    cb.append(column.getParameterKind()).append(' ');
                }
                cb.append(column.getName()).append(' ');
                appendParameterType(cb, column);
                colIndex++;
            }
            cb.append(")").append(GeneralUtils.getDefaultLineSeparator());
            for (SQLServerProcedureParameter column : CommonUtils.safeCollection(getParameters(monitor))) {
                if (column.getParameterKind() == DBSProcedureParameterKind.RETURN) {
                    cb.append("RETURNS ");
                    appendParameterType(cb, column);
                    cb.append(GeneralUtils.getDefaultLineSeparator());
                }
            }
            if (deterministic) {
                cb.append("DETERMINISTIC").append(GeneralUtils.getDefaultLineSeparator());
            }
            cb.append(getBody());
            clientBody = cb.toString();
*/
        }
        return body;
    }

    private String normalizeCreateStatement(String createDDL) {
        String procType = getProcedureType().name();
        int divPos = createDDL.indexOf(procType + " `");
        if (divPos != -1) {
            return createDDL.substring(0, divPos) + procType +
                " `" + getContainer().getName() + "`." +
                createDDL.substring(divPos + procType.length() + 1);
        }
        return createDDL;
    }

    @Override
    public Collection<SQLServerProcedureParameter> getParameters(DBRProgressMonitor monitor)
        throws DBException
    {
        return getContainer().getProcedureCache().getChildren(monitor, getContainer(), this);
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context)
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getContainer(),
            this);
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException
    {
        return getDeclaration(monitor);
    }

    //@Override
    public void setObjectDefinitionText(String sourceText) throws DBException
    {
        this.body = sourceText;
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {

        return this;//getContainer().proceduresCache.refreshObject(monitor, getContainer(), this);
    }

    @Override
    public String toString() {
        return procedureType.name() + " " + getName();
    }

}
