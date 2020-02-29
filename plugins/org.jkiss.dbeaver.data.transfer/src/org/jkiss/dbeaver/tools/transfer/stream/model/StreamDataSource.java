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

package org.jkiss.dbeaver.tools.transfer.stream.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.data.DBDPreferences;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.data.DBDValueHandlerProvider;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.data.DefaultValueHandler;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.struct.DBSInstance;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

/**
 * Data container transfer producer
 */
public class StreamDataSource implements DBPDataSource, DBSInstance, DBDValueHandlerProvider {

    private final StreamDataSourceContainer container;
    private final StreamExecutionContext executionContext;

    public StreamDataSource(StreamDataSourceContainer container) {
        this.container = container;
        this.executionContext = new StreamExecutionContext(this, "Main");
    }

    public StreamDataSource(File inputFile) {
        this(new StreamDataSourceContainer(inputFile));
    }

    public StreamDataSource(String inputName) {
        this(new StreamDataSourceContainer(inputName));
    }

    @NotNull
    @Override
    public DBPDataSourceContainer getContainer() {
        return container;
    }

    @NotNull
    @Override
    public DBPDataSourceInfo getInfo() {
        return new StreamDataSourceInfo();
    }

    @Override
    public Object getDataSourceFeature(String featureId) {
        return null;
    }

    @Override
    public SQLDialect getSQLDialect() {
        return BasicSQLDialect.INSTANCE;
    }

    @Override
    public void initialize(@NotNull DBRProgressMonitor monitor) throws DBException {

    }

    @NotNull
    @Override
    public DBCExecutionContext getDefaultContext(DBRProgressMonitor monitor, boolean meta) {
        return executionContext;
    }

    @NotNull
    @Override
    public DBCExecutionContext[] getAllContexts() {
        return new DBCExecutionContext[] { executionContext };
    }

    @NotNull
    @Override
    public StreamExecutionContext openIsolatedContext(@NotNull DBRProgressMonitor monitor, @NotNull String purpose, @Nullable DBCExecutionContext initFrom) throws DBException {
        return new StreamExecutionContext(this, purpose);
    }

    @NotNull
    @Override
    public DBSInstance getDefaultInstance() {
        return this;
    }

    @NotNull
    @Override
    public Collection<? extends DBSInstance> getAvailableInstances() {
        return Collections.singleton(this);
    }

    @Override
    public void shutdown(DBRProgressMonitor monitor) {

    }

    // We need to implement value handler provider to pass default value handler for attribute bindings
    @Nullable
    @Override
    public DBDValueHandler getValueHandler(DBPDataSource dataSource, DBDPreferences preferences, DBSTypedObject typedObject) {
        return DefaultValueHandler.INSTANCE;
    }

    @Nullable
    @Override
    public DBSObject getParentObject() {
        return null;
    }

    @NotNull
    @Override
    public DBPDataSource getDataSource() {
        return this;
    }

    @NotNull
    @Override
    public String getName() {
        return container.getName();
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }
}
