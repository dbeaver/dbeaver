/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.model.data;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDPreferences;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.data.DBDValueHandlerProvider;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.Types;

/**
 * PostgreValueHandlerProvider
 */
public class PostgreValueHandlerProvider implements DBDValueHandlerProvider {

    @Nullable
    @Override
    public DBDValueHandler getValueHandler(DBPDataSource dataSource, DBDPreferences preferences, DBSTypedObject typedObject) {
//        // FIXME: This doesn't work as data type information is not available during RS metadata reading
//        DBSDataType dataType = DBUtils.getDataType(typedObject);
//        if (dataType instanceof PostgreDataType && ((PostgreDataType) dataType).getTypeCategory() == PostgreTypeCategory.E) {
//            return PostgreEnumValueHandler.INSTANCE;
//        }
        int typeID = typedObject.getTypeID();
        switch (typeID) {
            case Types.ARRAY:
                return PostgreArrayValueHandler.INSTANCE;
            case Types.STRUCT:
                return PostgreStructValueHandler.INSTANCE;
            case Types.DATE:
            case Types.TIME:
            case Types.TIME_WITH_TIMEZONE:
            case Types.TIMESTAMP:
            case Types.TIMESTAMP_WITH_TIMEZONE:
                if (((PostgreDataSource) dataSource).getServerType().supportsTemporalAccessor()) {
                    return new PostgreTemporalAccessorValueHandler(preferences.getDataFormatterProfile());
                } else {
                    return new PostgreDateTimeValueHandler(preferences.getDataFormatterProfile());
                }
            default:
                switch (typedObject.getTypeName()) {
                    case PostgreConstants.TYPE_JSONB:
                    case PostgreConstants.TYPE_JSON:
                        return PostgreJSONValueHandler.INSTANCE;
                    case PostgreConstants.TYPE_HSTORE:
                        return PostgreHStoreValueHandler.INSTANCE;
                    case PostgreConstants.TYPE_BIT:
                        return PostgreBitStringValueHandler.INSTANCE;
                    case PostgreConstants.TYPE_REFCURSOR:
                        return PostgreRefCursorValueHandler.INSTANCE;
                    case PostgreConstants.TYPE_MONEY:
                        return PostgreMoneyValueHandler.INSTANCE;
                    case PostgreConstants.TYPE_GEOMETRY:
                    case PostgreConstants.TYPE_GEOGRAPHY:
                        return PostgreGeometryValueHandler.INSTANCE;
                    case PostgreConstants.TYPE_INTERVAL:
                        return PostgreIntervalValueHandler.INSTANCE;
                    default:
                        if (typedObject.getDataKind() == DBPDataKind.STRING) {
                            return PostgreStringValueHandler.INSTANCE;
                        }
                        return null;
                }
        }
    }

}
