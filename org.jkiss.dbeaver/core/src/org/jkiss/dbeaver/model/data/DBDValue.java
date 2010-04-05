package org.jkiss.dbeaver.model.data;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.dbc.DBCResultSet;
import org.jkiss.dbeaver.model.dbc.DBCColumnMetaData;
import org.jkiss.dbeaver.model.dbc.DBCException;

/**
 * Data value container
 */
public class DBDValue {

    private DBPDataSource dataSource;
    private DBCResultSet resultSet;
    private int columnIndex;

    public DBDValue(DBPDataSource dataSource, DBCResultSet resultSet, int columnIndex)
    {
        this.dataSource = dataSource;
        this.resultSet = resultSet;
        this.columnIndex = columnIndex;
    }

    public DBPDataSource getDataSource() {
        return dataSource;
    }

    public DBCResultSet getResultSet() {
        return resultSet;
    }

    public DBCColumnMetaData getColumnMetaData()
        throws DBCException
    {
        return resultSet.getMetaData().getColumns().get(columnIndex);
    }

    public int getColumnIndex() {
        return columnIndex;
    }

    public void setColumnIndex(int columnIndex) {
        this.columnIndex = columnIndex;
    }

}
