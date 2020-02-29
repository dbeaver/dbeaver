/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.model;

import org.jkiss.dbeaver.model.exec.DBCStatistics;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * DB value formatting utilities
 */
public final class DBFetchProgress {

    private DBRProgressMonitor monitor;
    private long startTime = System.currentTimeMillis();
    private long rowCount = 0;
    private long lastMonitor = 0;

    public DBFetchProgress(DBRProgressMonitor monitor) {
        this.monitor = monitor;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getRowCount() {
        return rowCount;
    }

    public void monitorRowFetch() {
        rowCount++;
        if (DBFetchProgress.monitorFetchProgress(rowCount)) {
            monitor.subTask(rowCount + ModelMessages.model_jdbc__rows_fetched);
            monitor.worked((int) (rowCount - lastMonitor));
            lastMonitor = rowCount;
        }
    }

    public void dumpStatistics(DBCStatistics statistics) {
        statistics.setFetchTime(System.currentTimeMillis() - startTime);
        statistics.setRowsFetched(rowCount);
        statistics.addStatementsCount();
    }

    public boolean isCanceled() {
        return monitor.isCanceled();
    }

    public boolean isMaxRowsFetched(long maxRows) {
        return maxRows > 0 && rowCount >= maxRows;
    }

    public static boolean monitorFetchProgress(long fetchedRows) {
        if (fetchedRows < 1000) {
            return fetchedRows % 100 == 0;
        } else if (fetchedRows < 100000) {
            return fetchedRows % 1000 == 0;
        } else {
            return fetchedRows % 10000 == 0;
        }
    }

}
