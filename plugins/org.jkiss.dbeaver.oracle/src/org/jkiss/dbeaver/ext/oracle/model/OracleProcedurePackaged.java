/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.struct.DBSEntityContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSProcedureType;

import java.sql.ResultSet;

/**
 * GenericProcedure
 */
public class OracleProcedurePackaged extends OracleProcedureBase
{
    private OraclePackage ownerPackage;
    public OracleProcedurePackaged(
        OraclePackage ownerPackage,
        ResultSet dbResult)
    {
        super(ownerPackage.getSchema(), DBSProcedureType.valueOf(JDBCUtils.safeGetString(dbResult, "PROCEDURE_TYPE")), dbResult);
        this.ownerPackage = ownerPackage;
    }

    public OraclePackage getOwnerPackage()
    {
        return ownerPackage;
    }

    public DBSEntityContainer getContainer()
    {
        return getOwnerPackage();
    }

    public DBSObject getParentObject()
    {
        return getOwnerPackage();
    }

    public String getFullQualifiedName()
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            ownerPackage.getSchema(),
            ownerPackage,
            this);
    }

    public OracleSourceType getSourceType()
    {
        return OracleSourceType.PACKAGE;
    }
}
