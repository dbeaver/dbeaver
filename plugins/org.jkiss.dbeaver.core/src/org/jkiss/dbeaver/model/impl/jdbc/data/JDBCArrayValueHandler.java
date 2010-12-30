/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.IMenuManager;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDArray;
import org.jkiss.dbeaver.model.data.DBDValue;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.views.properties.PropertySourceAbstract;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

    protected Object getColumnValue(
        DBCExecutionContext context,
        ResultSet resultSet,
        DBSTypedObject column,
        int columnIndex)
        throws DBCException, SQLException
    {
        Object value = resultSet.getObject(columnIndex);

        if (value == null) {
            return JDBCArray.makeArray(context, null);
        } else if (value instanceof Array) {
            return JDBCArray.makeArray(context, (Array)value);
        } else {
            throw new DBCException("Unsupported array type: " + value.getClass().getName());
        }
    }

    protected void bindParameter(
        DBCExecutionContext context,
        PreparedStatement statement,
        DBSTypedObject paramType,
        int paramIndex,
        Object value)
        throws DBCException, SQLException
    {
        throw new DBCException("Unsupported value type: " + value);
    }

    public Class getValueObjectType()
    {
        return DBDArray.class;
    }

    public Object copyValueObject(DBCExecutionContext context, Object value)
        throws DBCException
    {
        return null;
    }

    @Override
    public void releaseValueObject(Object value)
    {
        if (value instanceof DBDValue) {
            ((DBDValue)value).release();
        }
    }

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

    public void fillContextMenu(IMenuManager menuManager, final DBDValueController controller)
        throws DBCException
    {
    }

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

    public boolean editValue(final DBDValueController controller)
        throws DBException
    {
        return false;
    }

}