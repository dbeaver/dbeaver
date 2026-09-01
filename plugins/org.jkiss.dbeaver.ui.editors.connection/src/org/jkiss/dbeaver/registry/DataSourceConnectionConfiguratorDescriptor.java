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
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.utils.CommonUtils;

import java.util.List;

public class DataSourceConnectionConfiguratorDescriptor extends AbstractDescriptor {
    public static final String EXTENSION_ID = "org.jkiss.dbeaver.dataSourceConfigurator"; //$NON-NLS-1$

    private final String id;
    private final List<String> dataSourceIds;
    private final List<String> driverIds;
    private final ObjectType configuratorType;

    public DataSourceConnectionConfiguratorDescriptor(@NotNull IConfigurationElement config) {
        super(config.getContributor().getName());
        this.dataSourceIds = splitIds(config.getAttribute(RegistryConstants.ATTR_DATA_SOURCE));
        this.driverIds = splitIds(config.getAttribute(RegistryConstants.ATTR_DRIVER));
        this.configuratorType = new ObjectType(config.getAttribute(RegistryConstants.ATTR_CLASS));
        String className = configuratorType.getImplName();
        this.id = className.substring(className.lastIndexOf('.') + 1);
    }

    @NotNull
    private static List<String> splitIds(String ids) {
        return List.of(CommonUtils.split(ids, ",")).stream().map(String::trim).toList();
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public List<String> getDataSources() {
        return dataSourceIds;
    }

    @NotNull
    public List<String> getDrivers() {
        return driverIds;
    }

    @NotNull
    public <T> T createConfigurator(@NotNull Class<T> implementsClass) {
        try {
            return configuratorType.createInstance(implementsClass);
        } catch (Throwable ex) {
            throw new IllegalStateException("Can't create data source configurator '" + configuratorType.getImplName() + "'", ex);
        }
    }

    @Override
    public String toString() {
        return id;
    }
}
