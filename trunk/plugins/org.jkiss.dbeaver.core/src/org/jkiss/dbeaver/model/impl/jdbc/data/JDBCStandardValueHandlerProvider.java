/*
 * Copyright (C) 2010-2012 Serge Rieder
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
package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDPreferences;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.data.DBDValueHandlerProvider;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.struct.DBSDataKind;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

/**
 * standard JDBC data types provider
 */
public class JDBCStandardValueHandlerProvider implements DBDValueHandlerProvider {

    @Override
    public Image getTypeImage(DBSTypedObject type)
    {
        return DBUtils.getDataIcon(type).getImage();
    }

    @Override
    public DBDValueHandler getHandler(DBDPreferences preferences, String typeName, int valueType)
    {
        DBSDataKind dataKind = JDBCUtils.getDataKind(typeName, valueType);
        switch (dataKind) {
            case BOOLEAN:
                return new JDBCBooleanValueHandler();
            case STRING:
                if (valueType == java.sql.Types.LONGVARCHAR || valueType == java.sql.Types.LONGNVARCHAR) {
                    // Eval longvarchars as LOBs
                    return new JDBCContentValueHandler();
                } else {
                    return new JDBCStringValueHandler();
                }
            case NUMERIC:
                return new JDBCNumberValueHandler(preferences.getDataFormatterProfile());
            case DATETIME:
                return new JDBCDateTimeValueHandler(preferences.getDataFormatterProfile());
            case BINARY:
            case LOB:
                return new JDBCContentValueHandler();
            case ARRAY:
                return new JDBCArrayValueHandler();
            case STRUCT:
                return new JDBCStructValueHandler();
            default:
                return null;
        }
    }

}
