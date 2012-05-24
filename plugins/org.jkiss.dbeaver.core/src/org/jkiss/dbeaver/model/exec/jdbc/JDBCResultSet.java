/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.exec.jdbc;

import org.jkiss.dbeaver.model.exec.DBCResultSet;

import java.sql.ResultSet;

/**
 * JDBC statement
 */
public interface JDBCResultSet extends ResultSet, DBCResultSet {

    @Override
    JDBCStatement getSource();

    ResultSet getOriginal();

    @Override
    void close();
}