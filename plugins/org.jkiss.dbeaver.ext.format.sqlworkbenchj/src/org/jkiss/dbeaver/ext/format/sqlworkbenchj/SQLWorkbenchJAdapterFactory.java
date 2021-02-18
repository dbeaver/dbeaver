/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.format.sqlworkbenchj;

import org.eclipse.core.runtime.IAdapterFactory;
import org.jkiss.dbeaver.ui.editors.sql.preferences.format.SQLFormatterConfigurator;

/**
 * Adapts objects to their UI configurators
 */
public class SQLWorkbenchJAdapterFactory implements IAdapterFactory
{
    private static final Class<?>[] ADAPTER_LIST = {
        SQLFormatterConfigurator.class
    };

    @Override
    public <T> T getAdapter(Object adaptableObject, Class<T> adapterType)
    {
        if (adapterType == SQLFormatterConfigurator.class && adaptableObject instanceof SQLWorkbenchJFormatter) {
            return adapterType.cast(new SQLWorkbenchJFormatterSettingsPage());
        }
        return null;
    }

    @Override
    public Class[] getAdapterList()
    {
        return ADAPTER_LIST;
    }
}
