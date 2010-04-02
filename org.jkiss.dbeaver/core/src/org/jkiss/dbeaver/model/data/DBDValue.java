package org.jkiss.dbeaver.model.data;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.dbc.DBCResultSet;

/**
 * Data value container
 */
public class DBDValue {

    private DBPDataSource dataSource;
    private DBCResultSet resultSet;
    private int columnIndex;
    private Object columnValue;
    private DBSDataType dataType;

    public DBDValue(DBPDataSource dataSource, DBCResultSet resultSet)
    {
        this.dataSource = dataSource;
        this.resultSet = resultSet;
    }

    public DBPDataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DBPDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public DBCResultSet getResultSet() {
        return resultSet;
    }

    public void setResultSet(DBCResultSet resultSet) {
        this.resultSet = resultSet;
    }

    public int getColumnIndex() {
        return columnIndex;
    }

    public void setColumnIndex(int columnIndex) {
        this.columnIndex = columnIndex;
    }

    public Object getColumnValue() {
        return columnValue;
    }

    public void setColumnValue(Object columnValue) {
        this.columnValue = columnValue;
    }

    public DBSDataType getDataType() {
        return dataType;
    }

    public void setDataType(DBSDataType dataType) {
        this.dataType = dataType;
    }
}
