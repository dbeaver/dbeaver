/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.impl;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPConfigurationElement;

public class ConfigurationElementAdapter implements DBPConfigurationElement {
    private final IConfigurationElement ice;

    public ConfigurationElementAdapter(@NotNull IConfigurationElement ice) {
        this.ice = ice;
    }

    @Nullable
    @Override
    public String getAttribute(@NotNull String name) {
        return ice.getAttribute(name);
    }

    @NotNull
    @Override
    public String getName() {
        return ice.getName();
    }

    @Nullable
    @Override
    public String getValue() {
        return ice.getValue();
    }

    @NotNull
    @Override
    public DBPConfigurationElement[] getChildren() {
        IConfigurationElement[] iChildren = ice.getChildren();
        DBPConfigurationElement[] children = new DBPConfigurationElement[iChildren.length];
        for (int i = 0; i < iChildren.length; i++) {
            children[i] = new ConfigurationElementAdapter(iChildren[i]);
        }
        return children;
    }

    @NotNull
    @Override
    public DBPConfigurationElement[] getChildren(@NotNull String name) {
        IConfigurationElement[] iChildren = ice.getChildren(name);
        DBPConfigurationElement[] children = new DBPConfigurationElement[iChildren.length];
        for (int i = 0; i < iChildren.length; i++) {
            children[i] = new ConfigurationElementAdapter(iChildren[i]);
        }
        return children;
    }

    @NotNull
    @Override
    public String getContributorName() {
        return ice.getContributor().getName();
    }
}
