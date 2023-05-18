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
package org.jkiss.dbeaver.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPDataSourceProviderDescriptor;
import org.jkiss.dbeaver.ui.IDataSourceConfigurator;

import java.util.ArrayList;
import java.util.List;

public class DataSourceViewRegistry {

    private static DataSourceViewRegistry instance = null;

    private final List<DataSourceViewDescriptor> views = new ArrayList<>();
    private final List<DataSourceConfiguratorDescriptor> configurators = new ArrayList<>();

    public synchronized static DataSourceViewRegistry getInstance() {
        if (instance == null) {
            instance = new DataSourceViewRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private DataSourceViewRegistry(IExtensionRegistry registry) {
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

    public DataSourceViewDescriptor findView(DBPDataSourceProviderDescriptor provider, String targetID) {
        for (DBPDataSourceProviderDescriptor pd = provider; pd != null; pd = pd.getParentProvider()) {
            for (DataSourceViewDescriptor view : views) {
                if (view.getDataSources().contains(pd.getId()) && targetID.equals(view.getTargetID())) {
                    return view;
                }
            }
        }
        return null;
    }

    public List<DataSourceViewDescriptor> getViews(DBPDataSourceProviderDescriptor provider, String targetID) {
        List<DataSourceViewDescriptor> result = new ArrayList<>();
        for (DBPDataSourceProviderDescriptor pd = provider; pd != null; pd = pd.getParentProvider()) {
            for (DataSourceViewDescriptor view : views) {
                if (view.getDataSources().contains(pd.getId()) && targetID.equals(view.getTargetID())) {
                    result.add(view);
                }
            }
        }
        return result;
    }

    public List<IDataSourceConfigurator> getConfigurators(DBPDataSourceContainer dataSourceContainer) {
        List<IDataSourceConfigurator> result = new ArrayList<>();
        for (DataSourceConfiguratorDescriptor configuratorDesc : configurators) {
            if (configuratorDesc.getInstance().appliesTo(dataSourceContainer)) {
                result.add(configuratorDesc.getInstance());
            }
        }
        return result;
    }

}
