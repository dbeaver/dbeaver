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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.IMenuManager;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.data.DBDArray;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.properties.PropertySourceAbstract;

import java.sql.Array;
import java.sql.SQLException;

/**
 * JDBC Array value handler.
 * Handle ARRAY types.
 *
 * @author Serge Rider
 */
public class JDBCArrayValueHandler extends JDBCAbstractValueHandler {

    static final Log log = LogFactory.getLog(JDBCArrayValueHandler.class);

    public static final JDBCArrayValueHandler INSTANCE = new JDBCArrayValueHandler();

    @Override
    protected Object getColumnValue(
        DBCExecutionContext context,
        JDBCResultSet resultSet,
        DBSTypedObject column,
        int columnIndex)
        throws DBCException, SQLException
    {
        Object value = resultSet.getObject(columnIndex);

        if (value == null) {
            return JDBCArray.makeArray((JDBCExecutionContext) context, null);
        } else if (value instanceof Array) {
            return JDBCArray.makeArray((JDBCExecutionContext) context, (Array)value);
        } else {
            throw new DBCException(CoreMessages.model_jdbc_exception_unsupported_array_type_ + value.getClass().getName());
        }
    }

    @Override
    protected void bindParameter(
        JDBCExecutionContext context,
        JDBCPreparedStatement statement,
        DBSTypedObject paramType,
        int paramIndex,
        Object value)
        throws DBCException, SQLException
    {
        throw new DBCException(CoreMessages.model_jdbc_exception_unsupported_value_type_ + value);
    }

    @Override
    public int getFeatures()
    {
        return FEATURE_NONE;
    }

    @Override
    public Class getValueObjectType()
    {
        return DBDArray.class;
    }

    @Override
    public Object copyValueObject(DBCExecutionContext context, DBSTypedObject column, Object value)
        throws DBCException
    {
        return null;
    }

    @Override
    public String getValueDisplayString(DBSTypedObject column, Object value)
    {
        if (value instanceof JDBCArray) {
            String displayString = ((JDBCArray) value).makeArrayString();
            if (displayString != null) {
                return displayString;
            }
        }
        return super.getValueDisplayString(column, value);
    }

    @Override
    public void fillContextMenu(IMenuManager menuManager, final DBDValueController controller)
        throws DBCException
    {
    }

    @Override
    public void fillProperties(PropertySourceAbstract propertySource, DBDValueController controller)
    {
        try {
            Object value = controller.getValue();
            if (value instanceof DBDArray) {
/*
                propertySource.addProperty(
                    "array_length",
                    "Length",
                    ((DBDArray)value).getLength());
*/
            }
        }
        catch (Exception e) {
            log.warn("Could not extract array value information", e);
        }
    }

    @Override
    public boolean editValue(final DBDValueController controller)
        throws DBException
    {
        return false;
    }

}