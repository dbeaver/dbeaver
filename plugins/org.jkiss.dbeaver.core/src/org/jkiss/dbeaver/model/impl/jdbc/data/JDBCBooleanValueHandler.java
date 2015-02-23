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
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.data.DBDValueEditor;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.data.editors.BooleanInlineEditor;
import org.jkiss.dbeaver.model.impl.data.editors.BooleanPanelEditor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.dialogs.data.DefaultValueViewDialog;

import java.sql.SQLException;

/**
 * JDBC number value handler
 */
public class JDBCBooleanValueHandler extends JDBCAbstractValueHandler {

    public static final JDBCBooleanValueHandler INSTANCE = new JDBCBooleanValueHandler();

    static final Log log = Log.getLog(JDBCBooleanValueHandler.class);

    @Override
    protected Object fetchColumnValue(DBCSession session, JDBCResultSet resultSet, DBSTypedObject type, int index)
        throws SQLException
    {
        boolean value = resultSet.getBoolean(index);
        return resultSet.wasNull() ? null : value;
    }

    @Override
    protected void bindParameter(JDBCSession session, JDBCPreparedStatement statement, DBSTypedObject paramType,
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
    public Object getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object, boolean copy) throws DBCException
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
    public DBDValueEditor createEditor(@NotNull DBDValueController controller)
        throws DBException
    {
        switch (controller.getEditType()) {
            case INLINE:
                return new BooleanInlineEditor(controller);
            case PANEL:
                return new BooleanPanelEditor(controller);
            case EDITOR:
                return new DefaultValueViewDialog(controller);
            default:
                return null;
        }
    }

}