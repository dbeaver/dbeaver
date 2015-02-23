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
package org.jkiss.dbeaver.model.impl.jdbc.exec;

import org.jkiss.dbeaver.model.data.DBDAttributeValue;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.exec.DBCDataRequest;
import org.jkiss.dbeaver.model.exec.DBCSession;

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
    public String generateScript(DBCSession session)
    {
        return this.script.toString();
    }
}
