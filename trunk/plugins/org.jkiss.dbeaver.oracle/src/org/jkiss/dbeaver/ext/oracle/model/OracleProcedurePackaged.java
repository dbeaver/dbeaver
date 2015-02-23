/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.struct.DBSObjectUnique;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;

import java.sql.ResultSet;

/**
 * GenericProcedure
 */
public class OracleProcedurePackaged extends OracleProcedureBase<OraclePackage> implements DBSObjectUnique
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

    @Override
    public String getUniqueName()
    {
        return overload == null || overload <= 1 ? getName() : getName() + "#" + overload;
    }

}
