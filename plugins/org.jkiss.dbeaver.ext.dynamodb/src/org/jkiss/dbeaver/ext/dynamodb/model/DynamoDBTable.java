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
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionSource;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatistics;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraint;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.ExecuteStatementRequest;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndexDescription;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.LocalSecondaryIndexDescription;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DynamoDBTable implements DBSEntity, DBSDataManipulator, DBPRefreshableObject {

    private static final Log log = Log.getLog(DynamoDBTable.class);

    private final DynamoDBDataSource dataSource;
    private final String name;
    private List<DynamoDBAttribute> attributes;
    private List<DynamoDBIndex> indexes;
    private long itemCount;
    private long tableSizeBytes;
    private String tableStatus;
    private boolean metadataLoaded;

    public DynamoDBTable(@NotNull DynamoDBDataSource dataSource, @NotNull String name) {
        this.dataSource = dataSource;
        this.name = name;
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @Nullable
    @Override
    public String getDescription() {
        return tableStatus != null ? "Status: " + tableStatus + ", Items: " + itemCount : null;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @NotNull
    @Override
    public DBSObject getParentObject() {
        return dataSource;
    }

    @NotNull
    @Override
    public DynamoDBDataSource getDataSource() {
        return dataSource;
    }

    @NotNull
    @Override
    public DBSEntityType getEntityType() {
        return DBSEntityType.TABLE;
    }

    @Nullable
    @Override
    public List<? extends DBSEntityAttribute> getAttributes(@NotNull DBRProgressMonitor monitor) throws DBException {
        ensureMetadataLoaded(monitor);
        return attributes;
    }

    @Nullable
    @Override
    public DBSEntityAttribute getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName) throws DBException {
        ensureMetadataLoaded(monitor);
        if (attributes == null) return null;
        for (DynamoDBAttribute attr : attributes) {
            if (attr.getName().equalsIgnoreCase(attributeName)) {
                return attr;
            }
        }
        return null;
    }

    @Nullable
    @Override
    public Collection<? extends DBSEntityConstraint> getConstraints(@NotNull DBRProgressMonitor monitor) throws DBException {
        ensureMetadataLoaded(monitor);
        if (attributes == null || attributes.isEmpty()) return null;
        List<DynamoDBAttribute> keys = new ArrayList<>();
        for (DynamoDBAttribute attr : attributes) {
            if (attr.isKeyAttribute()) {
                keys.add(attr);
            }
        }
        if (keys.isEmpty()) return null;
        return Collections.singletonList(new DynamoDBKeyConstraint(this, keys));
    }

    @Nullable
    @Override
    public Collection<? extends DBSEntityAssociation> getAssociations(@NotNull DBRProgressMonitor monitor) throws DBException {
        return null;
    }

    @Nullable
    @Override
    public Collection<? extends DBSEntityAssociation> getReferences(@NotNull DBRProgressMonitor monitor) throws DBException {
        return null;
    }

    @NotNull
    public List<DynamoDBIndex> getIndexes(@NotNull DBRProgressMonitor monitor) throws DBException {
        ensureMetadataLoaded(monitor);
        return indexes != null ? indexes : Collections.emptyList();
    }

    private void ensureMetadataLoaded(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (metadataLoaded) return;
        DynamoDbClient client = dataSource.getDynamoClient();
        if (client == null) throw new DBException("DynamoDB client is not connected");

        try {
            DescribeTableResponse resp = client.describeTable(b -> b.tableName(name));
            TableDescription desc = resp.table();
            this.itemCount = desc.itemCount() != null ? desc.itemCount() : 0;
            this.tableSizeBytes = desc.tableSizeBytes() != null ? desc.tableSizeBytes() : 0;
            this.tableStatus = desc.tableStatusAsString();

            Map<String, String> attrTypes = new HashMap<>();
            if (desc.attributeDefinitions() != null) {
                for (AttributeDefinition ad : desc.attributeDefinitions()) {
                    attrTypes.put(ad.attributeName(), ad.attributeTypeAsString());
                }
            }

            List<DynamoDBAttribute> attrs = new ArrayList<>();
            int ordinal = 0;
            if (desc.keySchema() != null) {
                for (KeySchemaElement kse : desc.keySchema()) {
                    String attrName = kse.attributeName();
                    String attrType = attrTypes.getOrDefault(attrName, "S");
                    boolean isPartition = kse.keyType() == KeyType.HASH;
                    attrs.add(new DynamoDBAttribute(this, attrName, attrType, ordinal++, true, isPartition));
                }
            }
            this.attributes = attrs;

            // DynamoDB is schemaless — scan a sample to discover non-key attributes
            try {
                ScanResponse sampleResp = client.scan(ScanRequest.builder()
                        .tableName(name).limit(50).build());
                if (sampleResp.items() != null) {
                    // Collect all unique attribute names from sample
                    Map<String, String> discoveredAttrs = new HashMap<>();
                    for (Map<String, AttributeValue> item : sampleResp.items()) {
                        for (Map.Entry<String, AttributeValue> entry : item.entrySet()) {
                            if (!discoveredAttrs.containsKey(entry.getKey())) {
                                discoveredAttrs.put(entry.getKey(), DynamoDBTypeMapper.inferDynamoType(entry.getValue()));
                            }
                        }
                    }
                    // Add non-key attributes that aren't already in the list
                    for (Map.Entry<String, String> entry : discoveredAttrs.entrySet()) {
                        boolean exists = false;
                        for (DynamoDBAttribute existing : attrs) {
                            if (existing.getName().equals(entry.getKey())) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) {
                            attrs.add(new DynamoDBAttribute(this, entry.getKey(), entry.getValue(), ordinal++, false, false));
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("Could not scan sample for schema inference: " + e.getMessage());
            }

            List<DynamoDBIndex> idxList = new ArrayList<>();
            if (desc.globalSecondaryIndexes() != null) {
                for (GlobalSecondaryIndexDescription gsi : desc.globalSecondaryIndexes()) {
                    String pk = null, sk = null;
                    if (gsi.keySchema() != null) {
                        for (KeySchemaElement kse : gsi.keySchema()) {
                            if (kse.keyType() == KeyType.HASH) pk = kse.attributeName();
                            else sk = kse.attributeName();
                        }
                    }
                    idxList.add(new DynamoDBIndex(this, gsi.indexName(), "GSI", pk, sk,
                            gsi.projection() != null ? gsi.projection().projectionTypeAsString() : null));
                }
            }
            if (desc.localSecondaryIndexes() != null) {
                for (LocalSecondaryIndexDescription lsi : desc.localSecondaryIndexes()) {
                    String pk = null, sk = null;
                    if (lsi.keySchema() != null) {
                        for (KeySchemaElement kse : lsi.keySchema()) {
                            if (kse.keyType() == KeyType.HASH) pk = kse.attributeName();
                            else sk = kse.attributeName();
                        }
                    }
                    idxList.add(new DynamoDBIndex(this, lsi.indexName(), "LSI", pk, sk,
                            lsi.projection() != null ? lsi.projection().projectionTypeAsString() : null));
                }
            }
            this.indexes = idxList;
            this.metadataLoaded = true;
        } catch (Exception e) {
            throw new DBException("Error loading DynamoDB table metadata: " + e.getMessage(), e);
        }
    }

    @NotNull
    @Override
    public String[] getSupportedFeatures() {
        return new String[]{
                FEATURE_DATA_SELECT, FEATURE_DATA_COUNT, FEATURE_DATA_FILTER,
                FEATURE_DATA_INSERT, FEATURE_DATA_UPDATE, FEATURE_DATA_DELETE
        };
    }

    @NotNull
    @Override
    public DBCStatistics readData(
            @Nullable DBCExecutionSource source,
            @NotNull DBCSession session,
            @NotNull DBDDataReceiver dataReceiver,
            @Nullable DBDDataFilter dataFilter,
            long firstRow,
            long maxRows,
            long flags,
            int fetchSize) throws DBException {
        DBCStatistics statistics = new DBCStatistics();
        statistics.setQueryText("SCAN " + name);

        DynamoDbClient client = ((DynamoDBSession) session).getDynamoClient();
        if (client == null) throw new DBCException("DynamoDB client is not connected");

        ScanRequest.Builder reqBuilder = ScanRequest.builder().tableName(name);
        if (maxRows > 0 && maxRows < Integer.MAX_VALUE) {
            reqBuilder.limit((int) maxRows);
        }

        try {
            ScanResponse resp = client.scan(reqBuilder.build());
            List<Map<String, AttributeValue>> items = resp.items();

            DynamoDBStatement stmt = new DynamoDBStatement((DynamoDBSession) session, "SCAN " + name);
            DynamoDBResultSet resultSet = new DynamoDBResultSet((DynamoDBSession) session, stmt, items);

            try {
                dataReceiver.fetchStart(session, resultSet, firstRow, maxRows);
                long rowCount = 0;
                while (resultSet.nextRow()) {
                    dataReceiver.fetchRow(session, resultSet);
                    rowCount++;
                }
                statistics.setRowsFetched(rowCount);
                statistics.addStatementsCount();
            } finally {
                dataReceiver.fetchEnd(session, resultSet);
                resultSet.close();
                stmt.close();
            }
        } catch (DBException e) {
            throw e;
        } catch (Exception e) {
            throw new DBCException("Error scanning DynamoDB table: " + e.getMessage(), e);
        }

        return statistics;
    }

    @Override
    public long countData(
            @NotNull DBCExecutionSource source,
            @NotNull DBCSession session,
            @Nullable DBDDataFilter dataFilter,
            long flags) throws DBException {
        return itemCount;
    }

    @NotNull
    @Override
    public ExecuteBatch insertData(
            @NotNull DBCSession session,
            @NotNull DBSAttributeBase[] attributes,
            @Nullable DBDDataReceiver keysReceiver,
            @NotNull DBCExecutionSource source,
            @NotNull Map<String, Object> options) throws DBException {
        return new DynamoDBExecuteBatch(session, this, attributes, DynamoDBExecuteBatch.OperationType.INSERT);
    }

    @NotNull
    @Override
    public ExecuteBatch updateData(
            @NotNull DBCSession session,
            @NotNull DBSAttributeBase[] updateAttributes,
            @NotNull DBSAttributeBase[] keyAttributes,
            @Nullable DBDDataReceiver keysReceiver,
            @NotNull DBCExecutionSource source) throws DBException {
        return new DynamoDBExecuteBatch(session, this, updateAttributes, keyAttributes, DynamoDBExecuteBatch.OperationType.UPDATE);
    }

    @NotNull
    @Override
    public ExecuteBatch deleteData(
            @NotNull DBCSession session,
            @NotNull DBSAttributeBase[] keyAttributes,
            @NotNull DBCExecutionSource source) throws DBException {
        return new DynamoDBExecuteBatch(session, this, keyAttributes, DynamoDBExecuteBatch.OperationType.DELETE);
    }

    @NotNull
    @Override
    public DBCStatistics truncateData(
            @NotNull DBCSession session,
            @NotNull DBCExecutionSource source) throws DBException {
        throw new DBCException("DynamoDB does not support TRUNCATE. Delete items individually.");
    }

    @Nullable
    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        metadataLoaded = false;
        attributes = null;
        indexes = null;
        return this;
    }

    public long getItemCount() {
        return itemCount;
    }

    public long getTableSizeBytes() {
        return tableSizeBytes;
    }

    @Nullable
    public String getTableStatus() {
        return tableStatus;
    }
}
