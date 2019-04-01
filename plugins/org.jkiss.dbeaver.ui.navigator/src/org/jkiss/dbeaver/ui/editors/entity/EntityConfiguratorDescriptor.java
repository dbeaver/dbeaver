/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.editors.entity;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.impl.AbstractContextDescriptor;
import org.jkiss.dbeaver.ui.editors.entity.properties.ObjectPropertiesEditor;
import org.jkiss.dbeaver.ui.internal.UINavigatorActivator;

/**
 * EntityConfiguratorDescriptor
 */
public class EntityConfiguratorDescriptor extends AbstractContextDescriptor
{
    private static final Log log = Log.getLog(EntityConfiguratorDescriptor.class);

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.databaseObjectConfigurator"; //NON-NLS-1 //$NON-NLS-1$

    private ObjectType implType;

    EntityConfiguratorDescriptor()
    {
        super(UINavigatorActivator.PLUGIN_ID);
        this.implType = new ObjectType(ObjectPropertiesEditor.class.getName());
    }

    public EntityConfiguratorDescriptor(IConfigurationElement config)
    {
        super(config);

        this.implType = new ObjectType(config.getAttribute("class"));
    }

    public DBEObjectConfigurator createConfigurator()
    {
        try {
            return implType.createInstance(DBEObjectConfigurator.class);
        } catch (Exception ex) {
            log.error("Error instantiating entity configurator '" + implType.getImplName() + "'", ex); //$NON-NLS-1$ //$NON-NLS-2$
            return null;
        }
    }

    @Override
    public String toString() {
        return implType.getImplName();
    }
}
