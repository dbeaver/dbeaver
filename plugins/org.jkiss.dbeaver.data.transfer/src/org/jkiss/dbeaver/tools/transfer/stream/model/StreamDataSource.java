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

package org.jkiss.dbeaver.tools.transfer.stream.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.data.DBDPreferences;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.data.DBDValueHandlerProvider;
import org.jkiss.dbeaver.model.impl.AbstractSimpleDataSource;
import org.jkiss.dbeaver.model.impl.data.DefaultValueHandler;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.io.File;
import java.util.Collection;

/**
 * Data container transfer producer
 */
public class StreamDataSource extends AbstractSimpleDataSource implements DBDValueHandlerProvider {

    public StreamDataSource(File inputFile) {
        super(new StreamDataSourceContainer(inputFile));
    }

    public StreamDataSource(String inputName) {
        super(new StreamDataSourceContainer(inputName));
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
    public void initialize(@NotNull DBRProgressMonitor monitor) throws DBException {

    }

    @NotNull
    @Override
    public StreamExecutionContext openIsolatedContext(@NotNull DBRProgressMonitor monitor, @NotNull String purpose) throws DBException {
        return new StreamExecutionContext(this, purpose);
    }

    @Override
    public void shutdown(DBRProgressMonitor monitor) {

    }

    @Override
    public Collection<? extends DBSObject> getChildren(@NotNull DBRProgressMonitor monitor) throws DBException {
        return null;
    }

    @Override
    public DBSObject getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) throws DBException {
        return null;
    }

    @Override
    public Class<? extends DBSObject> getChildType(@NotNull DBRProgressMonitor monitor) throws DBException {
        return null;
    }

    @Override
    public void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope) throws DBException {

    }

    // We need to implement value handler provider to pass default value handler for attribute bindings
    @Nullable
    @Override
    public DBDValueHandler getValueHandler(DBPDataSource dataSource, DBDPreferences preferences, DBSTypedObject typedObject) {
        return DefaultValueHandler.INSTANCE;
    }
}
