/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.struct.DBSProcedureType;

import java.sql.ResultSet;

/**
 * GenericProcedure
 */
public class OracleProcedureStandalone extends OracleProcedureBase<OracleSchema> implements OracleCompileUnit
{

    private boolean valid;

    public OracleProcedureStandalone(
        OracleSchema schema,
        ResultSet dbResult)
    {
        super(
            schema,
            JDBCUtils.safeGetString(dbResult, "OBJECT_NAME"),
            DBSProcedureType.valueOf(JDBCUtils.safeGetString(dbResult, "OBJECT_TYPE")));
        this.valid = "VALID".equals(JDBCUtils.safeGetString(dbResult, "STATUS"));
    }

    @Property(name = "Valid", viewable = true, order = 3)
    public boolean isValid()
    {
        return valid;
    }

    public OracleSchema getSourceOwner()
    {
        return getParentObject();
    }

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

    public String getFullQualifiedName()
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getSourceOwner(),
            this);
    }

    public IDatabasePersistAction[] getCompileActions()
    {
        return new IDatabasePersistAction[] {
            new AbstractDatabasePersistAction(
                "Compile procedure",
                "ALTER " + getSourceType().name() + " " + getFullQualifiedName() + " COMPILE"
            )};
    }

    public DBSObjectState getObjectState()
    {
        return valid ? DBSObjectState.NORMAL : DBSObjectState.INVALID;
    }
}
