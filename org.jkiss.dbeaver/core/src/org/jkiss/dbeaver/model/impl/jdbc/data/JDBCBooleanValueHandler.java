/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSColumnBase;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.dialogs.data.NumberViewDialog;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * JDBC number value handler
 */
public class JDBCBooleanValueHandler extends JDBCAbstractValueHandler {

    public static final JDBCBooleanValueHandler INSTANCE = new JDBCBooleanValueHandler();

    static final Log log = LogFactory.getLog(JDBCBooleanValueHandler.class);

    protected Object getColumnValue(DBRProgressMonitor monitor, ResultSet resultSet, DBSColumnBase column,
                                    int columnIndex)
        throws DBCException, SQLException
    {
        boolean value = resultSet.getBoolean(columnIndex);
        return resultSet.wasNull() ? null : value;
    }

    protected void bindParameter(DBRProgressMonitor monitor, PreparedStatement statement, DBSTypedObject paramType,
                                 int paramIndex, Object value) throws SQLException
    {
        if (value == null) {
            statement.setNull(paramIndex, paramType.getValueType());
        } else {
            statement.setBoolean(paramIndex, (Boolean)value);
        }
    }

    public Class getValueObjectType()
    {
        return Boolean.class;
    }

    public Object copyValueObject(DBRProgressMonitor monitor, Object value)
        throws DBCException
    {
        // Boolean is immutable
        return value;
    }

    public boolean editValue(final DBDValueController controller)
        throws DBException
    {
        if (controller.isInlineEdit()) {
            Object value = controller.getValue();

            Combo editor = new Combo(controller.getInlinePlaceholder(), SWT.READ_ONLY);
            editor.add("FALSE");
            editor.add("TRUE");
            editor.setText(value == null ? "FALSE" : value.toString().toUpperCase());
            editor.setFocus();
            initInlineControl(controller, editor, new ValueExtractor<Combo>() {
                public Object getValueFromControl(Combo control)
                {
                    switch (control.getSelectionIndex()) {
                        case 0: return Boolean.FALSE;
                        case 1: return Boolean.TRUE;
                        default: return null;
                    }
                }
            });
            return true;
        } else {
            NumberViewDialog dialog = new NumberViewDialog(controller);
            dialog.open();
            return true;
        }
    }

}