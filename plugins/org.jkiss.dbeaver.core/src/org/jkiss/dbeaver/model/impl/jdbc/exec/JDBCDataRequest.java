package org.jkiss.dbeaver.model.impl.jdbc.exec;

import org.jkiss.dbeaver.model.data.DBDAttributeValue;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.exec.DBCDataRequest;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;

import java.util.List;

/**
 * JDBC data request
 */
public abstract class JDBCDataRequest implements DBCDataRequest {
    protected DBDDataReceiver dataReceiver;
    protected DBDDataFilter dataFilter;
    protected long firstRow;
    protected long maxRows;
    protected List<DBDAttributeValue> keys;
    protected List<DBDAttributeValue> data;
    protected long result;
    protected StringBuilder script = new StringBuilder();

    @Override
    public void setDataReceiver(DBDDataReceiver dataReceiver)
    {
        this.dataReceiver = dataReceiver;
    }

    @Override
    public void setDataFilter(DBDDataFilter dataFilter)
    {
        this.dataFilter = dataFilter;
    }

    @Override
    public void setLimit(long firstRow, long maxRows)
    {
        this.firstRow = firstRow;
        this.maxRows = maxRows;
    }

    @Override
    public void setKeys(List<DBDAttributeValue> attributes)
    {
        this.keys = attributes;
    }

    @Override
    public void setData(List<DBDAttributeValue> attributes)
    {
        this.data = attributes;
    }

    @Override
    public long getResult()
    {
        return this.result;
    }

    @Override
    public String generateScript(DBCExecutionContext context)
    {
        return this.script.toString();
    }
}
