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
package org.jkiss.dbeaver.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPDataSourceProviderDescriptor;

import java.util.ArrayList;
import java.util.List;

public class DataSourceViewRegistry {

    private static DataSourceViewRegistry instance = null;

    private final List<DataSourceViewDescriptor> views = new ArrayList<>();
    private final List<DataSourceConfiguratorDescriptor> configurators = new ArrayList<>();

    @NotNull
    public synchronized static DataSourceViewRegistry getInstance() {
        if (instance == null) {
            instance = new DataSourceViewRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private DataSourceViewRegistry(@NotNull IExtensionRegistry registry) {
        for (IConfigurationElement viewElement : registry.getConfigurationElementsFor(DataSourceViewDescriptor.EXTENSION_ID)) {
            if (viewElement.getName().equals(RegistryConstants.TAG_VIEW)) {
                this.views.add(
                    new DataSourceViewDescriptor(viewElement));
            }
        }

        for (IConfigurationElement cfgElement : registry.getConfigurationElementsFor(DataSourceConfiguratorDescriptor.EXTENSION_ID)) {
            if (cfgElement.getName().equals("dataSourceConfigurator")) {
                this.configurators.add(
                    new DataSourceConfiguratorDescriptor(cfgElement));
            }
        }
    }

    @Nullable
    public DataSourceViewDescriptor findView(@NotNull DBPDataSourceProviderDescriptor provider, @NotNull String targetID) {
        for (DBPDataSourceProviderDescriptor pd = provider; pd != null; pd = pd.getParentProvider()) {
            for (DataSourceViewDescriptor view : views) {
                if (view.getDataSources().contains(pd.getId()) && targetID.equals(view.getTargetID())) {
                    return view;
                }
            }
        }
        return null;
    }

    @NotNull
    public List<DataSourceConfiguratorDescriptor> getConfigurators(@NotNull DBPDataSourceContainer dataSourceContainer) {
        List<DataSourceConfiguratorDescriptor> result = new ArrayList<>();
        for (DataSourceConfiguratorDescriptor configuratorDesc : configurators) {
            if (configuratorDesc.appliesTo(dataSourceContainer)) {
                result.add(configuratorDesc);
            }
        }
        return result;
    }

    @NotNull
    public List<DataSourcePageDescriptor> getRootDataSourcePages(@NotNull DBPDataSourceContainer dataSource) {
        List<DataSourcePageDescriptor> roots = new ArrayList<>();
        for (DataSourceConfiguratorDescriptor configurator : getConfigurators(dataSource)) {
            roots.addAll(configurator.getRootPages(dataSource));
        }
        return sortPages(roots);
    }

    @NotNull
    public List<DataSourcePageDescriptor> getChildDataSourcePages(@NotNull DBPDataSourceContainer dataSource, @Nullable String parentId) {
        List<DataSourcePageDescriptor> children = new ArrayList<>();
        for (DataSourceConfiguratorDescriptor configurator : getConfigurators(dataSource)) {
            children.addAll(configurator.getChildPages(dataSource, parentId));
        }
        return sortPages(children);
    }

    @NotNull
    private List<DataSourcePageDescriptor> sortPages(@NotNull List<DataSourcePageDescriptor> pages) {
        pages.sort((o1, o2) -> {
            if (o1.getId().equals(o2.getAfterPageId())) {
                return -1;
            } else if (o2.getId().equals(o1.getAfterPageId())) {
                return 1;
            } else {
                return 0;
            }
        });
        return pages;
    }

}
