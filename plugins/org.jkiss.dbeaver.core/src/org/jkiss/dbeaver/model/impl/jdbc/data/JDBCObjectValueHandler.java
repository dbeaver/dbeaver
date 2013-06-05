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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.dialogs.data.CursorViewDialog;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.RowId;
import java.sql.SQLException;

/**
 * JDBC Object value handler.
 * Handle STRUCT types.
 *
 * @author Serge Rider
 */
public class JDBCObjectValueHandler extends JDBCAbstractValueHandler {

    static final Log log = LogFactory.getLog(JDBCObjectValueHandler.class);

    public static final JDBCObjectValueHandler INSTANCE = new JDBCObjectValueHandler();

    @Override
    protected Object fetchColumnValue(
        DBCExecutionContext context,
        JDBCResultSet resultSet,
        DBSTypedObject type,
        int index)
        throws DBCException, SQLException
    {
        Object value = resultSet.getObject(index);
        if (value instanceof ResultSet) {
            value = new JDBCCursor(
                (JDBCExecutionContext) context,
                (ResultSet) value,
                type.getTypeName());
        } else if (value instanceof RowId) {
            value = new JDBCRowId((RowId) value);
        }
        return value;
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
        throw new DBCException(CoreMessages.model_jdbc_unsupported_value_type_ + value);
    }

    @Override
    public int getFeatures()
    {
        return FEATURE_VIEWER | FEATURE_EDITOR;
    }

    @Override
    public Class getValueObjectType()
    {
        return Object.class;
    }

    @Override
    public Object getValueFromObject(DBCExecutionContext context, DBSTypedObject type, Object object, boolean copy) throws DBCException
    {
        if (copy && object != null) {
            if (object instanceof DBDValueCloneable) {
                return ((DBDValueCloneable) object).cloneValue(context.getProgressMonitor());
            }
            throw new DBCException("Can't copy object value " + object);
        }
        return object;
    }

    @Override
    public String getValueDisplayString(DBSTypedObject column, Object value, DBDDisplayFormat format)
    {
        if (value instanceof DBDValue) {
            return value.toString();
        }
        return DBUtils.getDefaultValueDisplayString(value);
    }

    @Override
    public DBDValueEditor createEditor(final DBDValueController controller)
        throws DBException
    {
        switch (controller.getEditType()) {
            case PANEL:
                return new ValueEditor<Text>(controller) {
                    @Override
                    protected Text createControl(Composite editPlaceholder)
                    {
                        return new Text(valueController.getEditPlaceholder(),
                            SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.READ_ONLY);
                    }
                    @Override
                    public void primeEditorValue(Object value) throws DBException
                    {
                        control.setText(CommonUtils.toString(value));
                    }
                    @Override
                    public Object extractEditorValue()
                    {
                        return null;
                    }
                };
            case EDITOR:
                final Object value = controller.getValue();
                if (value instanceof DBDCursor) {
                    return new CursorViewDialog(controller);
                }
                return null;
            default:
                return null;
        }
    }

}