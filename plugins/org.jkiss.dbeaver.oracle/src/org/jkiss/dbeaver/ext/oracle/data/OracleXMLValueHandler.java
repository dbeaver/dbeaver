package org.jkiss.dbeaver.ext.oracle.data;

import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCContentValueHandler;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCContentXML;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLXML;

/**
 * XML type support
 */
public class OracleXMLValueHandler extends JDBCContentValueHandler {

    public static final OracleXMLValueHandler INSTANCE = new OracleXMLValueHandler();

    protected DBDContent getColumnValue(DBCExecutionContext context, ResultSet resultSet, DBSTypedObject column, int columnIndex) throws DBCException, SQLException
    {
        SQLXML value = resultSet.getSQLXML(columnIndex);
        if (value == null) {
            return createValueObject(context, column);
        } else {
            return new JDBCContentXML(value);
        }
    }

}
