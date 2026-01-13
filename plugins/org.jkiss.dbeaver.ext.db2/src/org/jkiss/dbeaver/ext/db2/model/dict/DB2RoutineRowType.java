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
package org.jkiss.dbeaver.ext.db2.model.dict;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameterKind;

/**
 * DB2 Routine Row Type
 *
 * @author Denis Forveille
 */
public enum DB2RoutineRowType implements DBPNamedObject {
    B("Both input and output parameter", DBSProcedureParameterKind.INOUT),

    C("Result after casting", DBSProcedureParameterKind.RETURN),

    O("Output parameter", DBSProcedureParameterKind.OUT),

    P("Input parameter", DBSProcedureParameterKind.IN),

    R("Result before casting", DBSProcedureParameterKind.RETURN),

    S("Aggregation state variable", DBSProcedureParameterKind.UNKNOWN);

    private final String title;
    private final DBSProcedureParameterKind parameterKind;

    DB2RoutineRowType(String title, DBSProcedureParameterKind parameterKind) {
        this.title = title;
        this.parameterKind = parameterKind;
    }

    @NotNull
    @Override
    public String getName() {
        return title;
    }

    public DBSProcedureParameterKind getParameterKind() {
        return parameterKind;
    }

}