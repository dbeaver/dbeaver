/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.struct;

/**
 * Scrollable query
 */
public interface JDBCScrollableTable {

    String makeScrollableQuery(String query, int offset, int maxRows);

}
