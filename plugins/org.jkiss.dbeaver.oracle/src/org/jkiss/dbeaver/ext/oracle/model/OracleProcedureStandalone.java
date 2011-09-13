/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.struct.DBSProcedureType;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.sql.ResultSet;
import java.util.regex.Matcher;

/**
 * GenericProcedure
 */
public class OracleProcedureStandalone extends OracleProcedureBase<OracleSchema> implements OracleCompileUnit
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
            procedureType.name() + " " + name + ContentUtils.getDefaultLineSeparator() +
            "IS" + ContentUtils.getDefaultLineSeparator() +
            "BEGIN" + ContentUtils.getDefaultLineSeparator() +
            "END " + name + ";" + ContentUtils.getDefaultLineSeparator();
    }

    @Property(name = "Valid", viewable = true, order = 3)
    public boolean isValid()
    {
        return valid;
    }

    public OracleSchema getSchema()
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
            getSchema(),
            this);
    }

    public String getSQLDeclaration()
    {
        if (sourceDeclaration == null) {
            return null;
        }
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(getProcedureType().name() + "\\s+([\\w\\.]+)[\\s\\(]+", java.util.regex.Pattern.CASE_INSENSITIVE);
        final Matcher matcher = pattern.matcher(sourceDeclaration);
        if (matcher.find()) {
            String procedureName = matcher.group(1);
            if (procedureName.indexOf('.') == -1) {
                return sourceDeclaration.substring(0, matcher.start(1)) + getSchema().getName() + "." + procedureName + sourceDeclaration.substring(matcher.end(1));
            }
        }
        return sourceDeclaration;
    }

    @Property(name = "Declaration", hidden = true, editable = true, updatable = true, order = -1)
    public String getSourceDeclaration(DBRProgressMonitor monitor) throws DBCException
    {
        if (sourceDeclaration == null) {
            sourceDeclaration = OracleUtils.getSource(monitor, this, false);
        }
        return sourceDeclaration;
    }

    public void setSourceDeclaration(String sourceDeclaration)
    {
        this.sourceDeclaration = sourceDeclaration;
    }

    public IDatabasePersistAction[] getCompileActions()
    {
        return new IDatabasePersistAction[] {
            new OracleObjectPersistAction(
                getProcedureType() == DBSProcedureType.PROCEDURE ?
                    OracleObjectType.PROCEDURE : OracleObjectType.FUNCTION,
                "Compile procedure",
                "ALTER " + getSourceType().name() + " " + getFullQualifiedName() + " COMPILE"
            )};
    }

    @Override
    public boolean refreshEntity(DBRProgressMonitor monitor) throws DBException
    {
        this.sourceDeclaration = null;
        return super.refreshEntity(monitor);
    }

    public DBSObjectState getObjectState()
    {
        return valid ? DBSObjectState.NORMAL : DBSObjectState.INVALID;
    }

    public void refreshObjectState(DBRProgressMonitor monitor) throws DBCException
    {
        this.valid = OracleUtils.getObjectStatus(monitor, this,
            getProcedureType() == DBSProcedureType.PROCEDURE ?
                    OracleObjectType.PROCEDURE : OracleObjectType.FUNCTION);
    }

}
