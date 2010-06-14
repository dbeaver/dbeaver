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

    DBCStatement getSource();

    Object getColumnValue(int index) throws DBCException;

    boolean nextRow() throws DBCException;

    DBCResultSetMetaData getResultSetMetaData() throws DBCException;

    void close();
}
