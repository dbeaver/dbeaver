/*
 * Copyright (C) 2013      Denis Forveille titou10.titou10@gmail.com
 * Copyright (C) 2010-2015 Serge Rieder serge@jkiss.org
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
package org.jkiss.dbeaver.ext.db2.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.DB2Utils;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2RoutineRowType;
import org.jkiss.dbeaver.ext.db2.model.module.DB2Module;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameter;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameterType;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;

/**
 * DB2 Routine Parameter
 * 
 * @author Denis Forveille
 */
public class DB2RoutineParm implements DBSProcedureParameter {

    private final DB2Routine procedure;
    private String name;
    private String remarks;
    private Integer scale;
    private Integer length;
    private DB2RoutineRowType rowType;

    private DB2DataType dataType;
    private DB2Schema dataTypeSchema;
    private DB2Schema dataTypeModule;

    private String typeName;

    // -----------------------
    // Constructors
    // -----------------------

    public DB2RoutineParm(DBRProgressMonitor monitor, DB2Routine procedure, ResultSet dbResult) throws DBException
    {
        super();

        this.procedure = procedure;

        DB2DataSource db2DataSource = getDataSource();

        this.name = JDBCUtils.safeGetStringTrimmed(dbResult, "PARMNAME");
        this.scale = JDBCUtils.safeGetInteger(dbResult, "SCALE");
        this.length = JDBCUtils.safeGetInteger(dbResult, "LENGTH");
        this.remarks = JDBCUtils.safeGetStringTrimmed(dbResult, "REMARKS");
        this.rowType = CommonUtils.valueOf(DB2RoutineRowType.class, JDBCUtils.safeGetString(dbResult, "ROWTYPE"));

        String typeSchemaName = JDBCUtils.safeGetStringTrimmed(dbResult, "TYPESCHEMA");
        String typeModuleName = JDBCUtils.safeGetStringTrimmed(dbResult, "TYPEMODULENAME");
        this.typeName = JDBCUtils.safeGetStringTrimmed(dbResult, "TYPENAME");
        this.dataTypeSchema = db2DataSource.getSchema(monitor, typeSchemaName);

        // -------------------
        // Search for DataType
        // -------------------

        // First Search in System/Standard Data Types
        this.dataType = db2DataSource.getDataType(typeName);
        if (this.dataType != null) {
            return;
        }

        // Not found : Search for a UDT in Module
        if (typeModuleName != null) {
            DB2Module db2Module = DB2Utils.findModuleBySchemaNameAndName(monitor, db2DataSource, typeSchemaName, typeModuleName);
            this.dataType = db2Module.getType(monitor, typeName);
            return;
        }

        // Not found, search for a UDT
        this.dataType = this.dataTypeSchema.getUDT(monitor, typeName);
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return remarks;
    }

    @NotNull
    @Override
    public DB2DataSource getDataSource()
    {
        return procedure.getDataSource();
    }

    @Override
    public DB2Routine getParentObject()
    {
        return procedure;
    }

    @Override
    public boolean isPersisted()
    {
        return true;
    }

    @Override
    public int getPrecision()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    // DF: Strange typeName and typeId are attributes of DBPDataKind...
    @Override
    public String getTypeName()
    {
        return typeName;
    }

    @Override
    public int getTypeID()
    {
        return dataType.getEquivalentSqlType();
    }

    @Override
    public DBPDataKind getDataKind()
    {
        return dataType.getDataKind();
    }

    // -----------------------
    // Properties
    // -----------------------

    @Override
    @Property(viewable = true, order = 1)
    public String getName()
    {
        return name;
    }

    @Property(viewable = true, order = 2)
    public DB2Schema getDataTypeSchema()
    {
        return dataTypeSchema;
    }

    @Property(viewable = true, order = 3)
    public DB2DataType getDataType()
    {
        return dataType;
    }

    @Override
    @Property(viewable = true, order = 4)
    public DBSProcedureParameterType getParameterType()
    {
        return rowType.getParameterType();
    }

    @Override
    @Property(viewable = true, order = 5)
    public long getMaxLength()
    {
        return length;
    }

    @Override
    @Property(viewable = true, order = 6)
    public int getScale()
    {
        return scale;
    }

    public DB2RoutineRowType getRowType()
    {
        return rowType;
    }

}
