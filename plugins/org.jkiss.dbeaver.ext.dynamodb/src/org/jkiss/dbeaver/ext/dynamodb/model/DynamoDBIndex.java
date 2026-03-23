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
import org.jkiss.dbeaver.model.struct.DBSObject;

public class DynamoDBIndex implements DBSObject {

    private final DynamoDBTable table;
    private final String name;
    private final String indexType; // GSI or LSI
    private final String partitionKeyName;
    private final String sortKeyName;
    private final String projectionType;

    public DynamoDBIndex(
            @NotNull DynamoDBTable table,
            @NotNull String name,
            @NotNull String indexType,
            @Nullable String partitionKeyName,
            @Nullable String sortKeyName,
            @Nullable String projectionType) {
        this.table = table;
        this.name = name;
        this.indexType = indexType;
        this.partitionKeyName = partitionKeyName;
        this.sortKeyName = sortKeyName;
        this.projectionType = projectionType;
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @Nullable
    @Override
    public String getDescription() {
        return indexType + " - " + projectionType;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @NotNull
    @Override
    public DBSObject getParentObject() {
        return table;
    }

    @NotNull
    @Override
    public DynamoDBDataSource getDataSource() {
        return table.getDataSource();
    }

    @NotNull
    public String getIndexType() {
        return indexType;
    }

    @Nullable
    public String getPartitionKeyName() {
        return partitionKeyName;
    }

    @Nullable
    public String getSortKeyName() {
        return sortKeyName;
    }

    @Nullable
    public String getProjectionType() {
        return projectionType;
    }
}
