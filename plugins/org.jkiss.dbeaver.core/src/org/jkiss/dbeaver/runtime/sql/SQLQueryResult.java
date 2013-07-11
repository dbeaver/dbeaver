/*
 * Copyright (C) 2010-2013 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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
    private boolean hasResultSet;
    private Throwable error;
    private long queryTime;
    private String sourceEntity;

    public SQLQueryResult(SQLStatementInfo statement)
    {
        this.statement = statement;
    }

    public SQLStatementInfo getStatement() {
        return statement;
    }

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

    public boolean hasResultSet()
    {
        return hasResultSet;
    }

    public void setHasResultSet(boolean hasResultSet)
    {
        this.hasResultSet = hasResultSet;
    }

    public boolean hasError()
    {
        return error != null;
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

    public String getSourceEntity()
    {
        return sourceEntity;
    }

    public void setSourceEntity(String sourceEntity)
    {
        this.sourceEntity = sourceEntity;
    }
}
