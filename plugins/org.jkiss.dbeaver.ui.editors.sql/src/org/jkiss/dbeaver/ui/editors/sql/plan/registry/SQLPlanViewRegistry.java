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

package org.jkiss.dbeaver.ui.editors.sql.plan.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * SQLPlanViewRegistry
 */
public class SQLPlanViewRegistry {

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.sql.plan.view"; //NON-NLS-1 //$NON-NLS-1$

    private static final String TAG_VIEW = "view"; //NON-NLS-1

    private static SQLPlanViewRegistry instance;

    public synchronized static SQLPlanViewRegistry getInstance() {
        if (instance == null) {
            instance = new SQLPlanViewRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private List<SQLPlanViewDescriptor> planViewDescriptors = new ArrayList<>();

    private SQLPlanViewRegistry(IExtensionRegistry registry)
    {
        // Load target converters
        IConfigurationElement[] panelElements = registry.getConfigurationElementsFor(EXTENSION_ID);
        for (IConfigurationElement ext : panelElements) {
            if (TAG_VIEW.equals(ext.getName())) {
                SQLPlanViewDescriptor descriptor = new SQLPlanViewDescriptor(ext);
                planViewDescriptors.add(descriptor);
            }
        }
    }

    @NotNull
    public List<SQLPlanViewDescriptor> getPlanViewDescriptors() {
        List<SQLPlanViewDescriptor> result = new ArrayList<>(planViewDescriptors);
        result.sort(Comparator.comparingInt(SQLPlanViewDescriptor::getPriority));
        return result;
    }

    @Nullable
    public SQLPlanViewDescriptor getPlanViewDescriptor(String id) {
        for (SQLPlanViewDescriptor converter : planViewDescriptors) {
            if (converter.getId().equals(id)) {
                return converter;
            }
        }
        return null;
    }

}
