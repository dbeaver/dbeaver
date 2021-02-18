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
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPUniqueObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;

import java.sql.ResultSet;

/**
 * GenericProcedure
 */
public class OracleProcedurePackaged extends OracleProcedureBase<OraclePackage> implements DBPUniqueObject
{
    private Integer overload;

    public OracleProcedurePackaged(
        OraclePackage ownerPackage,
        ResultSet dbResult)
    {
        super(ownerPackage,
            JDBCUtils.safeGetString(dbResult, "PROCEDURE_NAME"),
            0l,
            DBSProcedureType.valueOf(JDBCUtils.safeGetString(dbResult, "PROCEDURE_TYPE")));
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context)
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getSchema(),
            getParentObject(),
            this);
    }

    @Override
    public OracleSchema getSchema()
    {
        return getParentObject().getSchema();
    }

    @Override
    public Integer getOverloadNumber()
    {
        return overload;
    }

    public void setOverload(int overload)
    {
        this.overload = overload;
    }

    @NotNull
    @Override
    public String getUniqueName()
    {
        return overload == null || overload <= 1 ? getName() : getName() + "#" + overload;
    }

}
