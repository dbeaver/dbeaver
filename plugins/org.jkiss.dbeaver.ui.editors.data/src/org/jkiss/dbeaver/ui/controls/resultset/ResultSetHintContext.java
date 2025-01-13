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
import org.jkiss.dbeaver.model.data.hints.DBDAttributeHintProvider;
import org.jkiss.dbeaver.model.data.hints.DBDCellHintProvider;
import org.jkiss.dbeaver.model.data.hints.DBDValueHintContext;
import org.jkiss.dbeaver.model.data.hints.DBDValueHintProvider;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.registry.data.hints.ValueHintContextConfiguration;
import org.jkiss.dbeaver.registry.data.hints.ValueHintProviderDescriptor;
import org.jkiss.dbeaver.registry.data.hints.ValueHintRegistry;
import org.jkiss.utils.CommonUtils;

import java.util.*;
import java.util.function.Supplier;

/**
 * Result set hint context
 */
public class ResultSetHintContext implements DBDValueHintContext {
    private static final Log log = Log.getLog(ResultSetHintContext.class);

    private final Supplier<DBSDataContainer> dataContainerSupplier;
    private final Supplier<DBSEntity> entitySupplier;
    private final Map<String, Object> contextAttributes = new HashMap<>();

    private final Map<DBDValueHintProvider, HintProviderInfo> hintProviders = new IdentityHashMap<>();
    private ValueHintContextConfiguration contextConfiguration;

    static class HintProviderInfo {
        final DBDValueHintProvider provider;
        boolean enabled;
        final Set<DBDAttributeBinding> attributes = new LinkedHashSet<>();

        private HintProviderInfo(DBDValueHintProvider provider) {
            this.provider = provider;
        }
    }

    ResultSetHintContext(Supplier<DBSDataContainer> dataContainerSupplier, Supplier<DBSEntity> entitySupplier) {
        this.dataContainerSupplier = dataContainerSupplier;
        this.entitySupplier = entitySupplier;
    }

    @Nullable
    @Override
    public DBSDataContainer getDataContainer() {
        return dataContainerSupplier.get();
    }

    @Nullable
    @Override
    public DBSEntity getContextEntity() {
        return entitySupplier.get();
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

    @Override
    public HintConfigurationLevel getConfigurationLevel() {
        return contextConfiguration.getLevel();
    }

    @Override
    public void setConfigurationLevel(HintConfigurationLevel level) {
        HintConfigurationLevel oldLevel = getConfigurationLevel();
        if (oldLevel == level) {
            return;
        }
        if (level.ordinal() < oldLevel.ordinal()) {
            // Delete old configuration
            contextConfiguration.deleteConfiguration();
        }

        // Detect new config
        DBSDataContainer dataContainer = getDataContainer();
        DBPDataSource ds = dataContainer == null || level == HintConfigurationLevel.GLOBAL ? null : dataContainer.getDataSource();
        DBSEntity entity = ds == null || level != HintConfigurationLevel.ENTITY ? null : entitySupplier.get();
        contextConfiguration = ValueHintRegistry.getInstance().getContextConfiguration(
            ds == null ? null : ds.getContainer(),
            entity,
            true);

        // Save new configuration
        contextConfiguration.saveConfiguration();
    }

    public ValueHintContextConfiguration getContextConfiguration() {
        return contextConfiguration;
    }

    public Set<DBDValueHintProvider> getApplicableHintProviders() {
        return hintProviders.keySet();
    }

    public List<DBDCellHintProvider> getCellHintProviders(DBDAttributeBinding attr) {
        List<DBDCellHintProvider> result = new ArrayList<>();
        for (HintProviderInfo pi : hintProviders.values()) {
            if (pi.enabled && pi.provider instanceof DBDCellHintProvider chp && pi.attributes.contains(attr)) {
                result.add(chp);
            }
        }
        return result;
    }

    public List<DBDAttributeHintProvider> getColumnHintProviders(DBDAttributeBinding attr) {
        List<DBDAttributeHintProvider> result = new ArrayList<>();
        for (HintProviderInfo pi : hintProviders.values()) {
            if (pi.enabled && pi.provider instanceof DBDAttributeHintProvider ahp && pi.attributes.contains(attr)) {
                result.add(ahp);
            }
        }
        return result;
    }

    void resetCache() {
        this.contextAttributes.clear();
        this.hintProviders.clear();
    }

    void initProviders(DBDAttributeBinding[] attributes) {
        DBSDataContainer dataContainer = getDataContainer();
        DBPDataSource ds = dataContainer == null ? null : dataContainer.getDataSource();
        DBSEntity entity = ds == null ? null : entitySupplier.get();

        contextConfiguration = ValueHintRegistry.getInstance().getContextConfiguration(
            ds == null ? null : ds.getContainer(),
            entity,
            false);
        try {
            for (DBDAttributeBinding attr : attributes) {
                ValueHintRegistry hintRegistry = ValueHintRegistry.getInstance();
                List<DBDValueHintProvider> attrHintProviders = hintRegistry.getAllValueBindings(
                    this,
                    ds,
                    attr,
                    attr.getValueHandler().getValueObjectType(attr));
                for (DBDValueHintProvider provider : attrHintProviders) {
                    HintProviderInfo providerInfo = hintProviders.computeIfAbsent(provider, p -> {
                        HintProviderInfo pi = new HintProviderInfo(p);
                        ValueHintProviderDescriptor providerDescriptor = hintRegistry.getDescriptorByInstance(provider);
                        pi.enabled = contextConfiguration.isHintEnabled(providerDescriptor);
                        return pi;
                    });
                    providerInfo.attributes.add(attr);
                }
            }
        } catch (Throwable e) {
            log.error("Error loading hint providers", e);
        }
    }

    public void cacheRequiredData(
        @NotNull DBRProgressMonitor monitor,
        @Nullable List<DBDAttributeBinding> attributes,
        @NotNull List<ResultSetRow> rows,
        boolean cleanupCache
    ) throws DBException {
        for (HintProviderInfo pi : hintProviders.values()) {
            if (pi.enabled && pi.provider instanceof DBDCellHintProvider chp) {
                chp.cacheRequiredData(
                    monitor,
                    this,
                    !CommonUtils.isEmpty(attributes) ? attributes : pi.attributes,
                    rows,
                    cleanupCache);
            }
        }
    }

}
