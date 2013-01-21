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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDCursor;
import org.jkiss.dbeaver.model.data.DBDValue;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.data.DBDValueEditor;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.dialogs.data.CursorViewDialog;
import org.jkiss.dbeaver.ui.properties.PropertySourceAbstract;
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
        return FEATURE_VIEWER;
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
            throw new DBCException("Can't copy object value " + object);
        }
        return object;
    }

    @Override
    public String getValueDisplayString(DBSTypedObject column, Object value)
    {
        if (value instanceof DBDValue) {
            return value.toString();
        }
        if (value instanceof RowId) {
            return CommonUtils.toHexString(((RowId) value).getBytes());
        }
        return DBUtils.getDefaultValueDisplayString(value);
    }

    @Override
    public void fillContextMenu(IMenuManager menuManager, final DBDValueController controller)
        throws DBCException
    {
    }

    @Override
    public void fillProperties(PropertySourceAbstract propertySource, DBDValueController controller)
    {
    }

    @Override
    public DBDValueEditor createEditor(final DBDValueController controller)
        throws DBException
    {
        switch (controller.getEditType()) {
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