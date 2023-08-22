/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

package org.jkiss.dbeaver.model.logical;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceContainerProvider;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBPObjectWithDescription;

import java.util.List;

/**
 * Logical datasource
 */
public class DBSLogicalDataSource implements DBPDataSourceContainerProvider, DBPNamedObject, DBPObjectWithDescription {

    private final DBPDataSourceContainer dataSourceContainer;
    private String name;
    private String description;
    private List<DBSLogicalCatalog> catalogs;
    private String currentCatalog;
    private String currentSchema;

    public DBSLogicalDataSource(DBPDataSourceContainer dataSourceContainer, String name, String description) {
        this.dataSourceContainer = dataSourceContainer;
        this.name = name;
        this.description = description;
    }

    @NotNull
    @Override
    public DBPDataSourceContainer getDataSourceContainer() {
        return dataSourceContainer;
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Nullable
    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<DBSLogicalCatalog> getCatalogs() {
        return catalogs;
    }

    public void setCatalogs(List<DBSLogicalCatalog> catalogs) {
        this.catalogs = catalogs;
    }

    public String getCurrentCatalog() {
        return currentCatalog;
    }

    public void setCurrentCatalog(String currentCatalog) {
        this.currentCatalog = currentCatalog;
    }

    public String getCurrentSchema() {
        return currentSchema;
    }

    public void setCurrentSchema(String currentSchema) {
        this.currentSchema = currentSchema;
    }
}
