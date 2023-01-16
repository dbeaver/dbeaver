/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.clickhouse.model.data;

import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDFormatSettings;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.data.DBDValueHandlerProvider;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCUUIDValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.util.Locale;

public class ClickhouseValueHandlerProvider implements DBDValueHandlerProvider {
    @Override
    public DBDValueHandler getValueHandler(DBPDataSource dataSource, DBDFormatSettings preferences, DBSTypedObject type) {
        String lowerTypeName = type.getTypeName().toLowerCase(Locale.ENGLISH);
        if ("enum8".equals(lowerTypeName) || "enum16".equals(lowerTypeName)) {
            return ClickhouseEnumValueHandler.INSTANCE;
        } else if (type.getDataKind() == DBPDataKind.ARRAY) {
            return ClickhouseArrayValueHandler.INSTANCE;
        } else if (type.getDataKind() == DBPDataKind.STRUCT) {
            return ClickhouseStructValueHandler.INSTANCE;
        } else if ("int128".equals(lowerTypeName) || "int256".equals(lowerTypeName)
            || "uint64".equals(lowerTypeName) || "uint128".equals(lowerTypeName) || "uint256".equals(lowerTypeName)) {
            return new ClickhouseBigNumberValueHandler(type, preferences);
        } else if ("bool".equals(lowerTypeName)) {
            return ClickhouseBoolValueHandler.INSTANCE;
        } else if ("uuid".equals(lowerTypeName)) {
            return JDBCUUIDValueHandler.INSTANCE;
        } else {
            return null;
        }
    }
}
