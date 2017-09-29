/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2017 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.db2.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.DB2Utils;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2RoutineRowType;
import org.jkiss.dbeaver.ext.db2.model.module.DB2Module;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.model.struct.DBSTypedObjectEx;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameter;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameterKind;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;

/**
 * DB2 Routine Parameter
 * 
 * @author Denis Forveille
 */
public class DB2RoutineParm implements DBSProcedureParameter, DBSTypedObject, DBSTypedObjectEx {

    private final DB2Routine  procedure;
    private String            name;
    private String            remarks;
    private Integer           scale;
    private Integer           length;
    private DB2RoutineRowType rowType;

    private DB2DataType       dataType;
    private DB2Schema         dataTypeSchema;

    private String            typeName;

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
        this.dataType = db2DataSource.getLocalDataType(typeName);
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
    public Integer getPrecision()
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
    public String getFullTypeName()
    {
        return DBUtils.getFullTypeName(this);
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

    @NotNull
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

    @Nullable
    @Override
    @Property(viewable = true, order = 3)
    public DB2DataType getDataType()
    {
        return dataType;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 4)
    public DBSProcedureParameterKind getParameterKind()
    {
        return rowType.getParameterKind();
    }

    @Override
    @Property(viewable = true, order = 5)
    public long getMaxLength()
    {
        return length;
    }

    @Override
    @Property(viewable = true, order = 6)
    public Integer getScale()
    {
        return scale;
    }

    public DB2RoutineRowType getRowType()
    {
        return rowType;
    }

    @NotNull
    @Override
    public DBSTypedObject getParameterType()
    {
        return this;
    }
}
