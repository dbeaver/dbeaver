package org.jkiss.dbeaver.model.dbc;

import java.util.List;

/**
 * DBCResultSetMetaData
 */
public interface DBCResultSetMetaData
{
    DBCResultSet getResultSet();

    List<DBCColumnMetaData> getColumns();
}
