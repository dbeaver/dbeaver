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
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.impl.AbstractContextDescriptor;
import org.jkiss.utils.CommonUtils;

/**
 * DashboardDescriptor
 */
public class DashboardDescriptor extends AbstractContextDescriptor
{
    public static final String EXTENSION_ID = "org.jkiss.dbeaver.dashboard"; //$NON-NLS-1$

    private final String id;
    private final String label;
    private final String description;
    private final String group;
    private final String[] tags;

    private final String dataSourceProvider;
    private final String driverId;
    private final String driverClass;

    public DashboardDescriptor(
        IConfigurationElement config)
    {
        super(config);

        this.id = config.getAttribute("id");
        this.label = config.getAttribute("label");
        this.description = config.getAttribute("description");
        this.group = config.getAttribute("group");
        this.tags = CommonUtils.notEmpty(config.getAttribute("tags")).split(",");

        this.dataSourceProvider = config.getAttribute("datasource");
        this.driverId = config.getAttribute("driver");
        this.driverClass = config.getAttribute("driverClass");
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public String getLabel()
    {
        return label;
    }

    public String getDescription()
    {
        return description;
    }

    public String getGroup() {
        return group;
    }

    public String[] getTags() {
        return tags;
    }

    public boolean matches(DBPDataSourceContainer dataSource) {
        if (this.dataSourceProvider != null && !this.dataSourceProvider.equals(dataSource.getDriver().getProviderId())) {
            return false;
        }
        if (this.driverId != null && !this.driverId.equals(dataSource.getDriver().getId())) {
            return false;
        }
        if (this.driverClass != null && !this.driverClass.equals(dataSource.getDriver().getDriverClassName())) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return id;
    }
}
