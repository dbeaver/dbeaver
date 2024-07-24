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
package org.jkiss.dbeaver.ext.clickhouse.model.data;

import org.jkiss.dbeaver.ext.clickhouse.ClickhouseConstants;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDFormatSettings;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.data.DBDValueHandlerProvider;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCNumberValueHandler;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCUUIDValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.util.Locale;

public class ClickhouseValueHandlerProvider implements DBDValueHandlerProvider {
    @Override
    public DBDValueHandler getValueHandler(DBPDataSource dataSource, DBDFormatSettings preferences, DBSTypedObject type) {
        String lowerTypeName = type.getTypeName().toLowerCase(Locale.ENGLISH);
        DBPDataKind dataKind = type.getDataKind();
        if ("enum8".equals(lowerTypeName) || "enum16".equals(lowerTypeName)) {
            return ClickhouseEnumValueHandler.INSTANCE;
        } else if (dataKind == DBPDataKind.ARRAY) {
            return ClickhouseArrayValueHandler.INSTANCE;
        } else if (dataKind == DBPDataKind.STRUCT) {
            return ClickhouseStructValueHandler.INSTANCE;
        } else if ("bool".equals(lowerTypeName)) {
            return ClickhouseBoolValueHandler.INSTANCE;
        } else if ("uuid".equals(lowerTypeName)) {
            return JDBCUUIDValueHandler.INSTANCE;
        } else if (dataKind == DBPDataKind.NUMERIC) {
            if (lowerTypeName.contains("int128") || lowerTypeName.contains("int256")
                || lowerTypeName.contains("uint64") || lowerTypeName.contains("uint128") || lowerTypeName.contains("uint256")) {
                return new ClickhouseBigNumberValueHandler(type, preferences);
            } else {
                return new JDBCNumberValueHandler(type, preferences);
            }
        } else if (ClickhouseConstants.DATA_TYPE_IPV4.equals(lowerTypeName) || ClickhouseConstants.DATA_TYPE_IPV6.equals(lowerTypeName)) {
            return ClikhouseInetTypeValueHandler.INSTANCE;
        } else if (dataKind == DBPDataKind.DATETIME) {
            return new ClickHouseDateTimeValueHandler(preferences);
        }
        return null;
    }
}
