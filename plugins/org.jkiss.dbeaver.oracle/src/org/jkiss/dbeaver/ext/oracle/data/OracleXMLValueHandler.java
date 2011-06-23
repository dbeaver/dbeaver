/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.data;

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
        //final Object object = resultSet.getObject(columnIndex);
        SQLXML xml = resultSet.getSQLXML(columnIndex);
        if (xml == null) {
            return createValueObject(context, column);
        } else {
            return new OracleContentXML(xml);
        }
    }

}
