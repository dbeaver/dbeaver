/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.model.connection;

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

}
