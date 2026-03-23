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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatistics;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ExecuteStatementRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DynamoDBExecuteBatch implements DBSDataManipulator.ExecuteBatch {

    private static final Log log = Log.getLog(DynamoDBExecuteBatch.class);

    public enum OperationType {
        INSERT, UPDATE, DELETE
    }

    private final DBCSession session;
    private final DynamoDBTable table;
    private final DBSAttributeBase[] attributes;
    private final DBSAttributeBase[] keyAttributes;
    private final OperationType operationType;
    private final List<Object[]> rows = new ArrayList<>();

    public DynamoDBExecuteBatch(
            @NotNull DBCSession session,
            @NotNull DynamoDBTable table,
            @NotNull DBSAttributeBase[] attributes,
            @NotNull OperationType operationType) {
        this(session, table, attributes, null, operationType);
    }

    public DynamoDBExecuteBatch(
            @NotNull DBCSession session,
            @NotNull DynamoDBTable table,
            @NotNull DBSAttributeBase[] attributes,
            @Nullable DBSAttributeBase[] keyAttributes,
            @NotNull OperationType operationType) {
        this.session = session;
        this.table = table;
        this.attributes = attributes;
        this.keyAttributes = keyAttributes;
        this.operationType = operationType;
    }

    @NotNull
    @Override
    public DBSDataManipulator.ExecuteBatch add(@NotNull Object[] attributeValues) throws DBCException {
        rows.add(attributeValues.clone());
        return this;
    }

    @NotNull
    @Override
    public DBCStatistics execute(@NotNull DBCSession session, @NotNull Map<String, Object> options) throws DBException {
        DBCStatistics stats = new DBCStatistics();
        DynamoDbClient client = ((DynamoDBSession) session).getDynamoClient();
        if (client == null) throw new DBCException("DynamoDB client is not connected");

        for (Object[] row : rows) {
            switch (operationType) {
                case INSERT:
                    executeInsert(client, row);
                    break;
                case UPDATE:
                    executeUpdate(client, row);
                    break;
                case DELETE:
                    executeDelete(client, row);
                    break;
            }
            stats.addRowsUpdated(1);
        }
        stats.addStatementsCount();
        return stats;
    }

    private void executeInsert(DynamoDbClient client, Object[] values) throws DBCException {
        Map<String, AttributeValue> item = new HashMap<>();
        for (int i = 0; i < attributes.length && i < values.length; i++) {
            if (values[i] != null) {
                item.put(attributes[i].getName(), toAttributeValue(values[i]));
            }
        }
        try {
            client.putItem(PutItemRequest.builder()
                    .tableName(table.getName())
                    .item(item)
                    .build());
        } catch (Exception e) {
            throw new DBCException("Error inserting item: " + e.getMessage(), e);
        }
    }

    private void executeUpdate(DynamoDbClient client, Object[] values) throws DBCException {
        StringBuilder partiQL = new StringBuilder("UPDATE \"")
                .append(table.getName()).append("\" SET ");
        List<AttributeValue> params = new ArrayList<>();

        // SET clauses for updated attributes
        boolean first = true;
        for (int i = 0; i < attributes.length && i < values.length; i++) {
            if (!first) partiQL.append(", ");
            partiQL.append("\"").append(attributes[i].getName()).append("\" = ?");
            params.add(toAttributeValue(values[i]));
            first = false;
        }

        // WHERE clause from key attributes
        if (keyAttributes != null && keyAttributes.length > 0) {
            partiQL.append(" WHERE ");
            int keyOffset = attributes.length;
            for (int i = 0; i < keyAttributes.length; i++) {
                if (i > 0) partiQL.append(" AND ");
                int valueIdx = keyOffset + i;
                partiQL.append("\"").append(keyAttributes[i].getName()).append("\" = ?");
                params.add(toAttributeValue(valueIdx < values.length ? values[valueIdx] : null));
            }
        }

        String sql = partiQL.toString();
        log.debug("DynamoDB UPDATE: " + sql);

        try {
            client.executeStatement(ExecuteStatementRequest.builder()
                    .statement(sql)
                    .parameters(params)
                    .build());
        } catch (Exception e) {
            throw new DBCException("Error updating item: " + e.getMessage(), e);
        }
    }

    private void executeDelete(DynamoDbClient client, Object[] values) throws DBCException {
        // Use PartiQL DELETE
        StringBuilder partiQL = new StringBuilder("DELETE FROM \"")
                .append(table.getName()).append("\" WHERE ");
        List<AttributeValue> params = new ArrayList<>();

        for (int i = 0; i < attributes.length && i < values.length; i++) {
            if (i > 0) partiQL.append(" AND ");
            partiQL.append("\"").append(attributes[i].getName()).append("\" = ?");
            params.add(toAttributeValue(values[i]));
        }

        String sql = partiQL.toString();
        log.debug("DynamoDB DELETE: " + sql);

        try {
            client.executeStatement(ExecuteStatementRequest.builder()
                    .statement(sql)
                    .parameters(params)
                    .build());
        } catch (Exception e) {
            throw new DBCException("Error deleting item: " + e.getMessage(), e);
        }
    }

    @NotNull
    private AttributeValue toAttributeValue(Object value) {
        if (value == null) return AttributeValue.fromNul(true);
        if (value instanceof String s) {
            if (s.isEmpty()) return AttributeValue.fromNul(true);
            return AttributeValue.fromS(s);
        }
        if (value instanceof Number n) return AttributeValue.fromN(n.toString());
        if (value instanceof Boolean b) return AttributeValue.fromBool(b);
        if (value instanceof byte[] bytes) return AttributeValue.fromB(software.amazon.awssdk.core.SdkBytes.fromByteArray(bytes));
        return AttributeValue.fromS(value.toString());
    }

    @Override
    public void generatePersistActions(
            @NotNull DBCSession session,
            @NotNull List<DBEPersistAction> actions,
            @NotNull Map<String, Object> options) throws DBException {
    }

    @Override
    public void close() {
        rows.clear();
    }
}
