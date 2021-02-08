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
package org.jkiss.dbeaver.model.impl.jdbc.data.handlers;

import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDFormatSettings;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.data.DBDValueHandlerProvider;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

/**
 * Standard JDBC data types handler provider
 */
public class JDBCStandardValueHandlerProvider implements DBDValueHandlerProvider {

    private static final boolean LONGVARCHAR_AS_LOB = false;

    @Override
    public DBDValueHandler getValueHandler(DBPDataSource dataSource, DBDFormatSettings preferences, DBSTypedObject typedObject)
    {
        int valueType = typedObject.getTypeID();
        DBPDataKind dataKind = typedObject.getDataKind();//JDBCUtils.resolveDataKind(dataSource, typedObject.getTypeName(), valueType);
        switch (dataKind) {
            case BOOLEAN:
                return new JDBCBooleanValueHandler();
            case STRING:
                if (LONGVARCHAR_AS_LOB && (valueType == java.sql.Types.LONGVARCHAR || valueType == java.sql.Types.LONGNVARCHAR)) {
                    // Eval long varchars as LOBs
                    return JDBCContentValueHandler.INSTANCE;
                } else {
                    return JDBCStringValueHandler.INSTANCE;
                }
            case NUMERIC:
                return new JDBCNumberValueHandler(typedObject, preferences);
            case DATETIME:
                return new JDBCDateTimeValueHandler(preferences);
            case BINARY:
            case CONTENT:
                if ("UUID".equalsIgnoreCase(typedObject.getTypeName())) {
                    return JDBCUUIDValueHandler.INSTANCE;
                }
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
