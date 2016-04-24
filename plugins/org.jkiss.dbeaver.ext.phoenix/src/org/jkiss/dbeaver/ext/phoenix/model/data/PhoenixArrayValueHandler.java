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
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCArray;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCArrayValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.lang.reflect.Method;
import java.sql.Array;
import java.sql.SQLException;


/**
 * PhoenixArrayValueHandler
 */
public class PhoenixArrayValueHandler extends JDBCArrayValueHandler {
    public static final PhoenixArrayValueHandler INSTANCE = new PhoenixArrayValueHandler();
    
    public static final String PHOENIX_ARRAY_TYPE = "PhoenixArray";

    @Override
    public Object getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object, boolean copy) throws DBCException
    {
        if (object != null && object.getClass().getSimpleName().contains(PHOENIX_ARRAY_TYPE)) {
            return JDBCArray.makeArray((JDBCSession) session, (Array)object);
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
    	if (value instanceof JDBCArray) {
    		JDBCArray jdbcArray = (JDBCArray)value;
    		String elementType = jdbcArray.getComponentType().getTypeName();
    		Array arrayVal  = statement.getConnection().createArrayOf(elementType, (Object [])jdbcArray.getRawValue());
    		statement.setArray(paramIndex, arrayVal);
    	} else {
    		throw new DBCException("Unsupported value type: " + value);
    	}
        
        
    }

}
