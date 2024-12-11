/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.dpi.DPIContainer;
import org.jkiss.dbeaver.model.dpi.DPIElement;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Abstract DataSource.
 */
public abstract class AbstractDataSource implements DBPDataSource, DBSObject {

    @NotNull
    protected final DBPDataSourceContainer container;
    private final Map<String, Object> contextAttributes = new LinkedHashMap<>();

    public AbstractDataSource(@NotNull DBPDataSourceContainer container) {
        this.container = container;
    }

    @DPIContainer(root = true)
    @NotNull
    @Override
    public DBPDataSourceContainer getContainer() {
        return container;
    }

    @DPIContainer
    @NotNull
    @Override
    public DBPDataSource getDataSource() {
        return this;
    }

    @DPIContainer(root = true)
    @Override
    public DBSObject getParentObject() {
        return container;
    }

    @DPIElement
    @NotNull
    @Override
    public String getName() {
        return container.getName();
    }

    @DPIElement
    @Nullable
    @Override
    public String getDescription() {
        return container.getDescription();
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @Override
    public Object getDataSourceFeature(String featureId) {
        return null;
    }

    @Override
    public Map<String, ?> getContextAttributes() {
        return new LinkedHashMap<>(contextAttributes);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getContextAttribute(String attributeName) {
        return (T) contextAttributes.get(attributeName);
    }

    @Override
    public <T> void setContextAttribute(String attributeName, T attributeValue) {
        contextAttributes.put(attributeName, attributeValue);
    }

    @Override
    public void removeContextAttribute(String attributeName) {
        contextAttributes.remove(attributeName);
    }
}
