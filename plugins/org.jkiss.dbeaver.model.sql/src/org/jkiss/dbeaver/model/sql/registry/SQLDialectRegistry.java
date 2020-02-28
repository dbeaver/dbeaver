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
package org.jkiss.dbeaver.model.sql.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SQLDialectRegistry
{
    static final String TAG_DIALECT = "dialect"; //$NON-NLS-1$

    private static SQLDialectRegistry instance = null;

    public synchronized static SQLDialectRegistry getInstance()
    {
        if (instance == null) {
            instance = new SQLDialectRegistry();
            instance.loadExtensions(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private final Map<String, SQLDialectDescriptor> dialects = new LinkedHashMap<>();

    private SQLDialectRegistry()
    {
    }

    private void loadExtensions(IExtensionRegistry registry)
    {
        IConfigurationElement[] extConfigs = registry.getConfigurationElementsFor(SQLDialectDescriptor.EXTENSION_ID);
        for (IConfigurationElement ext : extConfigs) {
            if (TAG_DIALECT.equals(ext.getName())) {
                SQLDialectDescriptor dialectDescriptor = new SQLDialectDescriptor(ext);
                this.dialects.put(dialectDescriptor.getId(), dialectDescriptor);
            }
        }

        for (IConfigurationElement ext : extConfigs) {
            if (TAG_DIALECT.equals(ext.getName())) {
                String dialectId = ext.getAttribute("id");
                String parentDialectId = ext.getAttribute("parent");
                if (!CommonUtils.isEmpty(dialectId) && !CommonUtils.isEmpty(parentDialectId)) {
                    SQLDialectDescriptor dialect = dialects.get(dialectId);
                    SQLDialectDescriptor parentDialect = dialects.get(parentDialectId);
                    if (dialect != null && parentDialect != null) {
                        dialect.setParentDialect(parentDialect);
                    }
                }
            }
        }
    }

    public void dispose()
    {
        dialects.clear();
    }

    public List<SQLDialectDescriptor> getDialects() {
        return new ArrayList<>(dialects.values());
    }

    public SQLDialectDescriptor getDialect(String id) {
        return dialects.get(id);
    }

    public List<SQLDialectDescriptor> getRootDialects() {
        List<SQLDialectDescriptor> roots = new ArrayList<>();
        for (SQLDialectDescriptor dd : dialects.values()) {
            if (dd.getParentDialect() == null) {
                roots.add(dd);
            }
        }
        return roots;
    }
}
