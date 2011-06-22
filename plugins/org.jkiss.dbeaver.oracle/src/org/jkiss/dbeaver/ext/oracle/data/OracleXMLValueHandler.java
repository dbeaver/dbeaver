package org.jkiss.dbeaver.ext.oracle.data;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCAbstractValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLXML;

/**
 * XML type support
 */
public class OracleXMLValueHandler extends JDBCAbstractValueHandler {

    public static final OracleXMLValueHandler INSTANCE = new OracleXMLValueHandler();

    @Override
    public String getValueDisplayString(DBSTypedObject column, Object value)
    {
        return value == null ? DBConstants.NULL_VALUE_LABEL : "[XML]";
    }

    @Override
    protected Object getColumnValue(DBCExecutionContext context, ResultSet resultSet, DBSTypedObject column, int columnIndex) throws DBCException, SQLException
    {
        return resultSet.getSQLXML(columnIndex);
    }

    @Override
    protected void bindParameter(DBCExecutionContext context, PreparedStatement statement, DBSTypedObject paramType, int paramIndex, Object value)
        throws DBCException, SQLException
    {
        statement.setSQLXML(paramIndex, (SQLXML) value);
    }

    public Class getValueObjectType()
    {
        return SQLXML.class;
    }

    public Object copyValueObject(DBCExecutionContext context, DBSTypedObject column, Object value) throws DBCException
    {
        return null;
    }

    public boolean editValue(DBDValueController controller) throws DBException
    {
        return false;
    }

}
