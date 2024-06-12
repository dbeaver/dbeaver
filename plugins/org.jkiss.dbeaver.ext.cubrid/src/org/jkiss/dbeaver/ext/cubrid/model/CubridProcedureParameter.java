/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.cubrid.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameter;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameterKind;

public class CubridProcedureParameter implements DBSProcedureParameter
{
    private String procName;
    private String argName;
    private String dataType;
    private String mode;
    private String comment;
    private CubridProcedure procedure;

    public CubridProcedureParameter(
            @NotNull CubridProcedure procedure,
            @NotNull String procName,
            @NotNull String argName,
            @NotNull String dataType,
            @NotNull String mode,
            @Nullable String comment) {
        this.procedure = procedure;
        this.procName = procName;
        this.argName = argName;
        this.dataType = dataType;
        this.mode = mode;
        this.comment = comment;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return procName;
    }

    @NotNull
    @Property(viewable = true, order = 2)
    public String getArgName() {
        return argName;
    }

    @NotNull
    @Property(viewable = true, order = 3)
    public String getDataType() {
        return dataType;
    }

    @Nullable
    @Override
    @Property(viewable = true, order = 5)
    public String getDescription() {
        return comment;
    }

    @Nullable
    @Override
    public DBSTypedObject getParameterType() {
        return (DBSTypedObject) this;
    }

    @NotNull
    @Override
    public CubridProcedure getParentObject() {
        return procedure;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 4)
    public DBSProcedureParameterKind getParameterKind() {
        switch (mode) {
            case "IN" :
                return DBSProcedureParameterKind.IN;
            case "INOUT":
                return DBSProcedureParameterKind.INOUT;
            case "OUT" :
                return DBSProcedureParameterKind.OUT;
            default:
                return DBSProcedureParameterKind.UNKNOWN;
        }
    }

    @Nullable
    @Override
    public DBPDataSource getDataSource() {
        return procedure.getDataSource();
    }

    @NotNull
    @Override
    public boolean isPersisted() {
        return true;
    }
}
