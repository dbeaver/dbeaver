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
package org.jkiss.dbeaver.ext.postgresql.model.data;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
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
    public DBDValueHandler getValueHandler(DBPDataSource dataSource, DBDPreferences preferences, DBSTypedObject typedObject)
    {
        int typeID = typedObject.getTypeID();
        if (typeID == Types.ARRAY) {
            return PostgreArrayValueHandler.INSTANCE;
        } else if (typeID == Types.STRUCT) {
            return PostgreStructValueHandler.INSTANCE;
        } else {
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
                default:
                    return null;
            }
        }
    }

}