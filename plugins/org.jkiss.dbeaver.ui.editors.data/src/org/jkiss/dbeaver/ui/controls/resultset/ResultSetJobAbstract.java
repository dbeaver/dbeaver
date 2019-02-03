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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionSource;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.runtime.jobs.DataSourceJob;

abstract class ResultSetJobAbstract extends DataSourceJob implements DBCExecutionSource {

    protected final DBSDataContainer dataContainer;
    protected final ResultSetViewer controller;

    protected ResultSetJobAbstract(String name, DBSDataContainer dataContainer, ResultSetViewer controller, DBCExecutionContext executionContext) {
        super(name, executionContext);
        this.dataContainer = dataContainer;
        this.controller = controller;
        setUser(false);
    }

    @Nullable
    @Override
    public DBSDataContainer getDataContainer() {
        return dataContainer;
    }

    @NotNull
    @Override
    public Object getExecutionController() {
        return controller;
    }

    @Nullable
    @Override
    public Object getSourceDescriptor() {
        return this;
    }

}
