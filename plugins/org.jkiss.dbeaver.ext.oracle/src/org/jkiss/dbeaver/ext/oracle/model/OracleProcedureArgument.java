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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameter;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameterKind;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * OracleProcedureArgument
 */
public class OracleProcedureArgument implements DBSProcedureParameter, DBSTypedObject
{
    private final OracleProcedureBase procedure;
    private String name;
    private int position;
    private int dataLevel;
    private int sequence;
    private OracleParameterMode mode;
    private OracleDataType type;
    private OracleDataType dataType;
    private String packageTypeName;
    private int dataLength;
    private int dataScale;
    private int dataPrecision;
    private List<OracleProcedureArgument> attributes;

    public OracleProcedureArgument(
        DBRProgressMonitor monitor,
        OracleProcedureBase procedure,
        ResultSet dbResult)
    {
        this.procedure = procedure;
        this.name = JDBCUtils.safeGetString(dbResult, "ARGUMENT_NAME");
        this.position = JDBCUtils.safeGetInt(dbResult, "POSITION");
        this.dataLevel = JDBCUtils.safeGetInt(dbResult, "DATA_LEVEL");
        this.sequence = JDBCUtils.safeGetInt(dbResult, "SEQUENCE");
        this.mode = OracleParameterMode.getMode(JDBCUtils.safeGetString(dbResult, "IN_OUT"));
        final String dataType = JDBCUtils.safeGetString(dbResult, "DATA_TYPE");
        this.type = CommonUtils.isEmpty(dataType) ? null : OracleDataType.resolveDataType(
            monitor,
            procedure.getDataSource(),
            null,
            dataType);
        final String typeName = JDBCUtils.safeGetString(dbResult, "TYPE_NAME");
        final String typeOwner = JDBCUtils.safeGetString(dbResult, "TYPE_OWNER");
        this.packageTypeName = JDBCUtils.safeGetString(dbResult, "TYPE_SUBNAME");
        if (!CommonUtils.isEmpty(typeName) && !CommonUtils.isEmpty(typeOwner) && CommonUtils.isEmpty(this.packageTypeName)) {
            this.dataType = OracleDataType.resolveDataType(
                monitor,
                procedure.getDataSource(),
                typeOwner,
                typeName);
        } else if (this.packageTypeName != null) {
            packageTypeName = typeName + "." + packageTypeName;
        }
        this.dataLength = JDBCUtils.safeGetInt(dbResult, "DATA_LENGTH");
        this.dataScale = JDBCUtils.safeGetInt(dbResult, "DATA_SCALE");
        this.dataPrecision = JDBCUtils.safeGetInt(dbResult, "DATA_PRECISION");
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return null;
    }

    @NotNull
    @Override
    public OracleDataSource getDataSource()
    {
        return procedure.getDataSource();
    }

    @Override
    public OracleProcedureBase getParentObject()
    {
        return procedure;
    }

    @Override
    public boolean isPersisted()
    {
        return true;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 10)
    public String getName()
    {
        if (CommonUtils.isEmpty(name)) {
            if (dataLevel == 0) {
                // Function result
                return "RESULT";
            } else {
                // Collection element
                return "ELEMENT";
            }
        }
        return name;
    }

    @Property(viewable = true, order = 11)
    public int getPosition()
    {
        return position;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 20)
    public DBSProcedureParameterKind getParameterKind()
    {
        return mode == null ? DBSProcedureParameterKind.UNKNOWN : mode.getParameterKind();
    }

    @Property(viewable = true, order = 21)
    public Object getType()
    {
        return packageTypeName != null ?
            packageTypeName :
            dataType == null ? type : dataType;
    }

    @Override
    @Property(viewable = true, order = 30)
    public long getMaxLength()
    {
        return dataLength;
    }

    @Override
    public String getTypeName()
    {
        return type.getName();
    }

    @Override
    public int getTypeID()
    {
        return type.getTypeID();
    }

    @Override
    public DBPDataKind getDataKind()
    {
        return type.getDataKind();
    }

    @Override
    @Property(viewable = true, order = 40)
    public int getScale()
    {
        return dataScale;
    }

    @Override
    @Property(viewable = true, order = 50)
    public int getPrecision()
    {
        return dataPrecision;
    }

    public int getDataLevel()
    {
        return dataLevel;
    }

    public int getSequence()
    {
        return sequence;
    }

    @Association
    public Collection<OracleProcedureArgument> getAttributes()
    {
        return attributes;
    }

    void addAttribute(OracleProcedureArgument attribute)
    {
        if (attributes == null) {
            attributes = new ArrayList<>();
        }
        attributes.add(attribute);
    }

    public boolean hasAttributes()
    {
        return !CommonUtils.isEmpty(attributes);
    }

    @NotNull
    @Override
    public DBSTypedObject getParameterType() {
        return this;
    }
}
