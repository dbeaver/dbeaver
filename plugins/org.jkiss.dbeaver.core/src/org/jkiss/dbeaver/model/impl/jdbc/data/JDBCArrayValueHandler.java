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
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.data.DBDArray;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.data.DBDValueEditor;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.dialogs.data.ComplexObjectEditor;
import org.jkiss.dbeaver.ui.dialogs.data.DefaultValueViewDialog;
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
    protected Object fetchColumnValue(
        DBCExecutionContext context,
        JDBCResultSet resultSet,
        DBSTypedObject type,
        int index)
        throws DBCException, SQLException
    {
        Object value = resultSet.getObject(index);
        return getValueFromObject(context, type, value, false);
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
        return FEATURE_VIEWER | FEATURE_EDITOR;
    }

    @Override
    public Class getValueObjectType()
    {
        return DBDArray.class;
    }

    @Override
    public Object getValueFromObject(DBCExecutionContext context, DBSTypedObject type, Object object, boolean copy) throws DBCException
    {
        if (object == null) {
            return JDBCArray.makeArray((JDBCExecutionContext) context, null);
        } else if (object instanceof JDBCArray) {
            return copy ? ((JDBCArray) object).cloneValue(context.getProgressMonitor()) : object;
        } else if (object instanceof Array) {
            return JDBCArray.makeArray((JDBCExecutionContext) context, (Array)object);
        } else {
            throw new DBCException(CoreMessages.model_jdbc_exception_unsupported_array_type_ + object.getClass().getName());
        }
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
    public DBDValueEditor createEditor(DBDValueController controller)
        throws DBException
    {
        switch (controller.getEditType()) {
            case PANEL:
                return new ValueEditor<Tree>(controller) {
                ComplexObjectEditor editor;
                @Override
                public void refreshValue()
                {
                    editor.setModel((DBDArray) valueController.getValue());
                }

                @Override
                protected Tree createControl(Composite editPlaceholder)
                {
                    editor = new ComplexObjectEditor(valueController.getEditPlaceholder(), SWT.BORDER);
                    editor.setModel((DBDArray) valueController.getValue());
                    return editor.getTree();
                }

                @Override
                public Object extractValue(DBRProgressMonitor monitor)
                {
                    return editor.getInput();
                }
            };
            case EDITOR:
                return new DefaultValueViewDialog(controller);
            default:
                return null;
        }
    }

}