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
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.ext.oracle.model.source.OracleSourceObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.sql.ResultSet;
import java.util.Map;

/**
 * GenericProcedure
 */
public class OracleProcedureStandalone extends OracleProcedureBase<OracleSchema> implements OracleSourceObject, DBPRefreshableObject
{

    private boolean valid;
    private String sourceDeclaration;

    public OracleProcedureStandalone(
        OracleSchema schema,
        ResultSet dbResult)
    {
        super(
            schema,
            JDBCUtils.safeGetString(dbResult, "OBJECT_NAME"),
            JDBCUtils.safeGetLong(dbResult, "OBJECT_ID"),
            DBSProcedureType.valueOf(JDBCUtils.safeGetString(dbResult, "OBJECT_TYPE")));
        this.valid = "VALID".equals(JDBCUtils.safeGetString(dbResult, "STATUS"));
    }

    public OracleProcedureStandalone(OracleSchema oracleSchema, String name, DBSProcedureType procedureType)
    {
        super(oracleSchema, name, 0l, procedureType);
        sourceDeclaration =
            procedureType.name() + " " + name + GeneralUtils.getDefaultLineSeparator() +
            "IS" + GeneralUtils.getDefaultLineSeparator() +
            "BEGIN" + GeneralUtils.getDefaultLineSeparator() +
            "END " + name + ";" + GeneralUtils.getDefaultLineSeparator();
    }

    @Property(viewable = true, order = 3)
    public boolean isValid()
    {
        return valid;
    }

    @Override
    public OracleSchema getSchema()
    {
        return getParentObject();
    }

    @Override
    public OracleSourceType getSourceType()
    {
        return getProcedureType() == DBSProcedureType.PROCEDURE ?
            OracleSourceType.PROCEDURE :
            OracleSourceType.FUNCTION;
    }

    @Override
    public Integer getOverloadNumber()
    {
        return null;
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context)
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getSchema(),
            this);
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBCException
    {
        if (sourceDeclaration == null && monitor != null) {
            sourceDeclaration = OracleUtils.getSource(monitor, this, false, true);
        }
        return sourceDeclaration;
    }

    public void setObjectDefinitionText(String sourceDeclaration)
    {
        this.sourceDeclaration = sourceDeclaration;
    }

    @Override
    public DBEPersistAction[] getCompileActions()
    {
        return new DBEPersistAction[] {
            new OracleObjectPersistAction(
                getProcedureType() == DBSProcedureType.PROCEDURE ?
                    OracleObjectType.PROCEDURE : OracleObjectType.FUNCTION,
                "Compile procedure",
                "ALTER " + getSourceType().name() + " " + getFullyQualifiedName(DBPEvaluationContext.DDL) + " COMPILE"
            )};
    }

    @NotNull
    @Override
    public DBSObjectState getObjectState()
    {
        return valid ? DBSObjectState.NORMAL : DBSObjectState.INVALID;
    }

    @Override
    public void refreshObjectState(@NotNull DBRProgressMonitor monitor) throws DBCException
    {
        this.valid = OracleUtils.getObjectStatus(monitor, this,
            getProcedureType() == DBSProcedureType.PROCEDURE ?
                    OracleObjectType.PROCEDURE : OracleObjectType.FUNCTION);
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        return getSchema().proceduresCache.refreshObject(monitor, getSchema(), this);
    }
}
