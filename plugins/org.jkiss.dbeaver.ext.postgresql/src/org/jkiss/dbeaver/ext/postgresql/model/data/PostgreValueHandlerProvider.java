/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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
                default:
                    return null;
            }
        }
    }

}