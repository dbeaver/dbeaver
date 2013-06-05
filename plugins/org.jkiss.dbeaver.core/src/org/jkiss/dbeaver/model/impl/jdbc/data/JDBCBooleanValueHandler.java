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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
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
import org.jkiss.dbeaver.ui.dialogs.data.DefaultValueViewDialog;

import java.sql.SQLException;

/**
 * JDBC number value handler
 */
public class JDBCBooleanValueHandler extends JDBCAbstractValueHandler {

    public static final JDBCBooleanValueHandler INSTANCE = new JDBCBooleanValueHandler();

    static final Log log = LogFactory.getLog(JDBCBooleanValueHandler.class);

    @Override
    protected Object fetchColumnValue(DBCExecutionContext context, JDBCResultSet resultSet, DBSTypedObject type,
                                      int index)
        throws DBCException, SQLException
    {
        boolean value = resultSet.getBoolean(index);
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
    public Object getValueFromObject(DBCExecutionContext context, DBSTypedObject type, Object object, boolean copy) throws DBCException
    {
        if (object == null) {
            return null;
        } else if (object instanceof Boolean) {
            return object;
        } else if (object instanceof String) {
            return Boolean.valueOf((String)object);
        } else if (object instanceof Number) {
            return ((Number) object).byteValue() != 0;
        } else {
            log.warn("Unrecognized type '" + object.getClass().getName() + "' - can't convert to boolean");
            return null;
        }
    }

    @Override
    public DBDValueEditor createEditor(DBDValueController controller)
        throws DBException
    {
        switch (controller.getEditType()) {
            case INLINE:
            {
                return new ValueEditor<Combo>(controller) {
                    @Override
                    protected Combo createControl(Composite editPlaceholder)
                    {
                        final Combo editor = new Combo(editPlaceholder, SWT.READ_ONLY);
                        editor.add("FALSE");
                        editor.add("TRUE");
                        editor.setEnabled(!valueController.isReadOnly());
                        return editor;
                    }
                    @Override
                    public Object extractEditorValue()
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
                    @Override
                    public void refreshValue()
                    {
                        Object value = valueController.getValue();
                        control.setText(value == null ? "FALSE" : value.toString().toUpperCase());
                    }
                };
            }
            case PANEL:
            {
                return new ValueEditor<List>(controller) {
                    @Override
                    public Object extractEditorValue()
                    {
                        return control.getSelectionIndex() == 1;
                    }
                    @Override
                    public void refreshValue()
                    {
                        Object value = valueController.getValue();
                        control.setSelection(Boolean.TRUE.equals(value) ? 1 : 0);
                    }

                    @Override
                    protected List createControl(Composite editPlaceholder)
                    {
                        final List editor = new List(valueController.getEditPlaceholder(), SWT.SINGLE | SWT.READ_ONLY);
                        editor.add("FALSE");
                        editor.add("TRUE");
                        return editor;
                    }
                };
            }
            case EDITOR:
                return new DefaultValueViewDialog(controller);
            default:
                return null;
        }
    }

}