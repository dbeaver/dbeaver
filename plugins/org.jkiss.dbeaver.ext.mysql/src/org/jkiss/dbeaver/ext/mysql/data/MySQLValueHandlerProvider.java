/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.data.gis.handlers.GISGeometryValueHandler;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDPreferences;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.data.DBDValueHandlerProvider;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCContentValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

/**
 * MySQL data types provider
 */
public class MySQLValueHandlerProvider implements DBDValueHandlerProvider {

    @Nullable
    @Override
    public DBDValueHandler getValueHandler(DBPDataSource dataSource, DBDPreferences preferences, DBSTypedObject typedObject)
    {
        if (typedObject.getDataKind() == DBPDataKind.DATETIME) {
            return new MySQLDateTimeValueHandler(preferences.getDataFormatterProfile());
        } else if (typedObject.getDataKind() == DBPDataKind.NUMERIC) {
            return new MySQLNumberValueHandler(typedObject, preferences.getDataFormatterProfile());
        } else if (typedObject.getTypeName().equalsIgnoreCase(MySQLConstants.TYPE_JSON)) {
            return JDBCContentValueHandler.INSTANCE;
        } else if (typedObject.getTypeName().equalsIgnoreCase(MySQLConstants.TYPE_GEOMETRY)) {
            return GISGeometryValueHandler.INSTANCE;
        } else {
            return null;
        }
    }

}