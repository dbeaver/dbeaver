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
package org.jkiss.dbeaver.ext.trino.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericDataType;
import org.jkiss.dbeaver.ext.generic.model.GenericDataTypeCache;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.sql.Types;
import java.util.Locale;

public class TrinoDataTypeCache extends GenericDataTypeCache {

    TrinoDataTypeCache(GenericStructContainer owner) {
        super(owner);
    }

    @Override
    protected GenericDataType fetchObject(
        @NotNull JDBCSession session,
        @NotNull GenericStructContainer container,
        @NotNull JDBCResultSet dbResult
    ) throws SQLException, DBException {
        String name = JDBCUtils.safeGetString(dbResult, JDBCConstants.TYPE_NAME);
        if (CommonUtils.isEmpty(name)) {
            return null;
        }
        int valueType = JDBCUtils.safeGetInt(dbResult, JDBCConstants.DATA_TYPE);
        // Check for incorrect values types
        if (valueType == Types.JAVA_OBJECT) {
            String lowerCaseName = name.toLowerCase(Locale.getDefault());
            switch (lowerCaseName) {
                case SQLConstants.DATA_TYPE_VARCHAR:
                    valueType = Types.VARCHAR;
                    break;
                case "char", "uuid":
                    valueType = Types.CHAR;
                    break;
                case "time with time zone":
                    valueType = Types.TIME_WITH_TIMEZONE;
                    break;
                case "timestamp with time zone":
                    valueType = Types.TIMESTAMP_WITH_TIMEZONE;
                    break;
                case "timestamp":
                    valueType = Types.TIMESTAMP;
                    break;
                case "time":
                    valueType = Types.TIME;
                    break;
                case "decimal":
                    valueType = Types.DECIMAL;
                    break;
                default:
                    return super.fetchObject(session, container, dbResult);
            }
            return makeDataType(dbResult, name, valueType);
        }
        return super.fetchObject(session, container, dbResult);
    }
}
