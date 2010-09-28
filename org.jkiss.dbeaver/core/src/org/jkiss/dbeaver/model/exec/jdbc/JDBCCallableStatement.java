/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.exec.jdbc;

import java.sql.CallableStatement;

/**
 * JDBC statement
 */
public interface JDBCCallableStatement extends CallableStatement, JDBCStatement {

    void close();

}