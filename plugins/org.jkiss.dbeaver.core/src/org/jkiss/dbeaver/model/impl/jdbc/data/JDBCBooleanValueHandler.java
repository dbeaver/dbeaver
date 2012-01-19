/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.dialogs.data.NumberViewDialog;

import java.sql.SQLException;

/**
 * JDBC number value handler
 */
public class JDBCBooleanValueHandler extends JDBCAbstractValueHandler {

    public static final JDBCBooleanValueHandler INSTANCE = new JDBCBooleanValueHandler();

    static final Log log = LogFactory.getLog(JDBCBooleanValueHandler.class);

    protected Object getColumnValue(DBCExecutionContext context, JDBCResultSet resultSet, DBSTypedObject column,
                                    int columnIndex)
        throws DBCException, SQLException
    {
        boolean value = resultSet.getBoolean(columnIndex);
        return resultSet.wasNull() ? null : value;
    }

    protected void bindParameter(JDBCExecutionContext context, JDBCPreparedStatement statement, DBSTypedObject paramType,
                                 int paramIndex, Object value) throws SQLException
    {
        if (value == null) {
            statement.setNull(paramIndex, paramType.getTypeID());
        } else {
            statement.setBoolean(paramIndex, (Boolean)value);
        }
    }

    public int getFeatures()
    {
        return FEATURE_VIEWER | FEATURE_EDITOR | FEATURE_INLINE_EDITOR;
    }

    public Class getValueObjectType()
    {
        return Boolean.class;
    }

    public Object copyValueObject(DBCExecutionContext context, DBSTypedObject column, Object value)
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

            CCombo editor = new CCombo(controller.getInlinePlaceholder(), SWT.READ_ONLY);
            editor.add("FALSE");
            editor.add("TRUE");
            editor.setText(value == null ? "FALSE" : value.toString().toUpperCase());
            editor.setFocus();
            initInlineControl(controller, editor, new ValueExtractor<CCombo>() {
                public Object getValueFromControl(CCombo control)
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