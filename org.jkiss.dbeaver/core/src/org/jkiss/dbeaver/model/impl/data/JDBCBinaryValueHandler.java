/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.data;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.model.dbc.DBCException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;

/**
 * JDBC binary value handler
 */
public class JDBCBinaryValueHandler extends JDBCAbstractValueHandler {

    public static final JDBCBinaryValueHandler INSTANCE = new JDBCBinaryValueHandler();

    protected Object getValueObject(ResultSet resultSet, DBSTypedObject columnType, int columnIndex)
        throws DBCException, SQLException
    {
        return "BINARY";
    }

    protected void bindParameter(PreparedStatement statement, DBSTypedObject paramType, int paramIndex, Object value)
        throws DBCException, SQLException
    {
    }

    public boolean editValue(final DBDValueController controller)
        throws DBException
    {
        if (controller.isInlineEdit()) {
            return false;
        } else {
            return false;
        }
    }

}