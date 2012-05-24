/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;

/**
 * Oracle data type attribute
 */
public class OracleDataTypeMethodParameter implements DBSObject {

    private final OracleDataTypeMethod method;
    private String name;
    private int number;
    private OracleParameterMode mode;
    private OracleDataType type;
    private OracleDataTypeModifier typeMod;

    public OracleDataTypeMethodParameter(DBRProgressMonitor monitor, OracleDataTypeMethod method, ResultSet dbResult)
    {
        this.method = method;
        this.name = JDBCUtils.safeGetString(dbResult, "PARAM_NAME");
        this.number = JDBCUtils.safeGetInt(dbResult, "PARAM_NO");
        this.mode = OracleParameterMode.getMode(JDBCUtils.safeGetString(dbResult, "PARAM_MODE"));
        this.type = OracleDataType.resolveDataType(
            monitor,
            method.getDataSource(),
            JDBCUtils.safeGetString(dbResult, "PARAM_TYPE_OWNER"),
            JDBCUtils.safeGetString(dbResult, "PARAM_TYPE_NAME"));
        this.typeMod = OracleDataTypeModifier.resolveTypeModifier(
            JDBCUtils.safeGetString(dbResult, "PARAM_TYPE_MOD"));
    }

    @Override
    public DBSObject getParentObject()
    {
        return method;
    }

    @Override
    public DBPDataSource getDataSource()
    {
        return method.getDataSource();
    }

    @Override
    public boolean isPersisted()
    {
        return true;
    }

    @Override
    public String getDescription()
    {
        return null;
    }

    @Override
    @Property(name = "Name", viewable = true, order = 1)
    public String getName()
    {
        return name;
    }

    @Property(name = "Number", viewable = true, order = 2)
    public int getNumber()
    {
        return number;
    }

    @Property(name = "Mode", viewable = true, order = 3)
    public OracleParameterMode getMode()
    {
        return mode;
    }

    @Property(id = "dataType", name = "Type", viewable = true, order = 4)
    public OracleDataType getType()
    {
        return type;
    }

    @Property(id = "dataTypeMod", name = "Type Mod", viewable = true, order = 5)
    public OracleDataTypeModifier getTypeMod()
    {
        return typeMod;
    }
}
