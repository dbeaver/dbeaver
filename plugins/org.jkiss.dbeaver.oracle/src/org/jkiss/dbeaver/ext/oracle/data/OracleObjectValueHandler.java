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
package org.jkiss.dbeaver.ext.oracle.data;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDValue;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCAbstractValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.SQLException;

/**
 * Object type support
 */
public class OracleObjectValueHandler extends JDBCAbstractValueHandler {

    public static final OracleObjectValueHandler INSTANCE = new OracleObjectValueHandler();

    @Override
    public String getValueDisplayString(DBSTypedObject column, Object value)
    {
        if (value != null) {
            return "[OBJECT]";
        } else {
            return super.getValueDisplayString(column, value);
        }
    }

    @Override
    protected DBDValue getColumnValue(DBCExecutionContext context, JDBCResultSet resultSet, DBSTypedObject column, int columnIndex) throws DBCException, SQLException
    {
        //final Object object = resultSet.getObject(columnIndex);
        Object object = resultSet.getObject(columnIndex);
        return new OracleObjectValue(object);
    }

    @Override
    protected void bindParameter(JDBCExecutionContext context, JDBCPreparedStatement statement, DBSTypedObject paramType, int paramIndex, Object value) throws DBCException, SQLException
    {
        throw new DBCException("Parameter bind is not implemented");
    }

    @Override
    public int getFeatures()
    {
        return FEATURE_NONE;
    }

    @Override
    public Class getValueObjectType()
    {
        return java.lang.Object.class;
    }

    @Override
    public Object copyValueObject(DBCExecutionContext context, DBSTypedObject column, Object value) throws DBCException
    {
        return null;
    }

    @Override
    public boolean editValue(DBDValueController controller) throws DBException
    {
        return false;
    }

}
