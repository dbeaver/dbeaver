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
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.DBPUniqueObject;
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
    public String getFullQualifiedName()
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
