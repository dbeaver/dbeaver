/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.IMenuManager;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDCursor;
import org.jkiss.dbeaver.model.data.DBDValue;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.dialogs.data.CursorViewDialog;
import org.jkiss.dbeaver.ui.dialogs.data.NumberViewDialog;
import org.jkiss.dbeaver.ui.properties.PropertySourceAbstract;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Struct;

/**
 * JDBC Object value handler.
 * Handle STRUCT types.
 *
 * @author Serge Rider
 */
public class JDBCObjectValueHandler extends JDBCAbstractValueHandler {

    static final Log log = LogFactory.getLog(JDBCObjectValueHandler.class);

    public static final JDBCObjectValueHandler INSTANCE = new JDBCObjectValueHandler();

    protected Object getColumnValue(
        DBCExecutionContext context,
        JDBCResultSet resultSet,
        DBSTypedObject column,
        int columnIndex)
        throws DBCException, SQLException
    {
        Object value = resultSet.getObject(columnIndex);
        if (value instanceof ResultSet) {
            value = new JDBCCursor(
                (JDBCExecutionContext) context,
                (ResultSet) value,
                column.getTypeName());
        }
        return value;
    }

    protected void bindParameter(
        JDBCExecutionContext context,
        JDBCPreparedStatement statement,
        DBSTypedObject paramType,
        int paramIndex,
        Object value)
        throws DBCException, SQLException
    {
        throw new DBCException("Unsupported value type: " + value);
    }

    public Class getValueObjectType()
    {
        return Object.class;
    }

    public Object copyValueObject(DBCExecutionContext context, DBSTypedObject column, Object value)
        throws DBCException
    {
        return null;
    }

    public String getValueDisplayString(DBSTypedObject column, Object value)
    {
        if (value instanceof DBDValue) {
            return value.toString();
        }
        return DBUtils.getDefaultValueDisplayString(value);
    }

    public void fillContextMenu(IMenuManager menuManager, final DBDValueController controller)
        throws DBCException
    {
    }

    public void fillProperties(PropertySourceAbstract propertySource, DBDValueController controller)
    {
    }

    public boolean editValue(final DBDValueController controller)
        throws DBException
    {
        if (controller.isInlineEdit()) {
            return false;
        }
        final Object value = controller.getValue();
        if (value instanceof DBDCursor) {
            CursorViewDialog dialog = new CursorViewDialog(controller);
            dialog.open();
            return true;
        }
        return false;
    }

}