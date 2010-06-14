/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.data;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Standard JDBC value handler
 */
public class JDBCUnsupportedValueHandler extends JDBCAbstractValueHandler {

    public static final JDBCUnsupportedValueHandler INSTANCE = new JDBCUnsupportedValueHandler();

    protected Object getColumnValue(ResultSet resultSet, DBSTypedObject columnType, int columnIndex)
        throws DBCException, SQLException
    {
        return resultSet.getObject(columnIndex);
    }

    protected void bindParameter(PreparedStatement statement, DBSTypedObject paramType, int paramIndex, Object value)
        throws DBCException, SQLException
    {
        throw new DBCException("Unsupported parameter type: " + paramType.getTypeName());
    }

    public boolean editValue(DBDValueController controller)
        throws DBException
    {
        if (!controller.isInlineEdit()) {
            controller.showMessage(
                "No suitable editor found for type '" + controller.getColumnMetaData().getTypeName() + "'", true);
        }
        return false;
    }

}
