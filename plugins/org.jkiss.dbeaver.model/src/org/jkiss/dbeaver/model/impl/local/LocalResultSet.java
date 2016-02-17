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

package org.jkiss.dbeaver.model.impl.local;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.data.DBDValueMeta;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.util.ArrayList;
import java.util.List;

/**
 * LocalResultSet
 */
public class LocalResultSet<SOURCE_STMT extends DBCStatement> implements DBCResultSet
{
    protected final DBCSession session;
    protected final SOURCE_STMT statement;
    private final List<DBCAttributeMetaData> metaColumns = new ArrayList<>();
    protected final List<Object[]> rows = new ArrayList<>();
    protected int curPosition = -1;

    public LocalResultSet(DBCSession session, SOURCE_STMT statement)
    {
        this.session = session;
        this.statement = statement;
    }

    @Override
    public DBCSession getSession()
    {
        return session;
    }

    @Override
    public SOURCE_STMT getSourceStatement()
    {
        return statement;
    }

    @Override
    public Object getAttributeValue(int index) throws DBCException
    {
        return rows.get(curPosition)[index];
    }

    @Nullable
    @Override
    public Object getAttributeValue(String name) throws DBCException {
        for (int i = 0; i < metaColumns.size(); i++) {
            if (metaColumns.get(i).getName().equals(name)) {
                return getAttributeValue(i);
            }
        }
        return null;
    }

    @Override
    public DBDValueMeta getAttributeValueMeta(int index) throws DBCException
    {
        return null;
    }

    @Override
    public DBDValueMeta getRowMeta() throws DBCException
    {
        return null;
    }

    @Override
    public boolean nextRow()
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

    @NotNull
    @Override
    public DBCResultSetMetaData getMeta() throws DBCException
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
    public String getResultSetName() throws DBCException {
        return null;
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
        LocalResultSetColumn column = new LocalResultSetColumn(this, metaColumns.size(), label, dataKind);
        metaColumns.add(column);
        return column;
    }

    public DBCAttributeMetaData addColumn(String label, DBSTypedObject typedObject)
    {
        LocalResultSetColumn column = new LocalResultSetColumn(this, metaColumns.size(), label, typedObject);
        metaColumns.add(column);
        return column;
    }

    public void addRow(Object ... values)
    {
        rows.add(values);
    }

}
