/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.data;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCContentValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.SQLException;
import java.sql.SQLXML;

/**
 * XML type support
 */
public class OracleXMLValueHandler extends JDBCContentValueHandler {

    public static final OracleXMLValueHandler INSTANCE = new OracleXMLValueHandler();

    protected DBDContent getColumnValue(DBCExecutionContext context, JDBCResultSet resultSet, DBSTypedObject column, int columnIndex) throws DBCException, SQLException
    {
        Object object = null;
        boolean hasError = false;
        try {
            object = resultSet.getObject(columnIndex);
        } catch (SQLException e) {
            hasError = true;
        }
        if (hasError || !(object instanceof SQLXML)) {
            object = resultSet.getSQLXML(columnIndex);
        }
        if (object == null) {
            return createValueObject(context, column);
        } else if (object instanceof SQLXML) {
            return new OracleContentXML((SQLXML) object);
        } else {
            throw new DBCException("Unsupported object type: " + object.getClass().getName());
        }
    }

}
