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

package org.jkiss.dbeaver.ext.postgresql.model.fdw;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;

import java.util.ArrayList;
import java.util.List;

/**
 * DataTransferRegistry
 */
public class FDWConfigRegistry {

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.postgresql.fdw.config"; //$NON-NLS-1$

    private static FDWConfigRegistry instance = null;

    private static final Log log = Log.getLog(FDWConfigRegistry.class);

    public synchronized static FDWConfigRegistry getInstance()
    {
        if (instance == null) {
            instance = new FDWConfigRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private List<FDWConfigDescriptor> configDescriptors = new ArrayList<>();

    private FDWConfigRegistry(IExtensionRegistry registry)
    {
        // Load datasource providers from external plugins
        IConfigurationElement[] extElements = registry.getConfigurationElementsFor(EXTENSION_ID);
        for (IConfigurationElement ext : extElements) {
            // Load main configDescriptors
            if ("fdw".equals(ext.getName())) {
                configDescriptors.add(new FDWConfigDescriptor(ext));
            }
        }
    }

    public List<FDWConfigDescriptor> getConfigDescriptors() {
        return configDescriptors;
    }

    public FDWConfigDescriptor findFirstMatch(DBPDataSourceContainer dataSource) {
        for (FDWConfigDescriptor desc : configDescriptors) {
            if (desc.matches(dataSource)) {
                return desc;
            }
        }
        return null;
    }

}
