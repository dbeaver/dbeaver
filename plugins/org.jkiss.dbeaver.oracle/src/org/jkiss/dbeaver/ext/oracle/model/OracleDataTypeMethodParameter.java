/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.ext.oracle.OracleUtils;
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

    public enum ParameterMode {
        IN,
        OUT,
        INOUT
    }

    private final OracleDataTypeMethod method;
    private String name;
    private int number;
    private ParameterMode mode;
    private OracleDataType type;
    private OracleDataTypeModifier typeMod;

    public OracleDataTypeMethodParameter(DBRProgressMonitor monitor, OracleDataTypeMethod method, ResultSet dbResult)
    {
        this.method = method;
        this.name = JDBCUtils.safeGetString(dbResult, "PARAM_NAME");
        this.number = JDBCUtils.safeGetInt(dbResult, "PARAM_NO");
        String modeName = JDBCUtils.safeGetString(dbResult, "PARAM_MODE");
        if ("IN".equals(modeName)) {
            this.mode = ParameterMode.IN;
        } else if ("OUT".equals(modeName)) {
            this.mode = ParameterMode.OUT;
        } else {
            this.mode = ParameterMode.INOUT;
        }
        this.type = OracleUtils.resolveDataType(
            monitor,
            method.getDataSource(),
            JDBCUtils.safeGetString(dbResult, "PARAM_TYPE_OWNER"),
            JDBCUtils.safeGetString(dbResult, "PARAM_TYPE_NAME"));
        this.typeMod = OracleUtils.resolveTypeModifier(
            JDBCUtils.safeGetString(dbResult, "PARAM_TYPE_MOD"));
    }

    public DBSObject getParentObject()
    {
        return method;
    }

    public DBPDataSource getDataSource()
    {
        return method.getDataSource();
    }

    public boolean isPersisted()
    {
        return true;
    }

    public String getDescription()
    {
        return null;
    }

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
    public ParameterMode getMode()
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
