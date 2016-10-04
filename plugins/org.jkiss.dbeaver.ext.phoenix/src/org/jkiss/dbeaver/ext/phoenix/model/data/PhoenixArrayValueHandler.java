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
package org.jkiss.dbeaver.ext.phoenix.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.data.DBDCollection;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCCollection;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCArrayValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.Array;
import java.sql.SQLException;
import java.sql.Types;


/**
 * PhoenixArrayValueHandler
 */
public class PhoenixArrayValueHandler extends JDBCArrayValueHandler {
    public static final PhoenixArrayValueHandler INSTANCE = new PhoenixArrayValueHandler();
    
    public static final String PHOENIX_ARRAY_TYPE = "PhoenixArray";

    @Override
    public JDBCCollection getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object, boolean copy) throws DBCException
    {
        if (object != null && object.getClass().getSimpleName().contains(PHOENIX_ARRAY_TYPE)) {
            return JDBCCollection.makeArray((JDBCSession) session, type, (Array) object);
        }
        return super.getValueFromObject(session, type, object, copy);
    }
    
    @Override
    protected void bindParameter(
        JDBCSession session,
        JDBCPreparedStatement statement,
        DBSTypedObject paramType,
        int paramIndex,
        Object value)
        throws DBCException, SQLException
    {
        if (value == null) {
            statement.setNull(paramIndex, Types.ARRAY);
        } else if (value instanceof DBDCollection) {
            DBDCollection collection = (DBDCollection) value;
            if (collection.isNull()) {
                statement.setNull(paramIndex, Types.ARRAY);
            } else if (collection instanceof JDBCCollection) {
                statement.setArray(paramIndex, ((JDBCCollection) collection).getArrayValue());
            } else {
                statement.setArray(paramIndex, (Array)collection.getRawValue());
            }
        } else {
            throw new DBCException("Array parameter type '" + value.getClass().getName() + "' not supported");
        }
    }
    
}
