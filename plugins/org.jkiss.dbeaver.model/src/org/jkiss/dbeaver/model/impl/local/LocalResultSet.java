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

package org.jkiss.dbeaver.model.impl.local;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.AbstractResultSet;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.util.ArrayList;
import java.util.List;

/**
 * LocalResultSet
 */
public class LocalResultSet<SOURCE_STMT extends DBCStatement> extends AbstractResultSet<DBCSession, SOURCE_STMT> {

    private final List<DBCAttributeMetaData> metaColumns = new ArrayList<>();
    protected final List<Object[]> rows = new ArrayList<>();
    protected int curPosition = -1;

    public LocalResultSet(DBCSession session, SOURCE_STMT statement) {
        super(session, statement);
    }

    @Override
    public Object getAttributeValue(int index) throws DBCException {
        Object[] row = rows.get(curPosition);
        if (index >= row.length) {
            throw new DBCException("Attribute index out of range (" + index + "/" + row.length + ")");
        }
        return row[index];
    }

    @Nullable
    @Override
    public Object getAttributeValue(String name) throws DBCException {
        for (int i = 0; i < metaColumns.size(); i++) {
            if (metaColumns.get(i).getName().equals(name)) {
                return getAttributeValue(i);
            }
        }
        throw new DBCException("Bad attribute name: " + name);
    }

    @Override
    public boolean nextRow() {
        if (curPosition + 1 >= rows.size()) {
            return false;
        }
        curPosition++;
        return true;
    }

    @Override
    public boolean moveTo(int position) throws DBCException {
        if (position < 0 || position >= rows.size()) {
            return false;
        }
        curPosition = position;
        return true;
    }

    @NotNull
    @Override
    public DBCResultSetMetaData getMeta() throws DBCException {
        return new LocalResultSetMeta(metaColumns);
    }

    @Override
    public Object getFeature(String name) {
        if (name.equals(FEATURE_NAME_LOCAL)) {
            return true;
        }
        return super.getFeature(name);
    }

    @Override
    public void close() {
        curPosition = -1;
        rows.clear();
        metaColumns.clear();
    }

    public int getColumnCount() {
        return metaColumns.size();
    }

    public DBCAttributeMetaData addColumn(String label, DBPDataKind dataKind) {
        LocalResultSetColumn column = new LocalResultSetColumn(this, metaColumns.size(), label, dataKind);
        metaColumns.add(column);
        return column;
    }

    public DBCAttributeMetaData addColumn(String label, DBSTypedObject typedObject) {
        LocalResultSetColumn column = new LocalResultSetColumn(this, metaColumns.size(), label, typedObject);
        metaColumns.add(column);
        return column;
    }

    public void addRow(Object... values) {
        rows.add(values);
    }

}
