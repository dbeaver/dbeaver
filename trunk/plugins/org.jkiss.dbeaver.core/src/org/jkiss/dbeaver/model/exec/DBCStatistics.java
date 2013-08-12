package org.jkiss.dbeaver.model.exec;

import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Execution statistics
 */
public class DBCStatistics {

    private long rowsUpdated;
    private long rowsFetched;
    private long executeTime;
    private long fetchTime;
    private List<String> messages;

    public long getRowsUpdated()
    {
        return rowsUpdated;
    }

    public void setRowsUpdated(long rowsUpdated)
    {
        this.rowsUpdated = rowsUpdated;
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

    public long getFetchTime()
    {
        return fetchTime;
    }

    public void setFetchTime(long fetchTime)
    {
        this.fetchTime = fetchTime;
    }

    public long getTotalTime()
    {
        return executeTime + fetchTime;
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
        if (!CommonUtils.isEmpty(stat.messages)) {
            for (String message : stat.messages) {
                addMessage(message);
            }
        }
    }

    public void reset()
    {
        rowsUpdated = 0;
        rowsFetched = 0;
        executeTime = 0;
        fetchTime = 0;
        messages = null;
    }

}