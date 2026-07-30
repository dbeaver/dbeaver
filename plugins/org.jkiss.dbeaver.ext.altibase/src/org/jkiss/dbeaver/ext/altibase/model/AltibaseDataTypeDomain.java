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
package org.jkiss.dbeaver.ext.altibase.model;

import org.jkiss.dbeaver.ext.altibase.AltibaseConstants;

import java.sql.JDBCType;
import java.sql.Types;

public enum AltibaseDataTypeDomain {
    // char types
    CHAR(JDBCType.CHAR),
    VARCHAR(JDBCType.VARCHAR),
    NCHAR(JDBCType.NCHAR.getName(), Types.NVARCHAR),
    NVARCHAR(JDBCType.NVARCHAR),
    // encrypted column data type: ECHAR, EVARCHAR should not visible to user.

    // number types
    INTEGER(JDBCType.INTEGER),
    SMALLINT(JDBCType.SMALLINT),
    BIGINT(JDBCType.BIGINT),
    REAL(JDBCType.REAL),
    NUMBER(AltibaseConstants.TYPE_NAME_NUMBER, Types.NUMERIC),
    NUMERIC(JDBCType.NUMERIC),
    DOUBLE(JDBCType.DOUBLE),
    FLOAT(JDBCType.FLOAT),

    // date & time
    DATE(JDBCType.DATE.getName(), Types.TIMESTAMP),

    // binary
    BIT(JDBCType.BIT), // STRING, CHAR -> {0}
    VARBIT(AltibaseConstants.TYPE_NAME_VARBIT, Types.BIT),
    BYTE(AltibaseConstants.TYPE_NAME_BYTE, Types.BINARY),
    VARBYTE(AltibaseConstants.TYPE_NAME_VARBYTE, Types.BINARY),
    NIBBLE(AltibaseConstants.TYPE_NAME_NIBBLE, Types.BINARY),
    BINARY(JDBCType.BINARY),

    CLOB(JDBCType.CLOB),
    BLOB(JDBCType.BLOB),
    GEOMETRY(AltibaseConstants.TYPE_NAME_GEOMETRY, Types.BINARY),
    JSON(AltibaseConstants.TYPE_NAME_JSON, Types.OTHER);

    private final String name;
    private final int jdbcTypeID;

    // JDBCType fully compatible
    AltibaseDataTypeDomain(JDBCType jdbcType) {
        this.name = jdbcType.getName();
        this.jdbcTypeID = jdbcType.getVendorTypeNumber();
    }
    
    // JDBCType doesn't match
    AltibaseDataTypeDomain(String name, int jdbcType) {
        this.name = name;
        this.jdbcTypeID = jdbcType;
    }
    
    public int getValueType() {
        return jdbcTypeID;
    }

    public String getTypeName() {
        return name;
    }

    /**
     * Get AltibaseDataTypeDomain by data type id.
     */
    public static AltibaseDataTypeDomain getByTypeName(String typeName) {

        for (AltibaseDataTypeDomain ft : values()) {
            if (ft.name.equalsIgnoreCase(typeName)) {
                return ft;
            }
        }
        return null;
    }
}