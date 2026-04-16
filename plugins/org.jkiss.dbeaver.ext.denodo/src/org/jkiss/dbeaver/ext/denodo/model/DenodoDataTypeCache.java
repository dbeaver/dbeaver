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
package org.jkiss.dbeaver.ext.denodo.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCBasicDataTypeCache;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;

import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Objects;

public class DenodoDataTypeCache extends JDBCBasicDataTypeCache<GenericStructContainer, DenodoDataType> {

    @NotNull
    final DenodoDataSource owner;

    public DenodoDataTypeCache(@NotNull GenericStructContainer owner) {
        super(owner);
        this.owner = (DenodoDataSource) owner.getDataSource();
    }

    @Override
    protected void addCustomObjects(
        @NotNull DBRProgressMonitor monitor,
        @NotNull GenericStructContainer genericStructContainer,
        @NotNull List<DenodoDataType> dataTypes
    ) throws DBException {
        dataTypes.add(makeBuiltinDataType(Types.INTEGER, "int", true, 0, 0));
        dataTypes.add(makeBuiltinDataType(Types.BIGINT, "long", true, 0, 0));
        dataTypes.add(makeBuiltinDataType(Types.FLOAT, "float", true, 0, 0));
        dataTypes.add(makeBuiltinDataType(Types.DOUBLE, "double", true,  0, 0));
        dataTypes.add(makeBuiltinDataType(Types.BOOLEAN, "boolean", true, 0, 0));
        dataTypes.add(makeBuiltinDataType(Types.VARCHAR, "text", true, 0, 0));
        dataTypes.add(makeBuiltinDataType(Types.DATE, "date", true, 0, 0));
        dataTypes.add(makeBuiltinDataType(Types.TIMESTAMP, "localdate", true, 0, 0));
        dataTypes.add(makeBuiltinDataType(Types.TIME, "time", true, 0, 0));
        dataTypes.add(makeBuiltinDataType(Types.TIMESTAMP, "timestamp", true, 0, 0));
        dataTypes.add(makeBuiltinDataType(Types.TIMESTAMP_WITH_TIMEZONE, "timestamptz", true, 0, 0));
        dataTypes.add(makeBuiltinDataType(Types.TIME, "intervaldaysecond", true, 0, 0));
        dataTypes.add(makeBuiltinDataType(Types.DATE, "intervalyearmonth", true, 0, 0));
        dataTypes.add(makeBuiltinDataType(Types.BLOB, "blob", false, 0, 0));
        dataTypes.add(makeBuiltinDataType(Types.SQLXML, "xml", false, 0, 0));

    }

    @NotNull
    private DenodoDataType makeBuiltinDataType(int valueType, @NotNull String name, boolean searchable, int precision, int scale) {
        return new DenodoDataType(this.owner, valueType, name, searchable, precision, scale);
    }

    @NotNull
    @Override
    protected DenodoDataType makeDataType(@NotNull JDBCResultSet dbResult, @NotNull String name, int valueType) {
        String denodoTypeKindString = JDBCUtils.safeGetString(dbResult, "vdp_type");
        Objects.requireNonNull(denodoTypeKindString);
        DenodoDataType.Kind kind = DenodoDataType.Kind.fromVdpTypeString(denodoTypeKindString);
        return switch (kind) {
            case ARRAY -> new DenodoArrayDataType(this.owner, valueType, name);
            case REGISTER -> new DenodoRegisterDataType(this.owner, valueType, name);
            default ->
                // according to the docs, should be unreachable
                // https://community.denodo.com/docs/html/browse/8.0/en/vdp/vql/stored_procedures/predefined_stored_procedures/get_user_defined_types
                throw new IllegalStateException("Unexpected data kind " + kind);
        };
    }

    @NotNull
    @Override
    protected JDBCStatement prepareObjectsStatement(
        @NotNull JDBCSession session,
        @NotNull GenericStructContainer owner
    ) throws SQLException {
        String sqlText = """
                            SELECT
                                type_name as TYPE_NAME,
                                sql_type_code as DATA_TYPE,
                                vdp_type
                            FROM GET_USER_DEFINED_TYPES()
                         """;
        // WHERE database_name = ?
        // statement.setString(dbName);
        return session.prepareStatement(sqlText);
    }
}
