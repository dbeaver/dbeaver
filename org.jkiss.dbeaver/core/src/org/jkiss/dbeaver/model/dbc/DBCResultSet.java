/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.dbc;

/**
 * DBCResultSet
 */
public interface DBCResultSet
{
    /**
     * ResultSet implementation object (usualy java.sql.ResultSet)
     * @return implementation
     */
    Object getNestedResultSet();

    DBCStatement getStatement();

    Object getObject(int index) throws DBCException;

    boolean wasNull() throws DBCException;

    boolean next() throws DBCException;

    String getCursorName() throws DBCException;

    DBCResultSetMetaData getMetaData() throws DBCException;

    void close();
}
