package org.jkiss.dbeaver.model.dbc;

/**
 * DBCStatement
 */
public interface DBCStatement
{
    Object getNestedStatement();

    DBCSession getSession();

    void execute() throws DBCException;

    boolean hasResultSet() throws DBCException;

    DBCResultSet getResultSet() throws DBCException;

    int getUpdateCount() throws DBCException;

    void cancel() throws DBCException;

    void close() throws DBCException;

    void setFirstResult(int offset) throws DBCException;

    void setMaxResults(int limit) throws DBCException;
}
