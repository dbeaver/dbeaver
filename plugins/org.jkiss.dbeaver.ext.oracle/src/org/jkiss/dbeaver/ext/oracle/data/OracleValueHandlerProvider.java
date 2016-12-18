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
package org.jkiss.dbeaver.ext.oracle.data;

import org.jkiss.dbeaver.ext.oracle.model.OracleConstants;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDPreferences;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.data.DBDValueHandlerProvider;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.Types;

/**
 * Oracle data types provider
 */
public class OracleValueHandlerProvider implements DBDValueHandlerProvider {

    @Override
    public DBDValueHandler getValueHandler(DBPDataSource dataSource, DBDPreferences preferences, DBSTypedObject typedObject)
    {
        final String typeName = typedObject.getTypeName();
        if (typedObject.getTypeID() == Types.BLOB) {
            return OracleBLOBValueHandler.INSTANCE;
        } else if (typedObject.getTypeID() == Types.CLOB || typedObject.getTypeID() == Types.NCLOB) {
            return OracleCLOBValueHandler.INSTANCE;
        } else if (OracleConstants.TYPE_NAME_XML.equals(typeName) || OracleConstants.TYPE_FQ_XML.equals(typeName)) {
            return OracleXMLValueHandler.INSTANCE;
        } else if (OracleConstants.TYPE_NAME_BFILE.equals(typeName)) {
            return OracleBFILEValueHandler.INSTANCE;
        } else if (typedObject.getTypeID() == java.sql.Types.STRUCT) {
            return OracleObjectValueHandler.INSTANCE;
        } else if (typeName.contains("TIMESTAMP") || typedObject.getDataKind() == DBPDataKind.DATETIME) {
            return new OracleTimestampValueHandler(preferences.getDataFormatterProfile());
        } else {
            return null;
        }
    }

}