/*
 * Copyright (C) 2010-2013 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.mssql.data;

import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.ext.mssql.MSSQLConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDPreferences;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.data.DBDValueHandlerProvider;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.Types;

/**
 * MSSQL data types provider
 */
public class MSSQLValueHandlerProvider implements DBDValueHandlerProvider {

    @Override
    public Image getTypeImage(DBSTypedObject type)
    {
        return DBUtils.getDataIcon(type).getImage();
    }

    @Override
    public DBDValueHandler getHandler(DBDPreferences preferences, String typeName, int valueType)
    {
        if (MSSQLConstants.TYPE_NAME_ENUM.equalsIgnoreCase(typeName)) {
            return MSSQLEnumValueHandler.INSTANCE;
        } else if (MSSQLConstants.TYPE_NAME_SET.equalsIgnoreCase(typeName)) {
            return MSSQLSetValueHandler.INSTANCE;
        } else if (valueType == Types.DATE || valueType == Types.TIME || valueType == Types.TIMESTAMP) {
            return new MSSQLDateTimeValueHandler(preferences.getDataFormatterProfile());
        } else {
            return null;
        }
    }

}