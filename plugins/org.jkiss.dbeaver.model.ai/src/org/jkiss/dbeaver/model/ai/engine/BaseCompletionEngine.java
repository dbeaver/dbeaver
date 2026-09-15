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
package org.jkiss.dbeaver.model.ai.engine;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Map;

public abstract class BaseCompletionEngine<PROPS extends AIEngineProperties> implements AIEngine<PROPS> {

    protected final PROPS properties;

    public BaseCompletionEngine(@NotNull PROPS properties) {
        this.properties = properties;
    }

    @NotNull
    @Override
    public PROPS getProperties() {
        return properties;
    }

    @Nullable
    protected String getCatalogProviderId() {
        return null;
    }

    @NotNull
    protected Map<String, AIModelCatalogEntry> getModelCatalog(@NotNull DBRProgressMonitor monitor) {
        String providerId = getCatalogProviderId();
        return providerId == null ? Map.of() : AIModelCatalog.getInstance().getModels(monitor, providerId);
    }

    @NotNull
    protected Map<String, AIModelCatalogEntry> getCachedModelCatalog() {
        String providerId = getCatalogProviderId();
        return providerId == null ? Map.of() : AIModelCatalog.getInstance().getCachedModels(providerId);
    }

    @Nullable
    protected AIModelCatalogEntry getCachedCatalogEntry() throws DBException {
        String model = properties.getModel();
        return model == null ? null : getCachedModelCatalog().get(model);
    }

    @Override
    public int getContextWindowSize(@NotNull DBRProgressMonitor monitor) throws DBException {
        getModelCatalog(monitor);
        Integer contextSize = properties.getContextWindowSize();
        if (contextSize != null && contextSize > 0) {
            return contextSize;
        }
        AIModelCatalogEntry entry = getCachedCatalogEntry();
        if (entry != null && entry.limit() != null && entry.limit().context() != null && entry.limit().context() > 0) {
            return entry.limit().context();
        }
        throw new DBException("Context window size is not set for the model: " + properties.getModel());
    }

    @Nullable
    protected Double getRequestTemperature() throws DBException {
        AIModelCatalogEntry entry = getCachedCatalogEntry();
        return entry != null && Boolean.FALSE.equals(entry.temperature()) ? null : temperature();
    }

    protected double temperature() throws DBException {
        return properties.getTemperature();
    }
}
