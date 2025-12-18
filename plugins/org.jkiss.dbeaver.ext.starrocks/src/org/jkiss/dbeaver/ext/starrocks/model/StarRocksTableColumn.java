/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.starrocks.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.starrocks.StarRocksDataSource;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableColumn;
import org.jkiss.dbeaver.model.meta.Property;

import java.sql.ResultSet;

/**
 * StarRocks Table Column
 */
public class StarRocksTableColumn extends JDBCTableColumn<StarRocksTable> {

    private String defaultValue;
    private String extraInfo;
    private String collation;
    private String comment;

    public StarRocksTableColumn(StarRocksTable table, ResultSet dbResult) throws DBException {
        super(table, true);

        // Parse SHOW FULL COLUMNS result
        // Field | Type | Collation | Null | Key | Default | Extra | Privileges | Comment
        setName(JDBCUtils.safeGetString(dbResult, "Field"));
        setTypeName(JDBCUtils.safeGetString(dbResult, "Type"));
        setOrdinalPosition(0); // Will be set based on order

        String nullableStr = JDBCUtils.safeGetString(dbResult, "Null");
        setRequired(!"YES".equalsIgnoreCase(nullableStr));

        String keyStr = JDBCUtils.safeGetString(dbResult, "Key");
        // PRI = primary key, UNI = unique, MUL = index

        this.defaultValue = JDBCUtils.safeGetString(dbResult, "Default");
        this.extraInfo = JDBCUtils.safeGetString(dbResult, "Extra");
        this.collation = JDBCUtils.safeGetString(dbResult, "Collation");
        this.comment = JDBCUtils.safeGetString(dbResult, "Comment");

        // Parse type to get precision and scale
        parseTypeInfo(getTypeName());
    }

    private void parseTypeInfo(String typeName) {
        if (typeName == null) {
            return;
        }

        // Extract precision/length from types like VARCHAR(255), DECIMAL(10,2), etc.
        String upperType = typeName.toUpperCase();
        int parenPos = upperType.indexOf('(');
        if (parenPos > 0) {
            String baseType = upperType.substring(0, parenPos);
            String params = upperType.substring(parenPos + 1, upperType.length() - 1);

            if (params.contains(",")) {
                // Numeric type with precision and scale
                String[] parts = params.split(",");
                try {
                    setPrecision(Integer.parseInt(parts[0].trim()));
                    setScale(Integer.parseInt(parts[1].trim()));
                } catch (NumberFormatException e) {
                    // Ignore
                }
            } else {
                // Type with length only
                try {
                    setMaxLength(Long.parseLong(params.trim()));
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
        }

        // Set value type based on base type
        setValueType(getValueTypeFromTypeName(typeName));
    }

    private int getValueTypeFromTypeName(String typeName) {
        if (typeName == null) {
            return java.sql.Types.OTHER;
        }
        String upperType = typeName.toUpperCase();
        int parenPos = upperType.indexOf('(');
        if (parenPos > 0) {
            upperType = upperType.substring(0, parenPos);
        }

        switch (upperType) {
            case "TINYINT":
                return java.sql.Types.TINYINT;
            case "SMALLINT":
                return java.sql.Types.SMALLINT;
            case "INT":
            case "INTEGER":
                return java.sql.Types.INTEGER;
            case "BIGINT":
                return java.sql.Types.BIGINT;
            case "LARGEINT":
                return java.sql.Types.BIGINT;
            case "FLOAT":
                return java.sql.Types.FLOAT;
            case "DOUBLE":
                return java.sql.Types.DOUBLE;
            case "DECIMAL":
            case "NUMERIC":
                return java.sql.Types.DECIMAL;
            case "CHAR":
                return java.sql.Types.CHAR;
            case "VARCHAR":
            case "STRING":
                return java.sql.Types.VARCHAR;
            case "DATE":
                return java.sql.Types.DATE;
            case "DATETIME":
            case "TIMESTAMP":
                return java.sql.Types.TIMESTAMP;
            case "BOOLEAN":
                return java.sql.Types.BOOLEAN;
            case "BINARY":
            case "VARBINARY":
                return java.sql.Types.VARBINARY;
            case "JSON":
                return java.sql.Types.OTHER;
            case "ARRAY":
                return java.sql.Types.ARRAY;
            case "MAP":
            case "STRUCT":
                return java.sql.Types.STRUCT;
            case "BITMAP":
            case "HLL":
                return java.sql.Types.OTHER;
            default:
                return java.sql.Types.OTHER;
        }
    }

    @NotNull
    @Override
    public StarRocksDataSource getDataSource() {
        return getTable().getDataSource();
    }

    @Property(viewable = true, order = 50)
    public String getDefaultValue() {
        return defaultValue;
    }

    @Property(viewable = true, order = 51)
    public String getExtraInfo() {
        return extraInfo;
    }

    @Property(viewable = false, order = 52)
    public String getCollation() {
        return collation;
    }

    @Nullable
    @Override
    @Property(viewable = true, order = 100)
    public String getDescription() {
        return comment;
    }

    @NotNull
    @Override
    public DBPDataKind getDataKind() {
        return getDataKindByTypeName(getTypeName());
    }

    private DBPDataKind getDataKindByTypeName(String typeName) {
        if (typeName == null) {
            return DBPDataKind.UNKNOWN;
        }
        String upperType = typeName.toUpperCase();
        int parenPos = upperType.indexOf('(');
        if (parenPos > 0) {
            upperType = upperType.substring(0, parenPos);
        }

        switch (upperType) {
            case "TINYINT":
            case "SMALLINT":
            case "INT":
            case "INTEGER":
            case "BIGINT":
            case "LARGEINT":
            case "FLOAT":
            case "DOUBLE":
            case "DECIMAL":
            case "NUMERIC":
                return DBPDataKind.NUMERIC;
            case "CHAR":
            case "VARCHAR":
            case "STRING":
            case "TEXT":
                return DBPDataKind.STRING;
            case "DATE":
            case "DATETIME":
            case "TIMESTAMP":
                return DBPDataKind.DATETIME;
            case "BOOLEAN":
                return DBPDataKind.BOOLEAN;
            case "BINARY":
            case "VARBINARY":
                return DBPDataKind.BINARY;
            case "JSON":
                return DBPDataKind.CONTENT;
            case "ARRAY":
                return DBPDataKind.ARRAY;
            case "MAP":
            case "STRUCT":
                return DBPDataKind.STRUCT;
            default:
                return DBPDataKind.UNKNOWN;
        }
    }
}
