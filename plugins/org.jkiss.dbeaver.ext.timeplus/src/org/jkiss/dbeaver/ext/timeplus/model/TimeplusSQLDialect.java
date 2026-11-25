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
package org.jkiss.dbeaver.ext.timeplus.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.generic.model.GenericSQLDialect;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataTypeProvider;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.JDBCType;
import java.util.Locale;
import java.util.Map;

public class TimeplusSQLDialect extends GenericSQLDialect {

    // JDBC type metadata lacks the standard aliases and useful numeric precision. Generic
    // matching would choose int8 for INTEGER/BIGINT simply because it is the first integer type.
    private static final Map<String, String> NUMERIC_TYPE_MAPPING = Map.of(
        JDBCType.TINYINT.getName(), "int8",
        JDBCType.SMALLINT.getName(), "int16",
        SQLConstants.DATA_TYPE_INT, "int32",
        JDBCType.INTEGER.getName(), "int32",
        JDBCType.BIGINT.getName(), "int64",
        JDBCType.REAL.getName(), "float64",
        JDBCType.FLOAT.getName(), "float64",
        JDBCType.DOUBLE.getName(), "float64"
    );

    @Nullable
    @Override
    public String convertExternalDataType(
        @NotNull SQLDialect sourceDialect,
        @NotNull DBSTypedObject sourceTypedObject,
        @Nullable DBPDataTypeProvider targetTypeProvider
    ) {
        if (sourceTypedObject.getDataKind() == DBPDataKind.NUMERIC) {
            String typeName = sourceTypedObject.getTypeName();
            if (typeName != null) {
                String mappedType = NUMERIC_TYPE_MAPPING.get(typeName.toUpperCase(Locale.ENGLISH));
                if (mappedType != null) {
                    // CSV uses REAL for values parsed as Double, so preserve 64-bit precision.
                    return mappedType;
                }
            }
        }
        return super.convertExternalDataType(sourceDialect, sourceTypedObject, targetTypeProvider);
    }
}
