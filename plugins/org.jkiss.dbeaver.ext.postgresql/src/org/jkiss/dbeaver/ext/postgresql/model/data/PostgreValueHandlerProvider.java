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
package org.jkiss.dbeaver.ext.postgresql.model.data;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.impls.redshift.PostgreServerRedshift;
import org.jkiss.dbeaver.ext.postgresql.model.impls.redshift.RedshiftGeometryValueHandler;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDFormatSettings;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCNumberValueHandler;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCStandardValueHandlerProvider;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.Types;

/**
 * PostgreValueHandlerProvider
 */
public class PostgreValueHandlerProvider extends JDBCStandardValueHandlerProvider {
    @Nullable
    @Override
    public DBDValueHandler getValueHandler(DBPDataSource dataSource, DBDFormatSettings preferences, DBSTypedObject typedObject) {
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
                    return new PostgreTemporalAccessorValueHandler(preferences);
                } else {
                    return new PostgreDateTimeValueHandler(preferences);
                }
            default:
                switch (typedObject.getTypeName()) {
                    case PostgreConstants.TYPE_JSONB:
                    case PostgreConstants.TYPE_JSON:
                        return PostgreJSONValueHandler.INSTANCE;
                    case PostgreConstants.TYPE_HSTORE:
                        return PostgreHStoreValueHandler.INSTANCE;
                    case PostgreConstants.TYPE_BIT:
                    case PostgreConstants.TYPE_VARBIT:
                        return PostgreBitStringValueHandler.INSTANCE;
                    case PostgreConstants.TYPE_REFCURSOR:
                        return PostgreRefCursorValueHandler.INSTANCE;
                    case PostgreConstants.TYPE_MONEY:
                        return PostgreMoneyValueHandler.INSTANCE;
                    case PostgreConstants.TYPE_GEOMETRY:
                    case PostgreConstants.TYPE_GEOGRAPHY:
                        if (((PostgreDataSource) dataSource).getServerType() instanceof PostgreServerRedshift) {
                            return RedshiftGeometryValueHandler.INSTANCE;
                        }
                        return PostgreGeometryValueHandler.INSTANCE;
                    case PostgreConstants.TYPE_INTERVAL:
                        return PostgreIntervalValueHandler.INSTANCE;
                    default:
                        if (PostgreConstants.SERIAL_TYPES.containsKey(typedObject.getTypeName())) {
                            return new JDBCNumberValueHandler(typedObject, preferences);
                        }
                        if (typeID == Types.OTHER || typedObject.getDataKind() == DBPDataKind.STRING) {
                            return PostgreStringValueHandler.INSTANCE;
                        }
                }
        }
        return super.getValueHandler(dataSource, preferences, typedObject);
    }
}
