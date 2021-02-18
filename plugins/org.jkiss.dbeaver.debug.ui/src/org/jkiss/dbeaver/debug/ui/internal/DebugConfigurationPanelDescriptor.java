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
package org.jkiss.dbeaver.debug.ui.internal;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.debug.ui.DBGConfigurationPanel;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.impl.AbstractContextDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * DebugConfigurationPanelDescriptor
 */
public class DebugConfigurationPanelDescriptor extends AbstractContextDescriptor
{
    public static final String EXTENSION_ID = "org.jkiss.dbeaver.debug.ui.configurationPanels"; //$NON-NLS-1$

    private final String id;
    private final String name;
    private final String description;
    private final ObjectType implType;
    private List<String> supportedDataSources = new ArrayList<>();

    public DebugConfigurationPanelDescriptor(
        IConfigurationElement config)
    {
        super(config);

        this.id = config.getAttribute(RegistryConstants.ATTR_ID);
        this.name = config.getAttribute(RegistryConstants.ATTR_NAME);
        this.description = config.getAttribute(RegistryConstants.ATTR_DESCRIPTION);
        this.implType = new ObjectType(config, RegistryConstants.ATTR_CLASS);

        IConfigurationElement[] dsElements = config.getChildren(RegistryConstants.TAG_DATASOURCE);
        for (IConfigurationElement dsElement : dsElements) {
            String dsId = dsElement.getAttribute(RegistryConstants.ATTR_ID);
            String dsClassName = dsElement.getAttribute(RegistryConstants.ATTR_CLASS);
            if (dsId == null && dsClassName == null) {
                continue;
            }
            supportedDataSources.add(dsId != null ? dsId : dsClassName);
        }
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public String getName()
    {
        return name;
    }

    public String getDescription()
    {
        return description;
    }

    public boolean isValid() {
        return !CommonUtils.isEmpty(implType.getImplName());
    }

    public DBGConfigurationPanel createPanel() throws DBException {
        return implType.createInstance(DBGConfigurationPanel.class);
    }

    @Override
    public String toString() {
        return id;
    }

    public boolean supportsDataSource(DBPDataSourceContainer dataSource) {
        return supportedDataSources.contains(dataSource.getDriver().getProviderId()) ||
            supportedDataSources.contains(dataSource.getDriver().getDriverClassName());
    }

}
