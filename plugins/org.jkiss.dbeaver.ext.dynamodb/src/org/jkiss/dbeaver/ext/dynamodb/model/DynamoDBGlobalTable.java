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
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.List;

public class DynamoDBGlobalTable implements DBSObject {

    private final DynamoDBDataSource dataSource;
    private final String name;
    private final List<String> replicationRegions;

    public DynamoDBGlobalTable(
            @NotNull DynamoDBDataSource dataSource,
            @NotNull String name,
            @NotNull List<String> replicationRegions) {
        this.dataSource = dataSource;
        this.name = name;
        this.replicationRegions = replicationRegions;
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @Nullable
    @Override
    public String getDescription() {
        return "Regions: " + String.join(", ", replicationRegions);
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
    public DBPDataSource getDataSource() {
        return dataSource;
    }

    @NotNull
    public List<String> getReplicationRegions() {
        return replicationRegions;
    }
}
