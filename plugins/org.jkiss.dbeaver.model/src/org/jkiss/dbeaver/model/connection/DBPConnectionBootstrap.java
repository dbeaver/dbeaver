/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * Connection bootstrap info.
 * Bootstrap properties are applied to each opened connection after data-source initialization.
 */
public class DBPConnectionBootstrap
{
    private String defaultObjectName;
    private Boolean defaultAutoCommit;
    private Integer defaultTransactionIsolation;
    private final List<String> initQueries;
    private boolean ignoreErrors;

    public DBPConnectionBootstrap()
    {
        this.initQueries = new ArrayList<>();
        this.ignoreErrors = false;
    }

    public DBPConnectionBootstrap(DBPConnectionBootstrap info)
    {
        this.defaultObjectName = info.defaultObjectName;
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

    public String getDefaultObjectName() {
        return defaultObjectName;
    }

    public void setDefaultObjectName(String defaultObjectName) {
        this.defaultObjectName = defaultObjectName;
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
            !CommonUtils.isEmpty(defaultObjectName) ||
            ignoreErrors ||
            !CommonUtils.isEmpty(initQueries);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DBPConnectionBootstrap)) {
            return false;
        }
        DBPConnectionBootstrap source = (DBPConnectionBootstrap)obj;
        return
            CommonUtils.equalObjects(this.defaultObjectName, source.defaultObjectName) &&
            CommonUtils.equalObjects(this.defaultAutoCommit, source.defaultAutoCommit) &&
            CommonUtils.equalObjects(this.defaultTransactionIsolation, source.defaultTransactionIsolation) &&
            CommonUtils.equalObjects(this.initQueries, source.initQueries) &&
            this.ignoreErrors == source.ignoreErrors;
    }
}
