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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.data.DBDValueEditor;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.dialogs.data.TextViewDialog;
import org.jkiss.dbeaver.ui.properties.PropertySourceAbstract;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;

/**
 * JDBC string value handler
 */
public class JDBCStringValueHandler extends JDBCAbstractValueHandler {

    public static final JDBCStringValueHandler INSTANCE = new JDBCStringValueHandler();

    private static final int MAX_STRING_LENGTH = 0xffff;

    @Override
    protected Object fetchColumnValue(
        DBCExecutionContext context,
        JDBCResultSet resultSet,
        DBSTypedObject type,
        int index)
        throws SQLException
    {
        return resultSet.getString(index);
    }

    @Override
    public void bindParameter(JDBCExecutionContext context, JDBCPreparedStatement statement, DBSTypedObject paramType,
                              int paramIndex, Object value)
        throws SQLException
    {
        if (value == null) {
            statement.setNull(paramIndex, paramType.getTypeID());
        } else {
            statement.setString(paramIndex, value.toString());
        }
    }

    @Override
    public DBDValueEditor createEditor(DBDValueController controller)
        throws DBException
    {
        switch (controller.getEditType()) {
            case INLINE:
            case PANEL:
                return new ValueEditor<Text>(controller) {
                    @Override
                    protected Text createControl(Composite editPlaceholder)
                    {
                        final boolean inline = valueController.getEditType() == DBDValueController.EditType.INLINE;
                        final Text editor = new Text(valueController.getEditPlaceholder(),
                            SWT.BORDER | (inline ? SWT.NONE : SWT.MULTI | SWT.WRAP | SWT.V_SCROLL));
                        editor.setEditable(!valueController.isReadOnly());
                        editor.setTextLimit(MAX_STRING_LENGTH);
                        editor.setEditable(!valueController.isReadOnly());
                        return editor;
                    }
                    @Override
                    public void refreshValue()
                    {
                        control.setText(CommonUtils.toString(valueController.getValue()));
                        if (valueController.getEditType() == DBDValueController.EditType.INLINE) {
                            control.selectAll();
                        }
                    }
                    @Override
                    public Object extractValue(DBRProgressMonitor monitor)
                    {
                        return control.getText();
                    }
                };
            case EDITOR:
                return new TextViewDialog(controller);
            default:
                return null;
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
        return String.class;
    }

    @Override
    public Object getValueFromObject(DBCExecutionContext context, DBSTypedObject type, Object object, boolean copy) throws DBCException
    {
        if (object == null || object instanceof String) {
            return object;
        } else if (object instanceof char[]) {
            return new String((char[])object);
        } else if (object instanceof byte[]) {
            return new String((byte[])object);
        } else {
            log.warn("Unrecognized type '" + object.getClass().getName() + "' - can't convert to string");
            return null;
        }
    }

    @Override
    public void fillProperties(PropertySourceAbstract propertySource, DBDValueController controller)
    {
        propertySource.addProperty(
            "max_length", //$NON-NLS-1$
            CoreMessages.model_jdbc_max_length,
            controller.getAttributeMetaData().getMaxLength());
    }

}