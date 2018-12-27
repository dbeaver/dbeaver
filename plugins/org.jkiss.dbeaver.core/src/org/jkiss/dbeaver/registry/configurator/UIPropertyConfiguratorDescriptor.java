/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.registry.configurator;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.impl.AbstractContextDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.dbeaver.ui.IObjectPropertyConfigurator;

/**
 * UIPropertyConfiguratorDescriptor
 */
public class UIPropertyConfiguratorDescriptor extends AbstractContextDescriptor
{
    public static final String EXTENSION_ID = "org.jkiss.dbeaver.ui.propertyConfigurator"; //$NON-NLS-1$

    private final String objectType;
    private final ObjectType uiConfigType;

    public UIPropertyConfiguratorDescriptor(
        IConfigurationElement config)
    {
        super(config);

        this.objectType = config.getAttribute(RegistryConstants.ATTR_CLASS);
        this.uiConfigType = new ObjectType(config.getAttribute(RegistryConstants.ATTR_UI_CLASS));
    }

    public String getObjectType() {
        return objectType;
    }

    @SuppressWarnings("unchecked")
    public IObjectPropertyConfigurator<DBWHandlerConfiguration> createConfigurator()
        throws DBException
    {
        return uiConfigType.createInstance(IObjectPropertyConfigurator.class);
    }

}
