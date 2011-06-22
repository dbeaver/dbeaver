/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.exec.jdbc;

import org.jkiss.dbeaver.model.exec.DBCResultSet;

import java.sql.ResultSet;

/**
 * JDBC statement
 */
public interface JDBCResultSet extends ResultSet, DBCResultSet {

    JDBCPreparedStatement getSource();

    ResultSet getOriginal();

    void close();
}