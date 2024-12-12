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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.hints.DBDValueHintContext;
import org.jkiss.dbeaver.model.data.hints.DBDValueHintProvider;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.registry.data.hints.ValueHintRegistry;

import java.util.*;
import java.util.function.Supplier;

/**
 * Result set hint context
 */
class ResultSetHintContext implements DBDValueHintContext {
    private static final Log log = Log.getLog(ResultSetHintContext.class);

    private final Supplier<DBSDataContainer> dataContainerSupplier;
    private final Map<String, Object> contextAttributes = new HashMap<>();

    private final Map<DBDValueHintProvider, HintProviderInfo> hintProviders = new IdentityHashMap<>();

    private static class HintProviderInfo {
        final DBDValueHintProvider provider;
        boolean enabled;
        final Set<DBDAttributeBinding> attributes = new LinkedHashSet<>();

        private HintProviderInfo(DBDValueHintProvider provider) {
            this.provider = provider;
        }
    }

    ResultSetHintContext(Supplier<DBSDataContainer> dataContainerSupplier) {
        this.dataContainerSupplier = dataContainerSupplier;
    }

    @Nullable
    @Override
    public DBSDataContainer getDataContainer() {
        return dataContainerSupplier.get();
    }

    @Nullable
    @Override
    public Object getHintContextAttribute(@NotNull String name) {
        return contextAttributes.get(name);
    }

    @Override
    public void setHintContextAttribute(@NotNull String name, @Nullable Object value) {
        if (value == null) {
            this.contextAttributes.remove(name);
        } else {
            this.contextAttributes.put(name, value);
        }
    }

    List<DBDValueHintProvider> getHintProviders(DBDAttributeBinding attr) {
        List<DBDValueHintProvider> result = new ArrayList<>();
        for (HintProviderInfo pi : hintProviders.values()) {
            if (pi.enabled && pi.attributes.contains(attr)) {
                result.add(pi.provider);
            }
        }
        return result;
    }

    void resetCache() {
        this.contextAttributes.clear();
        this.hintProviders.clear();
    }

    void initProviders(DBDAttributeBinding[] attributes) {
        try {
            DBSDataContainer dataContainer = getDataContainer();
            DBPDataSource ds = dataContainer == null ? null : dataContainer.getDataSource();
            for (DBDAttributeBinding attr : attributes) {
                List<DBDValueHintProvider> attrHintProviders = ValueHintRegistry.getInstance().getAllValueBindings(ds, attr, null);
                for (DBDValueHintProvider provider : attrHintProviders) {
                    HintProviderInfo providerInfo = hintProviders.computeIfAbsent(provider, HintProviderInfo::new);
                    providerInfo.enabled = true;
                    providerInfo.attributes.add(attr);
                }
            }
        } catch (Throwable e) {
            log.error("Error loading hint providers", e);
        }
    }

    public void cacheRequiredData(@NotNull DBRProgressMonitor monitor, @NotNull List<ResultSetRow> rows, boolean cleanupCache) throws DBException {
        for (HintProviderInfo pi : hintProviders.values()) {
            if (pi.enabled) {
                pi.provider.cacheRequiredData(
                    monitor,
                    this,
                    pi.attributes,
                    rows,
                    cleanupCache);
            }
        }
    }

}
