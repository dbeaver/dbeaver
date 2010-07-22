package org.jkiss.dbeaver.model.impl.jdbc.struct;

/**
 * Scrollable query
 */
public interface JDBCScrollableTable {

    String makeScrollableQuery(String query, int offset, int maxRows);

}
