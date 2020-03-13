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
package org.jkiss.dbeaver.model.impl;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSInstance;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * AbstractSimpleDataSource.
 * Data source which contains of single instance
 */
public abstract class AbstractSimpleDataSource<EXEC_CONTEXT extends DBCExecutionContext> implements DBPDataSource, DBSInstance, DBSObjectContainer, DBSObject {

    @NotNull
    private final DBPDataSourceContainer container;
    protected EXEC_CONTEXT executionContext;
    @NotNull
    protected List<EXEC_CONTEXT> allContexts = new ArrayList<>();

    public AbstractSimpleDataSource(@NotNull DBPDataSourceContainer container) {
        this.container = container;
    }

    @NotNull
    @Override
    public DBPDataSourceContainer getContainer() {
        return container;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @NotNull
    @Override
    public DBPDataSource getDataSource() {
        return this;
    }

    @Override
    public DBSObject getParentObject() {
        return container;
    }

    @NotNull
    @Override
    public String getName() {
        return container.getName();
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @NotNull
    @Override
    public EXEC_CONTEXT getDefaultContext(DBRProgressMonitor monitor, boolean meta) {
        return executionContext;
    }

    public EXEC_CONTEXT getDefaultContext() {
        return executionContext;
    }

    @NotNull
    @Override
    public DBCExecutionContext[] getAllContexts() {
        return allContexts.toArray(new DBCExecutionContext[0]);
    }

    @NotNull
    @Override
    public abstract EXEC_CONTEXT openIsolatedContext(@NotNull DBRProgressMonitor monitor, @NotNull String purpose, @Nullable DBCExecutionContext initFrom) throws DBException;

    public void addExecutionContext(EXEC_CONTEXT context) {
        allContexts.add(context);
    }

    public void removeExecutionContext(EXEC_CONTEXT context) {
        allContexts.remove(context);
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

}
