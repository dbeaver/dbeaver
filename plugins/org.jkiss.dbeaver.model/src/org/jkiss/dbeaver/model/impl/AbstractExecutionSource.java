/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.DBPContextWithAttributes;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionSource;
import org.jkiss.dbeaver.model.exec.DBCScriptContext;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AbstractExecutionSource
 */
public class AbstractExecutionSource implements DBCExecutionSource, DBPContextWithAttributes {

    @Nullable
    private final DBSDataContainer dataContainer;
    @Nullable
    private final DBCExecutionContext executionContext;
    @Nullable
    private final transient Object controller;
    @Nullable
    private final Object descriptor;

    @Nullable
    private DBCScriptContext scriptContext;

    @Nullable
    private Map<String, Object> contextAttributes;

    public AbstractExecutionSource(
        @Nullable DBSDataContainer dataContainer,
        @Nullable DBCExecutionContext executionContext,
        @Nullable Object controller
    ) {
        this(dataContainer, executionContext, controller, null);
    }

    public AbstractExecutionSource(
        @Nullable DBSDataContainer dataContainer,
        @Nullable DBCExecutionContext executionContext,
        @Nullable Object controller,
        @Nullable Object descriptor
    ) {
        this.dataContainer = dataContainer;
        this.executionContext = executionContext;
        this.controller = controller;
        this.descriptor = descriptor;
    }

    @Nullable
    @Override
    public DBSDataContainer getDataContainer() {
        return dataContainer;
    }

    @Nullable
    @Override
    public Object getExecutionController() {
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
        return scriptContext;
    }

    public void setScriptContext(@Nullable DBCScriptContext scriptContext) {
        this.scriptContext = scriptContext;
    }

    @NotNull
    @Override
    public Map<String, ?> getContextAttributes() {
        return this.contextAttributes == null ? Collections.emptyMap() :
            Collections.unmodifiableMap(new LinkedHashMap<>(this.contextAttributes));
    }

    @Nullable
    @SuppressWarnings("unchecked")
    @Override
    public <T> T getContextAttribute(@NotNull String attributeName) {
        return this.contextAttributes == null ? null : (T) this.contextAttributes.get(attributeName);
    }

    @Override
    public <T> void setContextAttribute(@NotNull String attributeName, @Nullable T attributeValue) {
        if (this.contextAttributes == null) {
            this.contextAttributes = new LinkedHashMap<>();
        }
        this.contextAttributes.put(attributeName, attributeValue);
    }

    @Override
    public void removeContextAttribute(@NotNull String attributeName) {
        if (this.contextAttributes != null) {
            this.contextAttributes.remove(attributeName);
            if (this.contextAttributes.isEmpty()) {
                this.contextAttributes = null;
            }
        }
    }
}
