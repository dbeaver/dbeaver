/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jkiss.dbeaver.model.sql;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

/**
 * SQLQueryResult
 */
public class SQLQueryResult
{
    private SQLQuery statement;
    //private DBCResultSetMetaData metaData;
    //private List<Object[]> rows;
    private Long rowOffset;
    private Long rowCount;
    private Long updateCount;
    private boolean hasResultSet;
    private Throwable error;
    private long queryTime;
    private String resultSetName;
    private Throwable[] warnings;

    public SQLQueryResult(@NotNull SQLQuery statement)
    {
        this.statement = statement;
    }

    @NotNull
    public SQLQuery getStatement() {
        return statement;
    }

    public Long getRowOffset() {
        return rowOffset;
    }

    public void setRowOffset(Long rowOffset) {
        this.rowOffset = rowOffset;
    }

    @Nullable
    public Long getRowCount()
    {
        return rowCount;
    }

    public void setRowCount(Long rowCount)
    {
        this.rowCount = rowCount;
    }

    @Nullable
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

    @Nullable
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

    @Nullable
    public String getResultSetName()
    {
        return resultSetName;
    }

    public void setResultSetName(String resultSetName)
    {
        this.resultSetName = resultSetName;
    }

    public Throwable[] getWarnings() {
        return warnings;
    }

    public void setWarnings(Throwable[] warnings) {
        this.warnings = warnings;
    }
}
