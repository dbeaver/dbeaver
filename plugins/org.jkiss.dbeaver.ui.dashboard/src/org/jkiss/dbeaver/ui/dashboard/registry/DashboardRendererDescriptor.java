/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.dashboard.DBDashboardDataType;
import org.jkiss.dbeaver.model.dashboard.registry.DashboardItemConfiguration;
import org.jkiss.dbeaver.model.impl.AbstractContextDescriptor;
import org.jkiss.dbeaver.ui.IObjectPropertyConfigurator;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardItemRenderer;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardItemViewSettings;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardRendererType;
import org.jkiss.utils.CommonUtils;

/**
 * DashboardDescriptor
 */
public class DashboardRendererDescriptor extends AbstractContextDescriptor implements DashboardRendererType
{
    public static final String EXTENSION_ID = "org.jkiss.dbeaver.dashboard.ui";
    private final String id;
    private final String label;
    private final String description;
    private final DBPImage icon;
    private final ObjectType implType;
    private final ObjectType itemConfigurationEditor;
    private final ObjectType itemViewSettingsEditor;
    private final DBDashboardDataType[] supportedDataTypes;
    private final boolean nativeRenderer;

    DashboardRendererDescriptor(
        IConfigurationElement config)
    {
        super(config);

        this.id = config.getAttribute("id");
        this.label = config.getAttribute("label");
        this.description = config.getAttribute("description");
        this.icon = iconToImage(config.getAttribute("icon"));
        String[] dataTypeNames = CommonUtils.notEmpty(config.getAttribute("dataTypes")).split(",");
        this.supportedDataTypes = new DBDashboardDataType[dataTypeNames.length];
        for (int i = 0; i < dataTypeNames.length; i++) {
            this.supportedDataTypes[i] = CommonUtils.valueOf(DBDashboardDataType.class, dataTypeNames[i], DBDashboardDataType.timeseries);
        }
        this.nativeRenderer = CommonUtils.toBoolean(config.getAttribute("native"));

        this.implType = new ObjectType(config.getAttribute("renderer"));
        this.itemConfigurationEditor = new ObjectType(config.getAttribute("configurationEditor"));
        this.itemViewSettingsEditor = new ObjectType(config.getAttribute("viewSettingsEditor"));
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
    public DBDashboardDataType[] getSupportedTypes() {
        return supportedDataTypes;
    }

    public boolean isNativeRenderer() {
        return nativeRenderer;
    }

    @Override
    public DashboardItemRenderer createRenderer() throws DBException {
        return implType.createInstance(DashboardItemRenderer.class);
    }

    @Override
    public IObjectPropertyConfigurator<DashboardItemConfiguration, DashboardItemConfiguration> createItemConfigurationEditor() throws DBException {
        return itemConfigurationEditor.createInstance(IObjectPropertyConfigurator.class);
    }

    @Override
    public IObjectPropertyConfigurator<DashboardItemViewSettings, DashboardItemViewSettings> createItemViewSettingsEditor() throws DBException {
        return itemViewSettingsEditor.createInstance(IObjectPropertyConfigurator.class);
    }

    @Override
    public String toString() {
        return id;
    }

}
