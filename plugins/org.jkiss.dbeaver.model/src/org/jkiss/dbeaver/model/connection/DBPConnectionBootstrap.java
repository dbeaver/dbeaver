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
package org.jkiss.dbeaver.model.connection;

import org.jkiss.dbeaver.runtime.IVariableResolver;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Connection bootstrap info.
 * Bootstrap properties are applied to each opened connection after data-source initialization.
 */
public class DBPConnectionBootstrap {
    private String defaultCatalogName;
    private String defaultSchemaName;
    private Boolean defaultAutoCommit;
    private Integer defaultTransactionIsolation;
    private final List<String> initQueries;
    private boolean ignoreErrors;

    public DBPConnectionBootstrap() {
        this.initQueries = new ArrayList<>();
        this.ignoreErrors = false;
    }

    public DBPConnectionBootstrap(DBPConnectionBootstrap info) {
        this.defaultCatalogName = info.defaultCatalogName;
        this.defaultSchemaName = info.defaultSchemaName;
        this.defaultAutoCommit = info.defaultAutoCommit;
        this.defaultTransactionIsolation = info.defaultTransactionIsolation;
        this.initQueries = new ArrayList<>(info.initQueries);
        this.ignoreErrors = info.ignoreErrors;
    }

    public List<String> getInitQueries() {
        return initQueries;
    }

    public void setInitQueries(Collection<String> queries) {
        initQueries.clear();
        initQueries.addAll(queries);
    }

    public String getDefaultCatalogName() {
        return defaultCatalogName;
    }

    public void setDefaultCatalogName(String defaultCatalogName) {
        this.defaultCatalogName = defaultCatalogName;
    }

    public String getDefaultSchemaName() {
        return defaultSchemaName;
    }

    public void setDefaultSchemaName(String defaultSchemaName) {
        this.defaultSchemaName = defaultSchemaName;
    }

    public Boolean getDefaultAutoCommit() {
        return defaultAutoCommit;
    }

    public void setDefaultAutoCommit(Boolean defaultAutoCommit) {
        this.defaultAutoCommit = defaultAutoCommit;
    }

    public Integer getDefaultTransactionIsolation() {
        return defaultTransactionIsolation;
    }

    public void setDefaultTransactionIsolation(Integer defaultTransactionIsolation) {
        this.defaultTransactionIsolation = defaultTransactionIsolation;
    }

    public boolean isIgnoreErrors() {
        return ignoreErrors;
    }

    public void setIgnoreErrors(boolean ignoreErrors) {
        this.ignoreErrors = ignoreErrors;
    }

    public boolean hasData() {
        return
                defaultAutoCommit != null ||
                defaultTransactionIsolation != null ||
                !CommonUtils.isEmpty(defaultCatalogName) ||
                !CommonUtils.isEmpty(defaultSchemaName) ||
                ignoreErrors ||
                !CommonUtils.isEmpty(initQueries);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DBPConnectionBootstrap)) {
            return false;
        }
        DBPConnectionBootstrap source = (DBPConnectionBootstrap) obj;
        return
                CommonUtils.equalObjects(this.defaultCatalogName, source.defaultCatalogName) &&
                CommonUtils.equalObjects(this.defaultSchemaName, source.defaultSchemaName) &&
                CommonUtils.equalObjects(this.defaultAutoCommit, source.defaultAutoCommit) &&
                CommonUtils.equalObjects(this.defaultTransactionIsolation, source.defaultTransactionIsolation) &&
                CommonUtils.equalObjects(this.initQueries, source.initQueries) &&
                this.ignoreErrors == source.ignoreErrors;
    }

    void resolveDynamicVariables(IVariableResolver variableResolver) {
        this.defaultCatalogName = GeneralUtils.replaceVariables(this.defaultCatalogName, variableResolver);
        this.defaultSchemaName = GeneralUtils.replaceVariables(this.defaultSchemaName, variableResolver);
        for (int i = 0; i < initQueries.size(); i++) {
            initQueries.set(i, GeneralUtils.replaceVariables(initQueries.get(i), variableResolver));
        }
    }

}
