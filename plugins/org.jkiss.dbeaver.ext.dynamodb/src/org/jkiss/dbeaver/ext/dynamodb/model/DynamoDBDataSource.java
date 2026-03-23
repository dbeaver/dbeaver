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
import org.jkiss.dbeaver.ext.dynamodb.DynamoDBConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.DBPExclusiveResource;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.AbstractDataSource;
import org.jkiss.dbeaver.model.impl.SimpleExclusiveLock;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.struct.DBSInstance;
import org.jkiss.dbeaver.model.struct.DBSInstanceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.utils.CommonUtils;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.dynamodb.model.GlobalTable;
import software.amazon.awssdk.services.dynamodb.model.ListGlobalTablesRequest;
import software.amazon.awssdk.services.dynamodb.model.ListGlobalTablesResponse;
import software.amazon.awssdk.services.dynamodb.model.ListTablesRequest;
import software.amazon.awssdk.services.dynamodb.model.ListTablesResponse;
import software.amazon.awssdk.services.dynamodb.model.Replica;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class DynamoDBDataSource extends AbstractDataSource
        implements DBSObjectContainer, DBSInstance, DBSInstanceContainer, DBPRefreshableObject {

    private static final Log log = Log.getLog(DynamoDBDataSource.class);

    private DynamoDbClient dynamoClient;
    private DynamoDBExecutionContext defaultContext;
    private DynamoDBSQLDialect sqlDialect;
    private DynamoDBDataSourceInfo dataSourceInfo;
    private final SimpleExclusiveLock exclusiveLock = new SimpleExclusiveLock();
    private List<DynamoDBTable> tableCache;
    private List<DynamoDBGlobalTable> globalTableCache;
    private volatile boolean tablesCached;

    public DynamoDBDataSource(
            @NotNull DBRProgressMonitor monitor,
            @NotNull DBPDataSourceContainer container) throws DBException {
        super(container);
        this.sqlDialect = new DynamoDBSQLDialect();
        this.dataSourceInfo = new DynamoDBDataSourceInfo();
        this.defaultContext = new DynamoDBExecutionContext(this, "Main");
    }

    @Override
    public void initialize(@NotNull DBRProgressMonitor monitor) throws DBException {
        DBPConnectionConfiguration cfg = getContainer().getConnectionConfiguration();
        String region = cfg.getProviderProperty(DynamoDBConstants.PROP_REGION);
        if (CommonUtils.isEmpty(region)) {
            region = cfg.getServerName();
        }
        if (CommonUtils.isEmpty(region)) {
            region = DynamoDBConstants.DEFAULT_REGION;
        }

        DynamoDbClientBuilder builder = DynamoDbClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DynamoDBCredentialsFactory.createCredentialsProvider(getContainer()));

        String endpoint = cfg.getProviderProperty(DynamoDBConstants.PROP_ENDPOINT);
        if (!CommonUtils.isEmpty(endpoint)) {
            builder.endpointOverride(URI.create(endpoint));
        }

        try {
            this.dynamoClient = builder.build();
        } catch (Exception e) {
            throw new DBException("Cannot create DynamoDB client: " + e.getMessage(), e);
        }

        // Validate connectivity
        try {
            dynamoClient.listTables(ListTablesRequest.builder().limit(1).build());
        } catch (Exception e) {
            dynamoClient.close();
            dynamoClient = null;
            throw new DBException("Cannot connect to DynamoDB: " + e.getMessage(), e);
        }

        log.debug("DynamoDB connection initialized: region=" + region);
    }

    @Override
    public void shutdown(@NotNull DBRProgressMonitor monitor) {
        if (defaultContext != null) {
            defaultContext.close();
            defaultContext = null;
        }
        if (dynamoClient != null) {
            dynamoClient.close();
            dynamoClient = null;
        }
        tableCache = null;
        tablesCached = false;
    }

    @NotNull
    @Override
    public DBPDataSourceInfo getInfo() {
        return dataSourceInfo;
    }

    @NotNull
    @Override
    public SQLDialect getSQLDialect() {
        return sqlDialect;
    }

    @NotNull
    @Override
    public DBCExecutionContext getDefaultContext(@NotNull DBRProgressMonitor monitor, boolean meta) {
        return defaultContext;
    }

    @NotNull
    @Override
    public DBCExecutionContext[] getAllContexts() {
        return defaultContext == null
                ? new DBCExecutionContext[0]
                : new DBCExecutionContext[]{defaultContext};
    }

    @NotNull
    @Override
    public DBCExecutionContext openIsolatedContext(
            @NotNull DBRProgressMonitor monitor,
            @NotNull String purpose,
            @Nullable DBCExecutionContext initFrom) throws DBException {
        return new DynamoDBExecutionContext(this, purpose);
    }

    @NotNull
    @Override
    public DBPExclusiveResource getExclusiveLock() {
        return exclusiveLock;
    }

    @NotNull
    @Override
    public DBSInstance getDefaultInstance() {
        return this;
    }

    @NotNull
    @Override
    public Collection<? extends DBSInstance> getAvailableInstances() {
        return Collections.singletonList(this);
    }

    @Nullable
    @Override
    public Collection<? extends DBSObject> getChildren(@NotNull DBRProgressMonitor monitor) throws DBException {
        return getTables(monitor);
    }

    @Nullable
    @Override
    public DBSObject getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) throws DBException {
        for (DynamoDBTable table : getTables(monitor)) {
            if (table.getName().equals(childName)) {
                return table;
            }
        }
        return null;
    }

    @NotNull
    @Override
    public Class<? extends DBSObject> getPrimaryChildType(@Nullable DBRProgressMonitor monitor) throws DBException {
        return DynamoDBTable.class;
    }

    @Override
    public void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope) throws DBException {
        getTables(monitor);
    }

    @NotNull
    public List<DynamoDBTable> getTables(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (!tablesCached) {
            loadTables(monitor);
        }
        return tableCache != null ? tableCache : Collections.emptyList();
    }

    private void loadTables(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (dynamoClient == null) {
            throw new DBException("DynamoDB client is not connected");
        }
        monitor.beginTask("Loading DynamoDB tables", 1);
        try {
            List<DynamoDBTable> tables = new ArrayList<>();
            String lastEvaluated = null;
            do {
                ListTablesRequest.Builder req = ListTablesRequest.builder();
                if (lastEvaluated != null) {
                    req.exclusiveStartTableName(lastEvaluated);
                }
                ListTablesResponse resp = dynamoClient.listTables(req.build());
                for (String name : resp.tableNames()) {
                    tables.add(new DynamoDBTable(this, name));
                }
                lastEvaluated = resp.lastEvaluatedTableName();
            } while (lastEvaluated != null);

            this.tableCache = tables;
            this.tablesCached = true;
        } catch (Exception e) {
            throw new DBException("Error loading DynamoDB tables: " + e.getMessage(), e);
        } finally {
            monitor.done();
        }
    }

    @NotNull
    public List<DynamoDBGlobalTable> getGlobalTables(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (globalTableCache == null) {
            loadGlobalTables(monitor);
        }
        return globalTableCache != null ? globalTableCache : Collections.emptyList();
    }

    private void loadGlobalTables(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (dynamoClient == null) return;
        try {
            List<DynamoDBGlobalTable> globals = new ArrayList<>();
            String lastEvaluated = null;
            do {
                ListGlobalTablesRequest.Builder req = ListGlobalTablesRequest.builder();
                if (lastEvaluated != null) req.exclusiveStartGlobalTableName(lastEvaluated);
                ListGlobalTablesResponse resp = dynamoClient.listGlobalTables(req.build());
                if (resp.globalTables() != null) {
                    for (GlobalTable gt : resp.globalTables()) {
                        List<String> regions = new ArrayList<>();
                        if (gt.replicationGroup() != null) {
                            for (Replica r : gt.replicationGroup()) {
                                regions.add(r.regionName());
                            }
                        }
                        globals.add(new DynamoDBGlobalTable(this, gt.globalTableName(), regions));
                    }
                }
                lastEvaluated = resp.lastEvaluatedGlobalTableName();
            } while (lastEvaluated != null);
            this.globalTableCache = globals;
        } catch (Exception e) {
            log.debug("Error loading global tables (may not be available): " + e.getMessage());
            this.globalTableCache = Collections.emptyList();
        }
    }

    @Nullable
    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        tablesCached = false;
        tableCache = null;
        globalTableCache = null;
        return this;
    }

    @Nullable
    public DynamoDbClient getDynamoClient() {
        return dynamoClient;
    }
}
