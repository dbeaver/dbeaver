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
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractProcedure;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.sql.ResultSet;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;

/**
 * SQLServerProcedure
 */
public class SQLServerProcedure extends AbstractProcedure<SQLServerDataSource, SQLServerSchema> implements DBPRefreshableObject, DBPScriptObject
{
    private static final Log log = Log.getLog(SQLServerProcedure.class);

    private DBSProcedureType procedureType;
    private String body;

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
        setName(JDBCUtils.safeGetString(dbResult, "name"));
        //setDescription(JDBCUtils.safeGetString(dbResult, SQLServerConstants.COL_ROUTINE_COMMENT));
        String procType = JDBCUtils.safeGetString(dbResult, "type");
        try {
            this.procedureType = procType == null ? DBSProcedureType.PROCEDURE : DBSProcedureType.valueOf(procType.toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException e) {
            log.debug("Unsupported procedure type: " + procType);
            this.procedureType = DBSProcedureType.PROCEDURE;
        }
    }

    @Override
    @Property(order = 2)
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
                this.body = SQLServerUtils.extractSource(monitor, getContainer(), getName());
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
        return null;//getContainer().proceduresCache.getChildren(monitor, getContainer(), this);
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
