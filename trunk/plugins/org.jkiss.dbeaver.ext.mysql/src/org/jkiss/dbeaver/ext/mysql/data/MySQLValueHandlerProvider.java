/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.mysql.data;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDPreferences;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.data.DBDValueHandlerProvider;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.Types;

/**
 * MySQL data types provider
 */
public class MySQLValueHandlerProvider implements DBDValueHandlerProvider {

    @Override
    public DBPImage getTypeImage(DBSTypedObject type)
    {
        return DBUtils.getDataIcon(type);
    }

    @Nullable
    @Override
    public DBDValueHandler getHandler(DBPDataSource dataSource, DBDPreferences preferences, DBSTypedObject typedObject)
    {
        if (MySQLConstants.TYPE_NAME_ENUM.equalsIgnoreCase(typedObject.getTypeName())) {
            return MySQLEnumValueHandler.INSTANCE;
        } else if (MySQLConstants.TYPE_NAME_SET.equalsIgnoreCase(typedObject.getTypeName())) {
            return MySQLSetValueHandler.INSTANCE;
        } else {
            int typeID = typedObject.getTypeID();
            if (typeID == Types.DATE || typeID == Types.TIME || typeID == Types.TIMESTAMP) {
                return new MySQLDateTimeValueHandler(preferences.getDataFormatterProfile());
            } else {
                return null;
            }
        }
    }

}