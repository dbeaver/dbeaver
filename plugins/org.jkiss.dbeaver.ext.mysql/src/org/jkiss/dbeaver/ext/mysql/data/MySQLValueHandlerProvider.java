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
package org.jkiss.dbeaver.ext.mysql.data;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDFormatSettings;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.data.DBDValueHandlerProvider;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCContentValueHandler;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCObjectValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.util.Locale;

/**
 * MySQL data types provider
 */
public class MySQLValueHandlerProvider implements DBDValueHandlerProvider {

    @Nullable
    @Override
    public DBDValueHandler getValueHandler(DBPDataSource dataSource, DBDFormatSettings preferences, DBSTypedObject typedObject)
    {
        DBPDataKind dataKind = typedObject.getDataKind();
        if (dataKind == DBPDataKind.DATETIME) {
            return new MySQLDateTimeValueHandler(preferences);
        } else if (dataKind == DBPDataKind.NUMERIC) {
            return new MySQLNumberValueHandler(typedObject, preferences);
        }

        String typeName = typedObject.getTypeName().toLowerCase(Locale.ENGLISH);
        switch (typeName) {
            case MySQLConstants.TYPE_JSON:
                return JDBCContentValueHandler.INSTANCE;
            case MySQLConstants.TYPE_GEOMETRY:
                return MySQLGeometryValueHandler.INSTANCE;
            case MySQLConstants.TYPE_ENUM:
            case MySQLConstants.TYPE_SET:
                return JDBCObjectValueHandler.INSTANCE;
        }

        return null;
    }

}