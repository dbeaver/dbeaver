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
package org.jkiss.dbeaver.model.exec;

import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * Execution statistics
 */
public class DBCStatistics {

    private final long startTime;
    private long rowsUpdated = -1;
    private long rowsFetched = -1;
    private long executeTime;
    private long fetchTime;
    private int statementsCount;
    private String queryText;
    private Map<String, Object> infoMap;
    private List<String> messages;

    public DBCStatistics() {
        this.startTime = System.currentTimeMillis();
    }

    public long getRowsUpdated() {
        return rowsUpdated;
    }

    public void setRowsUpdated(long rowsUpdated) {
        this.rowsUpdated = rowsUpdated;
    }

    public void addRowsUpdated(long rowsUpdated) {
        if (rowsUpdated < 0) {
            return;
        }
        if (this.rowsUpdated == -1) {
            this.rowsUpdated = 0;
        }
        this.rowsUpdated += rowsUpdated;
    }

    public long getRowsFetched() {
        return rowsFetched;
    }

    public void setRowsFetched(long rowsFetched) {
        this.rowsFetched = rowsFetched;
    }

    public long getExecuteTime() {
        return executeTime;
    }

    public void setExecuteTime(long executeTime) {
        this.executeTime = executeTime;
    }

    public void addExecuteTime(long executeTime) {
        this.executeTime += executeTime;
    }

    public void addExecuteTime() {
        this.executeTime += (System.currentTimeMillis() - startTime);
    }

    public long getFetchTime() {
        return fetchTime;
    }

    public void setFetchTime(long fetchTime) {
        this.fetchTime = fetchTime;
    }

    public void addFetchTime(long fetchTime) {
        this.fetchTime += fetchTime;
    }

    public long getTotalTime() {
        return executeTime + fetchTime;
    }

    public int getStatementsCount() {
        return statementsCount;
    }

    public void setStatementsCount(int statementsCount) {
        this.statementsCount = statementsCount;
    }

    public void addStatementsCount() {
        this.statementsCount++;
    }


    public String getQueryText() {
        return queryText;
    }

    public void setQueryText(String queryText) {
        this.queryText = queryText;
    }

    public List<String> getMessages() {
        return messages;
    }

    public void addMessage(String message) {
        if (messages == null) {
            messages = new ArrayList<>();
        }
        messages.add(message);
    }

    public Map<String, Object> getInfo() {
        if (infoMap == null) {
            return Collections.emptyMap();
        }
        return infoMap;
    }

    public void addInfo(String name, Object value) {
        if (infoMap == null) {
            infoMap = new LinkedHashMap<>();
        }
        infoMap.put(name, value);
    }

    public boolean isEmpty() {
        return executeTime <= 0 && fetchTime <= 0 && statementsCount == 0;
    }

    public void accumulate(DBCStatistics stat) {
        if (stat.rowsUpdated >= 0) {
            if (rowsUpdated < 0) rowsUpdated = 0;
            rowsUpdated += stat.rowsUpdated;
        }
        if (stat.rowsFetched > 0) {
            if (rowsFetched < 0) rowsFetched = 0;
            rowsFetched += stat.rowsFetched;
        }
        executeTime += stat.executeTime;
        fetchTime += stat.fetchTime;
        statementsCount += stat.statementsCount;
        if (!CommonUtils.isEmpty(stat.messages)) {
            for (String message : stat.messages) {
                addMessage(message);
            }
        }
        if (!CommonUtils.isEmpty(stat.infoMap)) {
            for (Map.Entry<String, Object> info : stat.infoMap.entrySet()) {
                addInfo(info.getKey(), info.getValue());
            }
        }
    }

    public void reset() {
        rowsUpdated = -1;
        rowsFetched = -1;
        executeTime = 0;
        fetchTime = 0;
        statementsCount = 0;
        messages = null;
        infoMap = null;
    }

}