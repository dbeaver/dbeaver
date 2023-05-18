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
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.ui.IDataSourceConfigurator;

/**
 * DataSourceViewDescriptor
 */
public class DataSourceConfiguratorDescriptor extends AbstractDescriptor {
    public static final String EXTENSION_ID = "org.jkiss.dbeaver.dataSourceConfigurator"; //$NON-NLS-1$

    private final String id;
    private final ObjectType objectType;
    private IDataSourceConfigurator instance;

    public DataSourceConfiguratorDescriptor(IConfigurationElement config) {
        super(config.getContributor().getName());
        this.id = config.getAttribute(RegistryConstants.ATTR_ID);
        this.objectType = new ObjectType(config.getAttribute(RegistryConstants.ATTR_CLASS));
    }

    public String getId() {
        return id;
    }

    public IDataSourceConfigurator getInstance() {
        if (instance == null) {
            try {
                instance = objectType.createInstance(IDataSourceConfigurator.class);
            } catch (Throwable ex) {
                throw new IllegalStateException("Can't create configurator '" + objectType.getImplName() + "'", ex);
            }
        }
        return instance;
    }

    @Override
    public String toString() {
        return id;
    }
}
