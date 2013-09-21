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

package org.jkiss.dbeaver.model.impl.local;

import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.data.DBDValueMeta;
import org.jkiss.dbeaver.model.exec.*;

import java.util.ArrayList;
import java.util.List;

/**
 * LocalResultSet
 */
public class LocalResultSet implements DBCResultSet
{
    private final DBCExecutionContext context;
    private final DBCStatement statement;
    private final List<DBCAttributeMetaData> metaColumns = new ArrayList<DBCAttributeMetaData>();
    private final List<Object[]> rows = new ArrayList<Object[]>();
    private int curPosition = -1;

    public LocalResultSet(DBCExecutionContext context, DBCStatement statement)
    {
        this.context = context;
        this.statement = statement;
    }

    @Override
    public DBCExecutionContext getContext()
    {
        return context;
    }

    @Override
    public DBCStatement getSource()
    {
        return statement;
    }

    @Override
    public Object getColumnValue(int index) throws DBCException
    {
        return rows.get(curPosition)[index - 1];
    }

    @Override
    public Object getColumnValue(String name) throws DBCException
    {
        for (int i = 0; i < metaColumns.size(); i++) {
            if (metaColumns.get(i).getName().equals(name)) {
                return getColumnValue(metaColumns.get(i).getIndex());
            }
        }
        return null;
    }

    @Override
    public DBDValueMeta getColumnValueMeta(int index) throws DBCException
    {
        return null;
    }

    @Override
    public DBDValueMeta getRowMeta() throws DBCException
    {
        return null;
    }

    @Override
    public boolean nextRow() throws DBCException
    {
        if (curPosition + 1 >= rows.size()) {
            return false;
        }
        curPosition++;
        return true;
    }

    @Override
    public boolean moveTo(int position) throws DBCException
    {
        if (position < 0 || position >= rows.size()) {
            return false;
        }
        curPosition = position;
        return true;
    }

    @Override
    public DBCResultSetMetaData getResultSetMetaData() throws DBCException
    {
        return new DBCResultSetMetaData() {
            @Override
            public List<DBCAttributeMetaData> getAttributes()
            {
                return metaColumns;
            }
        };
    }

    @Override
    public void close()
    {
        curPosition = -1;
        rows.clear();
        metaColumns.clear();
    }

    public DBCAttributeMetaData addColumn(String label, DBPDataKind dataKind)
    {
        LocalResultSetColumn column = new LocalResultSetColumn(this, metaColumns.size() + 1, label, dataKind);
        metaColumns.add(column);
        return column;
    }

    public void addRow(Object ... values)
    {
        rows.add(values);
    }

}
