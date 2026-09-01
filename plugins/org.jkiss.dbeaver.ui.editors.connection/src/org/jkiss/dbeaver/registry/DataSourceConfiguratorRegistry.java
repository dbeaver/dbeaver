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
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

public class DataSourceConfiguratorRegistry {
    private static final String CONNECTION_PAGE_EXTENSION_ID = "org.jkiss.dbeaver.connectionPageConfigurator"; //$NON-NLS-1$

    private static DataSourceConfiguratorRegistry instance = null;

    private final List<DataSourceConnectionConfiguratorDescriptor> connectionConfigurators = new ArrayList<>();
    private final List<DataSourcePageDescriptor> pages = new ArrayList<>();

    @NotNull
    public synchronized static DataSourceConfiguratorRegistry getInstance() {
        if (instance == null) {
            instance = new DataSourceConfiguratorRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private DataSourceConfiguratorRegistry(@NotNull IExtensionRegistry registry) {
        for (IConfigurationElement element : registry.getConfigurationElementsFor(
            DataSourceConnectionConfiguratorDescriptor.EXTENSION_ID)) {
            if (element.getName().equals("configurator")) {
                connectionConfigurators.add(new DataSourceConnectionConfiguratorDescriptor(element));
            }
        }

        for (IConfigurationElement element : registry.getConfigurationElementsFor(CONNECTION_PAGE_EXTENSION_ID)) {
            if (element.getName().equals("page")) {
                pages.add(new DataSourcePageDescriptor(element));
            }
        }
    }

    @Nullable
    public DataSourceConnectionConfiguratorDescriptor findConnectionConfigurator(@NotNull DBPDriver driver) {
        for (DataSourceConnectionConfiguratorDescriptor configurator : connectionConfigurators) {
            if (configurator.getDrivers().contains(driver.getId())) {
                return configurator;
            }
        }
        return findConnectionConfigurator(driver.getProviderDescriptor());
    }

    @Nullable
    public DataSourceConnectionConfiguratorDescriptor findConnectionConfigurator(
        @NotNull DBPDataSourceProviderDescriptor provider
    ) {
        for (DBPDataSourceProviderDescriptor pd = provider; pd != null; pd = pd.getParentProvider()) {
            for (DataSourceConnectionConfiguratorDescriptor configurator : connectionConfigurators) {
                if (configurator.getDataSources().contains(pd.getId())) {
                    return configurator;
                }
            }
        }
        return null;
    }

    @NotNull
    public List<DataSourcePageDescriptor> getRootDataSourcePages(@NotNull DBPDataSourceContainer dataSource) {
        List<DataSourcePageDescriptor> roots = new ArrayList<>();
        for (DataSourcePageDescriptor page : pages) {
            if (CommonUtils.isEmpty(page.getParentId()) && page.appliesTo(dataSource)) {
                roots.add(page);
            }
        }
        return sortPages(roots);
    }

    @NotNull
    public List<DataSourcePageDescriptor> getChildDataSourcePages(@NotNull DBPDataSourceContainer dataSource, @Nullable String parentId) {
        List<DataSourcePageDescriptor> children = new ArrayList<>();
        for (DataSourcePageDescriptor page : pages) {
            if (parentId != null && parentId.equals(page.getParentId()) && page.appliesTo(dataSource)) {
                children.add(page);
            }
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
