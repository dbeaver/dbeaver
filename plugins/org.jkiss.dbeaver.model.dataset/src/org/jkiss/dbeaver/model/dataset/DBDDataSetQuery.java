/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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

package org.jkiss.dbeaver.model.dataset;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.virtual.DBVColorOverride;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dataset query
 */
public class DBDDataSetQuery {

    // Query ID
    @NotNull
    private String id;
    private String description;
    // Query datasource
    @NotNull
    private DBPDataSourceContainer dataSourceContainer;
    // Query text
    @NotNull
    private String queryText;
    // Data filters
    private DBDDataFilter dataFilters;
    // Custom colors
    private List<DBVColorOverride> colorOverrides;
    private final Map<String, String> queryParameters = new LinkedHashMap<>();
    private final DBDDataViewSettings viewSettings = new DBDDataViewSettings();

    public DBDDataSetQuery() {
    }

    @NotNull
    public String getId() {
        return id;
    }

    public void setId(@NotNull String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @NotNull
    public DBPDataSourceContainer getDataSourceContainer() {
        return dataSourceContainer;
    }

    public void setDataSourceContainer(@NotNull DBPDataSourceContainer dataSourceContainer) {
        this.dataSourceContainer = dataSourceContainer;
    }

    @NotNull
    public String getQueryText() {
        return queryText;
    }

    public void setQueryText(@NotNull String queryText) {
        this.queryText = queryText;
    }

    public DBDDataFilter getDataFilters() {
        return dataFilters;
    }

    public void setDataFilters(DBDDataFilter dataFilters) {
        this.dataFilters = dataFilters;
    }

    public List<DBVColorOverride> getColorOverrides() {
        return colorOverrides;
    }

    public void setColorOverrides(List<DBVColorOverride> colorOverrides) {
        this.colorOverrides = colorOverrides;
    }

    public Map<String, String> getQueryParameters() {
        return queryParameters;
    }

    public String getQueryParameter(String name) {
        return queryParameters.get(name);
    }

    public void setQueryParameter(String name, String value) {
        queryParameters.put(name, value);
    }

    public void setQueryParameters(Map<String, String> parameters) {
        queryParameters.clear();
        queryParameters.putAll(parameters);
    }

    public DBDDataViewSettings getViewSettings() {
        return viewSettings;
    }
}
