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
package org.jkiss.dbeaver.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;

import java.net.URL;

/**
 * ExternalResourceDescriptor
 */
public class ExternalResourceDescriptor extends AbstractDescriptor
{
    public static final String EXTENSION_ID = "org.jkiss.dbeaver.resources"; //$NON-NLS-1$

    private static final Log log = Log.getLog(ExternalResourceDescriptor.class);
    private final String name;
    private final String alias;

    public ExternalResourceDescriptor(IConfigurationElement config)
    {
        super(config);

        this.name = config.getAttribute(RegistryConstants.ATTR_NAME);
        this.alias = config.getAttribute(RegistryConstants.ATTR_ALIAS);
    }

    public String getName()
    {
        return name;
    }

    public String getAlias() {
        return alias;
    }

    public URL getURL()
    {
        return getContributorBundle().getEntry(name);
    }

}