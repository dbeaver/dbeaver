/*
 * Copyright (C) 2010-2015 Serge Rieder
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
package org.jkiss.dbeaver.model.exec;

import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * Execution statistics
 */
public class DBCStatistics {

    private long rowsUpdated;
    private long rowsFetched;
    private long executeTime;
    private long fetchTime;
    private int statementsCount;
    private String queryText;
    private Map<String, Object> infoMap;
    private List<String> messages;

    public DBCStatistics()
    {
    }

    public long getRowsUpdated()
    {
        return rowsUpdated;
    }

    public void setRowsUpdated(long rowsUpdated)
    {
        this.rowsUpdated = rowsUpdated;
    }

    public void addRowsUpdated(long rowsUpdated)
    {
        this.rowsUpdated += rowsUpdated;
    }

    public long getRowsFetched()
    {
        return rowsFetched;
    }

    public void setRowsFetched(long rowsFetched)
    {
        this.rowsFetched = rowsFetched;
    }

    public long getExecuteTime()
    {
        return executeTime;
    }

    public void setExecuteTime(long executeTime)
    {
        this.executeTime = executeTime;
    }

    public void addExecuteTime(long executeTime)
    {
        this.executeTime += executeTime;
    }

    public long getFetchTime()
    {
        return fetchTime;
    }

    public void setFetchTime(long fetchTime) {
        this.fetchTime = fetchTime;
    }

    public void addFetchTime(long fetchTime) {
        this.fetchTime += fetchTime;
    }

    public long getTotalTime()
    {
        return executeTime + fetchTime;
    }

    public int getStatementsCount()
    {
        return statementsCount;
    }

    public void setStatementsCount(int statementsCount)
    {
        this.statementsCount = statementsCount;
    }

    public void addStatementsCount()
    {
        this.statementsCount++;
    }

    public String getQueryText() {
        return queryText;
    }

    public void setQueryText(String queryText) {
        this.queryText = queryText;
    }

    public List<String> getMessages()
    {
        return messages;
    }

    public void addMessage(String message)
    {
        if (messages == null) {
            messages = new ArrayList<String>();
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
            infoMap = new LinkedHashMap<String, Object>();
        }
        infoMap.put(name, value);
    }

    public boolean isEmpty()
    {
        return executeTime <= 0 && fetchTime <= 0;
    }

    public void accumulate(DBCStatistics stat)
    {
        rowsUpdated += stat.rowsUpdated;
        rowsFetched += stat.rowsFetched;
        executeTime += stat.executeTime;
        fetchTime += stat.fetchTime;
        statementsCount += stat.statementsCount;
        if (!CommonUtils.isEmpty(stat.messages)) {
            for (String message : stat.messages) {
                addMessage(message);
            }
        }
        if (!CommonUtils.isEmpty(stat.infoMap)) {
            for (Map.Entry<String,Object> info : stat.infoMap.entrySet()) {
                addInfo(info.getKey(), info.getValue());
            }
        }
    }

    public void reset()
    {
        rowsUpdated = 0;
        rowsFetched = 0;
        executeTime = 0;
        fetchTime = 0;
        statementsCount = 0;
        messages = null;
        infoMap = null;
    }

}