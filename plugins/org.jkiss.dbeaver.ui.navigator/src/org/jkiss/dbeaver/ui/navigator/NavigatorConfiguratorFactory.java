/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.navigator;

import org.eclipse.core.runtime.IAdapterFactory;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.editors.entity.EntityConfiguratorDescriptor;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditorsRegistry;

/**
 * Adapts objects to their UI configurators
 */
public class NavigatorConfiguratorFactory implements IAdapterFactory
{
    private static final Class<?>[] ADAPTER_LIST = {
        DBEObjectConfigurator.class
    };

    @Override
    public <T> T getAdapter(Object adaptableObject, Class<T> adapterType)
    {
        if (adapterType == DBEObjectConfigurator.class && adaptableObject instanceof DBSObject) {
            EntityConfiguratorDescriptor configurator = EntityEditorsRegistry.getInstance().getEntityConfigurator((DBSObject) adaptableObject);
            return configurator == null ? null : adapterType.cast(configurator.createConfigurator());
        }
        return null;
    }

    @Override
    public Class[] getAdapterList()
    {
        return ADAPTER_LIST;
    }
}