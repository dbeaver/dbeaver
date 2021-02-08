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
package org.jkiss.dbeaver.model.sql.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

import java.util.ArrayList;
import java.util.List;

public class SQLInsertReplaceMethodRegistry {

    private static final String TAG_METHOD = "method"; //$NON-NLS-1$

    private static SQLInsertReplaceMethodRegistry instance = null;

    public synchronized static SQLInsertReplaceMethodRegistry getInstance()
    {
        if (instance == null) {
            instance = new SQLInsertReplaceMethodRegistry();
            instance.loadExtensions(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private final List<SQLInsertReplaceMethodDescriptor> insertMethods = new ArrayList<>();

    private SQLInsertReplaceMethodRegistry()
    {
    }

    private void loadExtensions(IExtensionRegistry registry)
    {
        IConfigurationElement[] extConfigs = registry.getConfigurationElementsFor(SQLInsertReplaceMethodDescriptor.EXTENSION_ID);
        for (IConfigurationElement ext : extConfigs) {
            // Load insert methods
            if (TAG_METHOD.equals(ext.getName())) {
                this.insertMethods.add(new SQLInsertReplaceMethodDescriptor(ext));
            }
        }
    }

    public void dispose()
    {
        insertMethods.clear();
    }

    public List<SQLInsertReplaceMethodDescriptor> getInsertMethods() {
        return new ArrayList<>(insertMethods);
    }

    public SQLInsertReplaceMethodDescriptor getInsertMethod(String id) {
        for (SQLInsertReplaceMethodDescriptor method : insertMethods) {
            if (method.getId().equalsIgnoreCase(id)) {
                return method;
            }
        }
        return null;
    }

    public SQLInsertReplaceMethodDescriptor getInsertMethodOnLabel(String label) {
        for (SQLInsertReplaceMethodDescriptor method : insertMethods) {
            if (method.getLabel().equalsIgnoreCase(label)) {
                return method;
            }
        }
        return null;
    }
}
