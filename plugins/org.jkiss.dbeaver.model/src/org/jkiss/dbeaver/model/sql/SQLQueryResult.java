/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jkiss.dbeaver.model.sql;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.DBCExecutionResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SQLQueryResult
 */
public class SQLQueryResult implements DBCExecutionResult
{
    public static class ExecuteResult {
        private boolean resultSet;
        private Long rowCount;
        private Long updateCount;
        private String resultSetName;

        public ExecuteResult(boolean resultSet) {
            this.resultSet = resultSet;
        }

        public boolean isResultSet() {
            return resultSet;
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

        @Nullable
        public String getResultSetName()
        {
            return resultSetName;
        }

        public void setResultSetName(String resultSetName)
        {
            this.resultSetName = resultSetName;
        }
    }

    private SQLQuery statement;
    private Long rowOffset;
    private boolean hasResultSet;
    private Throwable error;
    private long queryTime;
    private List<Throwable> warnings;
    private List<ExecuteResult> executeResults = new ArrayList<>();

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

    public List<Throwable> getWarnings() {
        return warnings;
    }

    public void addWarnings(Throwable[] warnings) {
        if (warnings == null) {
            return;
        }
        if (this.warnings == null) {
            this.warnings = new ArrayList<>();
        }
        Collections.addAll(this.warnings, warnings);
    }

    public ExecuteResult addExecuteResult(boolean resultSet) {
        ExecuteResult executeResult = new ExecuteResult(resultSet);
        executeResults.add(executeResult);
        return executeResult;
    }

    public List<ExecuteResult> getExecuteResults() {
        return executeResults;
    }

    public ExecuteResult getExecuteResults(int order, boolean resultSets) {
        int rsIndex = -1;
        for (int i = 0; i < executeResults.size(); i++) {
            if (resultSets && !executeResults.get(i).isResultSet()) {
                continue;
            }
            rsIndex++;
            if (rsIndex == order) {
                return executeResults.get(i);
            }
        }
        return null;
    }

}
