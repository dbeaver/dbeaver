/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.sql;

/**
 * SQLQueryResult
 */
public class SQLQueryResult
{
    private SQLStatementInfo statement;
    //private DBCResultSetMetaData metaData;
    //private List<Object[]> rows;
    private Integer rowOffset;
    private Integer rowCount;
    private Integer updateCount;
    private Throwable error;
    private long queryTime;

    public SQLQueryResult(SQLStatementInfo statement)
    {
        this.statement = statement;
    }

    public SQLStatementInfo getStatement() {
        return statement;
    }

/*
    public DBCResultSetMetaData getResultSetMetaData()
    {
        return metaData;
    }

    public List<Object[]> getRows()
    {
        return rows;
    }

    public void setResultSet(DBCResultSetMetaData metaData, List<Object[]> rows)
    {
        this.metaData = metaData;
        this.rows = rows;
    }

*/

    public Integer getRowOffset() {
        return rowOffset;
    }

    public void setRowOffset(Integer rowOffset) {
        this.rowOffset = rowOffset;
    }

    public Integer getRowCount()
    {
        return rowCount;
    }

    public void setRowCount(Integer rowCount)
    {
        this.rowCount = rowCount;
    }

    public Integer getUpdateCount()
    {
        return updateCount;
    }

    public void setUpdateCount(Integer updateCount)
    {
        this.updateCount = updateCount;
    }

    public Throwable getError()
    {
        return error;
    }

    public void setError(Throwable error)
    {
        this.error = error;
    }

    public long getQueryTime()
    {
        return queryTime;
    }

    public void setQueryTime(long queryTime)
    {
        this.queryTime = queryTime;
    }
}
