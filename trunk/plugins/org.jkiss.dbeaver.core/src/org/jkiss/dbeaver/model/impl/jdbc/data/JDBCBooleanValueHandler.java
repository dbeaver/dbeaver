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
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.widgets.List;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.data.DBDValueEditor;
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

    @Override
    protected Object getColumnValue(DBCExecutionContext context, JDBCResultSet resultSet, DBSTypedObject column,
                                    int columnIndex)
        throws DBCException, SQLException
    {
        boolean value = resultSet.getBoolean(columnIndex);
        return resultSet.wasNull() ? null : value;
    }

    @Override
    protected void bindParameter(JDBCExecutionContext context, JDBCPreparedStatement statement, DBSTypedObject paramType,
                                 int paramIndex, Object value) throws SQLException
    {
        if (value == null) {
            statement.setNull(paramIndex, paramType.getTypeID());
        } else {
            statement.setBoolean(paramIndex, (Boolean)value);
        }
    }

    @Override
    public int getFeatures()
    {
        return FEATURE_VIEWER | FEATURE_EDITOR | FEATURE_INLINE_EDITOR;
    }

    @Override
    public Class getValueObjectType()
    {
        return Boolean.class;
    }

    @Override
    public Object copyValueObject(DBCExecutionContext context, DBSTypedObject column, Object value)
        throws DBCException
    {
        // Boolean is immutable
        return value;
    }

    @Override
    public DBDValueEditor createEditor(final DBDValueController controller)
        throws DBException
    {
        switch (controller.getEditType()) {
            case INLINE:
            {
                final CCombo editor = new CCombo(controller.getEditPlaceholder(), SWT.READ_ONLY);
                initInlineControl(controller, editor, new ValueExtractor<CCombo>() {
                    @Override
                    public Object getValueFromControl(CCombo control)
                    {
                        switch (control.getSelectionIndex()) {
                            case 0:
                                return Boolean.FALSE;
                            case 1:
                                return Boolean.TRUE;
                            default:
                                return null;
                        }
                    }
                });
                editor.add("FALSE");
                editor.add("TRUE");
                return new DBDValueEditor() {
                    @Override
                    public void refreshValue()
                    {
                        Object value = controller.getValue();
                        editor.setText(value == null ? "FALSE" : value.toString().toUpperCase());
                    }
                };
            }
            case PANEL:
            {
                final List editor = new List(controller.getEditPlaceholder(), SWT.SINGLE | SWT.READ_ONLY);
                initInlineControl(controller, editor, new ValueExtractor<List>() {
                    @Override
                    public Object getValueFromControl(List control)
                    {
                        return control.getSelectionIndex() == 1;
                    }
                });
                editor.add("FALSE");
                editor.add("TRUE");
                return new DBDValueEditor() {
                    @Override
                    public void refreshValue()
                    {
                        Object value = controller.getValue();
                        editor.setSelection(Boolean.TRUE.equals(value) ? 1 : 0);
                    }
                };
            }
            case EDITOR:
                return new NumberViewDialog(controller);
            default:
                return null;
        }
    }

}