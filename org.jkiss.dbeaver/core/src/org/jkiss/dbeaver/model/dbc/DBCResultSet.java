package org.jkiss.dbeaver.model.dbc;

/**
 * DBCResultSet
 */
public interface DBCResultSet
{
    DBCStatement getStatement();

    Object getObject(int index) throws DBCException;

    boolean wasNull() throws DBCException;

    boolean next() throws DBCException;

    String getCursorName() throws DBCException;

    DBCResultSetMetaData getMetaData() throws DBCException;

    void close() throws DBCException;
}
