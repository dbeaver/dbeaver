/*
 * Copyright (C) 2010-2015 Serge Rieder
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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.data.DBDValueEditor;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
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

    @NotNull
    @Override
    public String getValueDisplayString(@NotNull DBSTypedObject column, Object value, @NotNull DBDDisplayFormat format)
    {
        if (value != null) {
            return "[OBJECT]";
        } else {
            return super.getValueDisplayString(column, value, format);
        }
    }

    @Override
    protected OracleObjectValue fetchColumnValue(DBCSession session, JDBCResultSet resultSet, DBSTypedObject type, int index) throws DBCException, SQLException
    {
        //final Object object = resultSet.getObject(columnIndex);
        Object object = resultSet.getObject(index);
        return getValueFromObject(session, type, object, false);
    }

    @Override
    protected void bindParameter(JDBCSession session, JDBCPreparedStatement statement, DBSTypedObject paramType, int paramIndex, Object value) throws DBCException, SQLException
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
    public OracleObjectValue getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object, boolean copy) throws DBCException
    {
        if (object == null) {
            return new OracleObjectValue(null);
        } else if (object instanceof OracleObjectValue) {
            return copy ? new OracleObjectValue(((OracleObjectValue) object).getValue()) : (OracleObjectValue)object;
        } else {
            return new OracleObjectValue(object);
        }
    }

    @Override
    public DBDValueEditor createEditor(@NotNull DBDValueController controller) throws DBException
    {
        return null;
    }

}
