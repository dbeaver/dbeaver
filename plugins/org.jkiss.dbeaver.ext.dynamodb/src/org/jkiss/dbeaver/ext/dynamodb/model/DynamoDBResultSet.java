/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.dynamodb.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.data.DBDValueMeta;
import org.jkiss.dbeaver.model.exec.DBCAttributeMetaData;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCResultSetMetaData;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;

public class DynamoDBResultSet implements DBCResultSet {

    private final DynamoDBSession session;
    private final DynamoDBStatement statement;
    private final List<Map<String, AttributeValue>> items;
    private final DynamoDBResultSetMetaData metaData;
    private int currentRow = -1;
    private Object[] currentRowValues;

    public DynamoDBResultSet(
            @NotNull DynamoDBSession session,
            @NotNull DynamoDBStatement statement,
            @NotNull List<Map<String, AttributeValue>> items) {
        this.session = session;
        this.statement = statement;
        this.items = items;
        this.metaData = DynamoDBSchemaInference.inferMetaData(items);
    }

    @Override
    public boolean nextRow() throws DBCException {
        currentRow++;
        if (currentRow >= items.size()) {
            return false;
        }
        Map<String, AttributeValue> item = items.get(currentRow);
        List<? extends DBCAttributeMetaData> cols = metaData.getAttributes();
        currentRowValues = new Object[cols.size()];
        for (int i = 0; i < cols.size(); i++) {
            String colName = cols.get(i).getName();
            AttributeValue av = item.get(colName);
            currentRowValues[i] = DynamoDBTypeMapper.toJavaValue(av);
        }
        return true;
    }

    @Nullable
    @Override
    public Object getAttributeValue(int index) throws DBCException {
        if (currentRowValues == null || index < 0 || index >= currentRowValues.length) {
            return null;
        }
        return currentRowValues[index];
    }

    @Nullable
    @Override
    public Object getAttributeValue(String name) throws DBCException {
        List<? extends DBCAttributeMetaData> cols = metaData.getAttributes();
        for (int i = 0; i < cols.size(); i++) {
            if (cols.get(i).getName().equals(name)) {
                return getAttributeValue(i);
            }
        }
        return null;
    }

    @Nullable
    @Override
    public DBDValueMeta getAttributeValueMeta(int index) throws DBCException {
        return null;
    }

    @Nullable
    @Override
    public DBDValueMeta getRowMeta() throws DBCException {
        return null;
    }

    @Override
    public boolean moveTo(int position) throws DBCException {
        return false;
    }

    @NotNull
    @Override
    public DBCResultSetMetaData getMeta() throws DBCException {
        return metaData;
    }

    @Nullable
    @Override
    public String getResultSetName() throws DBCException {
        return null;
    }

    @Nullable
    @Override
    public Object getFeature(String name) {
        return null;
    }

    @NotNull
    @Override
    public DBCSession getSession() {
        return session;
    }

    @NotNull
    @Override
    public DBCStatement getSourceStatement() {
        return statement;
    }

    @Override
    public void close() {
    }
}
