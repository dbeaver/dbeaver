/*
 * Copyright (C) 2010-2015 Serge Rieder
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

import org.jkiss.dbeaver.core.Log;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.data.editors.BaseValueEditor;
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

    static final Log log = Log.getLog(JDBCObjectValueHandler.class);

    public static final JDBCObjectValueHandler INSTANCE = new JDBCObjectValueHandler();

    @Override
    protected Object fetchColumnValue(
        DBCSession session,
        JDBCResultSet resultSet,
        DBSTypedObject type,
        int index)
        throws DBCException, SQLException
    {
        Object value = resultSet.getObject(index);
        if (value instanceof ResultSet) {
            value = new JDBCCursor(
                (JDBCSession) session,
                (ResultSet) value,
                type.getTypeName());
        } else if (value instanceof RowId) {
            value = new JDBCRowId((RowId) value);
        }
        return value;
    }

    @Override
    protected void bindParameter(
        JDBCSession session,
        JDBCPreparedStatement statement,
        DBSTypedObject paramType,
        int paramIndex,
        Object value)
        throws DBCException, SQLException
    {
        if (value == null) {
            statement.setNull(paramIndex, paramType.getTypeID());
        } else if (value instanceof JDBCRowId) {
            statement.setRowId(paramIndex, ((JDBCRowId) value).getValue());
        } else {
            statement.setObject(paramIndex, value);
        }
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
    public Object getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object, boolean copy) throws DBCException
    {
        if (copy && object != null) {
            if (object instanceof DBDValueCloneable) {
                return ((DBDValueCloneable) object).cloneValue(session.getProgressMonitor());
            }
            throw new DBCException("Can't copy object value " + object);
        }
        return object;
    }

    @NotNull
    @Override
    public String getValueDisplayString(@NotNull DBSTypedObject column, Object value, @NotNull DBDDisplayFormat format)
    {
        if (value instanceof DBDValue) {
            return value.toString();
        }
        return DBUtils.getDefaultValueDisplayString(value, format);
    }

    @Override
    public DBDValueEditor createEditor(@NotNull final DBDValueController controller)
        throws DBException
    {
        switch (controller.getEditType()) {
            case PANEL:
                return new BaseValueEditor<Text>(controller) {
                    @Override
                    protected Text createControl(Composite editPlaceholder)
                    {
                        return new Text(valueController.getEditPlaceholder(),
                            SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.READ_ONLY);
                    }
                    @Override
                    public void primeEditorValue(@Nullable Object value) throws DBException
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