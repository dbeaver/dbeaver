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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.exec.DBCExecutionSource;
import org.jkiss.dbeaver.model.exec.DBCScriptContext;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;

public class ResultSetExecutionSource implements DBCExecutionSource {
    private final DBSDataContainer container;
    private final ResultSetViewer controller;
    private final Object descriptor;
    private final DBDDataFilter dataFilter;

    public  ResultSetExecutionSource(
        @NotNull DBSDataContainer container,
        @NotNull ResultSetViewer controller,
        @Nullable Object descriptor,
        @Nullable DBDDataFilter dataFilter
    ) {
        this.container = container;
        this.controller = controller;
        this.descriptor = descriptor;
        this.dataFilter = dataFilter;
    }

    public ResultSetExecutionSource(
        @NotNull DBSDataContainer container,
        @NotNull ResultSetViewer controller,
        @Nullable Object descriptor
    ) {
        this(container, controller, descriptor, null);
    }

    @NotNull
    @Override
    public DBSDataContainer getDataContainer() {
        return container;
    }

    @NotNull
    @Override
    public ResultSetViewer getExecutionController() {
        return controller;
    }

    @Nullable
    @Override
    public Object getSourceDescriptor() {
        return descriptor;
    }

    @Nullable
    @Override
    public DBCScriptContext getScriptContext() {
        return null;
    }

    @Nullable
    public DBDDataFilter getUseDataFilter() {
        if (dataFilter != null) {
            return dataFilter;
        } else if (getDataContainer() == getExecutionController().getDataContainer()) {
            return getExecutionController().getDataFilter();
        } else {
            return null;
        }
    }

    @Nullable
    public DBDDataFilter getDataFilter() {
        return dataFilter;
    }
}
