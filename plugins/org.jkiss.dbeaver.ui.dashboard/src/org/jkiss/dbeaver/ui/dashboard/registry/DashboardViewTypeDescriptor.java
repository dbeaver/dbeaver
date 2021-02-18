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
package org.jkiss.dbeaver.ui.dashboard.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.impl.AbstractContextDescriptor;
import org.jkiss.dbeaver.ui.dashboard.control.DashboardRenderer;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardDataType;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardViewType;
import org.jkiss.utils.CommonUtils;

/**
 * DashboardDescriptor
 */
public class DashboardViewTypeDescriptor extends AbstractContextDescriptor implements DashboardViewType
{
    private String id;
    private String label;
    private String description;
    private DBPImage icon;
    private ObjectType implType;
    private DashboardDataType[] supportedDataTypes;

    DashboardViewTypeDescriptor(
        IConfigurationElement config)
    {
        super(config);

        this.id = config.getAttribute("id");
        this.label = config.getAttribute("label");
        this.description = config.getAttribute("description");
        this.icon = iconToImage(config.getAttribute("icon"));
        String[] dataTypeNames = CommonUtils.notEmpty(config.getAttribute("dataTypes")).split(",");
        this.supportedDataTypes = new DashboardDataType[dataTypeNames.length];
        for (int i = 0; i < dataTypeNames.length; i++) {
            this.supportedDataTypes[i] = CommonUtils.valueOf(DashboardDataType.class, dataTypeNames[i], DashboardDataType.timeseries);
        }

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
    public DBPImage getIcon() {
        return icon;
    }

    @Override
    public DashboardDataType[] getSupportedTypes() {
        return supportedDataTypes;
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
