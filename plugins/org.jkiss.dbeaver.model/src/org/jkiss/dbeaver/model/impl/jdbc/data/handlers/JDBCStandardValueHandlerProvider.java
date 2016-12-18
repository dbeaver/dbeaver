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
