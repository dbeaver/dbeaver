/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.tibero.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractProcedure;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameter;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameterKind;
import org.jkiss.utils.CommonUtils;

public class TiberoProcedureParameter implements DBSProcedureParameter, DBSTypedObject {

    private final AbstractProcedure<TiberoDataSource, ?> procedure;
    private final String name;
    private final int position;
    private final int dataLevel;
    private final DBSProcedureParameterKind parameterKind;
    private final String typeName;
    private final int dataLength;
    private final int dataPrecision;
    private final int dataScale;

    public TiberoProcedureParameter(@NotNull AbstractProcedure<TiberoDataSource, ?> procedure, @NotNull JDBCResultSet dbResult) {
        this.procedure = procedure;
        this.name = JDBCUtils.safeGetString(dbResult, "ARGUMENT_NAME");
        this.position = JDBCUtils.safeGetInt(dbResult, "POSITION");
        this.dataLevel = JDBCUtils.safeGetInt(dbResult, "DATA_LEVEL");
        this.typeName = JDBCUtils.safeGetString(dbResult, "DATA_TYPE");
        this.dataLength = JDBCUtils.safeGetInt(dbResult, "DATA_LENGTH");
        this.dataPrecision = JDBCUtils.safeGetInt(dbResult, "DATA_PRECISION");
        this.dataScale = JDBCUtils.safeGetInt(dbResult, "DATA_SCALE");
        this.parameterKind = resolveParameterKind(JDBCUtils.safeGetString(dbResult, "IN_OUT"));
    }

    @NotNull
    private static DBSProcedureParameterKind resolveParameterKind(@Nullable String inOut) {
        if (CommonUtils.isEmpty(inOut)) {
            return DBSProcedureParameterKind.RETURN;
        }
        return switch (inOut.trim().toUpperCase()) {
            case "IN" -> DBSProcedureParameterKind.IN;
            case "OUT" -> DBSProcedureParameterKind.OUT;
            case "IN/OUT", "INOUT" -> DBSProcedureParameterKind.INOUT;
            default -> DBSProcedureParameterKind.UNKNOWN;
        };
    }

    @NotNull
    @Override
    public TiberoDataSource getDataSource() {
        return procedure.getDataSource();
    }

    @Override
    public AbstractProcedure<TiberoDataSource, ?> getParentObject() {
        return procedure;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 10)
    public String getName() {
        if (CommonUtils.isEmpty(name)) {
            return dataLevel == 0 ? "RESULT" : "ELEMENT";
        }
        return name;
    }

    @Property(viewable = true, order = 11)
    public int getPosition() {
        return position;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 20)
    public DBSProcedureParameterKind getParameterKind() {
        return parameterKind;
    }

    @Property(viewable = true, order = 21)
    public String getType() {
        return typeName;
    }

    @Override
    @Property(viewable = true, order = 30)
    public long getMaxLength() {
        return dataLength;
    }

    @Override
    public long getTypeModifiers() {
        return 0;
    }

    @NotNull
    @Override
    public String getTypeName() {
        return CommonUtils.notEmpty(typeName);
    }

    @NotNull
    @Override
    public String getFullTypeName() {
        return DBUtils.getFullTypeName(this);
    }

    @Override
    public int getTypeID() {
        return 0;
    }

    @NotNull
    @Override
    public DBPDataKind getDataKind() {
        return DBPDataKind.UNKNOWN;
    }

    @Nullable
    @Override
    @Property(viewable = true, order = 40)
    public Integer getScale() {
        return dataScale;
    }

    @Override
    @Property(viewable = true, order = 50)
    public Integer getPrecision() {
        return dataPrecision;
    }

    public int getDataLevel() {
        return dataLevel;
    }

    @NotNull
    @Override
    public DBSTypedObject getParameterType() {
        return this;
    }
}
