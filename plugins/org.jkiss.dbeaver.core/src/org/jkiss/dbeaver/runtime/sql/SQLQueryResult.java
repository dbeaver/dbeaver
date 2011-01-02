/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
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
    private Long rowOffset;
    private Long rowCount;
    private Long updateCount;
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

    public Long getRowOffset() {
        return rowOffset;
    }

    public void setRowOffset(Long rowOffset) {
        this.rowOffset = rowOffset;
    }

    public Long getRowCount()
    {
        return rowCount;
    }

    public void setRowCount(Long rowCount)
    {
        this.rowCount = rowCount;
    }

    public Long getUpdateCount()
    {
        return updateCount;
    }

    public void setUpdateCount(Long updateCount)
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
