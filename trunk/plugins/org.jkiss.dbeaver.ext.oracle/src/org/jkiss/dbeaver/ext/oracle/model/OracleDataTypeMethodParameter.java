/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSParameter;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.ResultSet;

/**
 * Oracle data type attribute
 */
public class OracleDataTypeMethodParameter implements DBSParameter {

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

    @NotNull
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

    @Nullable
    @Override
    public String getDescription()
    {
        return null;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName()
    {
        return name;
    }

    @Property(viewable = true, order = 2)
    public int getNumber()
    {
        return number;
    }

    @Property(viewable = true, order = 3)
    public OracleParameterMode getMode()
    {
        return mode;
    }

    @Property(id = "dataType", viewable = true, order = 4)
    public OracleDataType getType()
    {
        return type;
    }

    @Property(id = "dataTypeMod", viewable = true, order = 5)
    public OracleDataTypeModifier getTypeMod()
    {
        return typeMod;
    }

    @NotNull
    @Override
    public DBSTypedObject getParameterType() {
        return type;
    }
}
