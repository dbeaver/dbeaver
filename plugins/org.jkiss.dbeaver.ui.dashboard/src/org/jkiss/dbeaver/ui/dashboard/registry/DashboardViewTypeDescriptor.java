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
package org.jkiss.dbeaver.ui.dashboard.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.AbstractContextDescriptor;
import org.jkiss.dbeaver.ui.dashboard.control.DashboardRenderer;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardViewType;

/**
 * DashboardDescriptor
 */
public class DashboardViewTypeDescriptor extends AbstractContextDescriptor implements DashboardViewType
{
    private String id;
    private String label;
    private String description;
    private ObjectType implType;

    DashboardViewTypeDescriptor(
        IConfigurationElement config)
    {
        super(config);

        this.id = config.getAttribute("id");
        this.label = config.getAttribute("label");
        this.description = config.getAttribute("description");
        this.implType = new ObjectType(config.getAttribute("renderer"));
    }

    @Override
    @NotNull
    public String getId() {
        return id;
    }

    @Override
    @NotNull
    public String getTitle()
    {
        return label;
    }

    @Override
    public String getDescription()
    {
        return description;
    }

    @Override
    public DashboardRenderer createRenderer() throws DBException {
        return implType.createInstance(DashboardRenderer.class);
    }

    @Override
    public String toString() {
        return id;
    }

}
