/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.firebird.model;

import org.jkiss.dbeaver.ext.generic.model.GenericProcedure;
import org.jkiss.dbeaver.ext.generic.model.GenericProcedureParameter;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameterKind;

public class FireBirdProcedureParameter extends GenericProcedureParameter {

    private String defaultValue;

    public FireBirdProcedureParameter(
            GenericProcedure procedure,
            String columnName,
            String typeName,
            int valueType,
            int ordinalPosition,
            int columnSize,
            Integer scale,
            Integer precision,
            boolean notNull,
            String remarks,
            DBSProcedureParameterKind parameterKind,
            String defaultValue)
    {
        super(procedure, columnName, typeName, valueType, ordinalPosition, columnSize, scale, precision, notNull, remarks, parameterKind);
        this.defaultValue = defaultValue;
    }

    public String getDefaultValue() {
        return defaultValue;
    }
}
