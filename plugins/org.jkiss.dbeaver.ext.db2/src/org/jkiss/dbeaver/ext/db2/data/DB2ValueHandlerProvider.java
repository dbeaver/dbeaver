/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
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
package org.jkiss.dbeaver.ext.db2.data;

import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDPreferences;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.data.DBDValueHandlerProvider;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

/**
 * DB2 Data Types provider
 * 
 * @author Denis Forveille
 */
public class DB2ValueHandlerProvider implements DBDValueHandlerProvider {

    @Override
    public DBDValueHandler getValueHandler(DBPDataSource dataSource, DBDPreferences preferences, DBSTypedObject typedObject)
    {
        final String typeName = typedObject.getTypeName();
        if (DB2Constants.TYPE_NAME_DECFLOAT.equals(typeName)) {
            return new DB2DecFloatValueHandler(preferences.getDataFormatterProfile());
        }
        return null;
    }
}