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

import org.jkiss.dbeaver.Log;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSEntityElement;

import java.sql.ResultSet;

/**
 * Oracle data type member
 */
public abstract class OracleDataTypeMember implements DBSEntityElement
{

    static final Log log = Log.getLog(OracleDataTypeMember.class);

    private OracleDataType dataType;
    protected String name;
    protected int number;
    private boolean inherited;
    private boolean persisted;

    protected OracleDataTypeMember(OracleDataType dataType)
    {
        this.dataType = dataType;
        this.persisted = false;
    }

    protected OracleDataTypeMember(OracleDataType dataType, ResultSet dbResult)
    {
        this.dataType = dataType;
        this.inherited = JDBCUtils.safeGetBoolean(dbResult, "INHERITED", OracleConstants.YES);
        this.persisted = true;
    }

    @NotNull
    public OracleDataType getDataType()
    {
        return dataType;
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return null;
    }

    @NotNull
    @Override
    public OracleDataType getParentObject()
    {
        return dataType;
    }

    @NotNull
    @Override
    public OracleDataSource getDataSource()
    {
        return dataType.getDataSource();
    }

    @Override
    public boolean isPersisted()
    {
        return persisted;
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
    public String getName()
    {
        return name;
    }

    @Property(viewable = true, order = 2)
    public int getNumber()
    {
        return number;
    }

    @Property(viewable = true, order = 20)
    public boolean isInherited()
    {
        return inherited;
    }
}
