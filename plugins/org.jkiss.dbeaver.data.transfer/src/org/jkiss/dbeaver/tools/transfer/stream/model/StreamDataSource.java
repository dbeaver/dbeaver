/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.data.DBDFormatSettings;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.data.DBDValueHandlerProvider;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.AbstractSimpleDataSource;
import org.jkiss.dbeaver.model.impl.data.DefaultValueHandler;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.util.Collection;

/**
 * Stream data source. It is a fake client-side datasource to emulate database-like data producers.
 */
public class StreamDataSource extends AbstractSimpleDataSource<StreamExecutionContext> implements DBDValueHandlerProvider {

    private final StreamDataSourceDialect dialect;

    public StreamDataSource(StreamDataSourceContainer container) {
        super(container);
        this.executionContext = new StreamExecutionContext(this, "Main");
        this.dialect = new StreamDataSourceDialect();
    }

    public StreamDataSource(String inputName) {
        this(new StreamDataSourceContainer(inputName));
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
        return dialect;
    }

    @Override
    public void initialize(@NotNull DBRProgressMonitor monitor) throws DBException {

    }

    @NotNull
    @Override
    public StreamExecutionContext openIsolatedContext(@NotNull DBRProgressMonitor monitor, @NotNull String purpose, @Nullable DBCExecutionContext initFrom) throws DBException {
        return new StreamExecutionContext(this, purpose);
    }

    // We need to implement value handler provider to pass default value handler for attribute bindings
    @Nullable
    @Override
    public DBDValueHandler getValueHandler(DBPDataSource dataSource, DBDFormatSettings preferences, DBSTypedObject typedObject) {
        return DefaultValueHandler.INSTANCE;
    }

    @Override
    public Collection<? extends DBSObject> getChildren(@NotNull DBRProgressMonitor monitor) throws DBException {
        return null;
    }

    @Nullable
    @Override
    public DBSObject getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) throws DBException {
        return null;
    }

    @NotNull
    @Override
    public Class<? extends DBSObject> getPrimaryChildType(@Nullable DBRProgressMonitor monitor) throws DBException {
        return DBSObject.class;
    }

    @Override
    public void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope) throws DBException {

    }

}
