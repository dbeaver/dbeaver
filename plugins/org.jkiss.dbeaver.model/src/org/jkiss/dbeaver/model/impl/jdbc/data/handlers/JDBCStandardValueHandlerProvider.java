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
package org.jkiss.dbeaver.model.impl.jdbc.data.handlers;

import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDPreferences;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.data.DBDValueHandlerProvider;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

/**
 * Standard JDBC data types handler provider
 */
public class JDBCStandardValueHandlerProvider implements DBDValueHandlerProvider {

    @Override
    public DBDValueHandler getValueHandler(DBPDataSource dataSource, DBDPreferences preferences, DBSTypedObject typedObject)
    {
        int valueType = typedObject.getTypeID();
        DBPDataKind dataKind = typedObject.getDataKind();//JDBCUtils.resolveDataKind(dataSource, typedObject.getTypeName(), valueType);
        switch (dataKind) {
            case BOOLEAN:
                return new JDBCBooleanValueHandler();
            case STRING:
                if (valueType == java.sql.Types.LONGVARCHAR || valueType == java.sql.Types.LONGNVARCHAR) {
                    // Eval long varchars as LOBs
                    return JDBCContentValueHandler.INSTANCE;
                } else {
                    return JDBCStringValueHandler.INSTANCE;
                }
            case NUMERIC:
                return new JDBCNumberValueHandler(preferences.getDataFormatterProfile());
            case DATETIME:
                return new JDBCDateTimeValueHandler(preferences.getDataFormatterProfile());
            case BINARY:
            case CONTENT:
                return JDBCContentValueHandler.INSTANCE;
            case ARRAY:
                return JDBCArrayValueHandler.INSTANCE;
            case STRUCT:
                return JDBCStructValueHandler.INSTANCE;
            case REFERENCE:
                return JDBCReferenceValueHandler.INSTANCE;
            case ROWID:
                return JDBCObjectValueHandler.INSTANCE;
            default:
                // Unknown type
                return null;
        }
    }

}
