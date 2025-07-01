/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.impl.AbstractContextDescriptor;
import org.jkiss.dbeaver.ui.editors.sql.SQLPlanViewProvider;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * SQLPlanViewDescriptor
 */
public class SQLPlanViewDescriptor extends AbstractContextDescriptor {

    private final String id;
    private final String label;
    private final String description;
    private final int priority;
    private final ObjectType implClass;
    private final DBPImage icon;
    private List<String> supportedDataSources = new ArrayList<>();

    SQLPlanViewDescriptor(IConfigurationElement config) {
        super(config);

        this.id = config.getAttribute("id");
        this.label = config.getAttribute("label");
        this.description = config.getAttribute("description");
        this.implClass = new ObjectType(config.getAttribute("class"));
        this.icon = iconToImage(config.getAttribute("icon"));
        this.priority = CommonUtils.toInt(config.getAttribute("priority"));
        this.supportedDataSources = getSupportedDataSources(config);
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public DBPImage getIcon() {
        return icon;
    }

    public int getPriority() {
        return priority;
    }

    public SQLPlanViewProvider createInstance() throws DBException {
        return implClass.createInstance(SQLPlanViewProvider.class);
    }

    @Override
    public String toString() {
        return id;
    }

    private List<String> getSupportedDataSources(IConfigurationElement config) {
        List<String> result = new ArrayList<>();
        IConfigurationElement[] children = config.getChildren("datasource");
        if (children != null) {
            for (IConfigurationElement dsElement : children) {
                String dsId = dsElement.getAttribute("id");
                if (!CommonUtils.isEmpty(dsId)) {
                    result.add(dsId);
                }
            }
        }
        return result;
    }

    public boolean supportedBy(@Nullable DBPDataSource dataSource) {
        if (dataSource == null) {
            return true;
        }

        if (supportedDataSources.isEmpty()) {
            return true;
        }

        String driverId = dataSource.getContainer().getDriver().getId();
        String providerId = dataSource.getContainer().getDriver().getProviderId();

        for (String dsId : supportedDataSources) {
            if (dsId.equals(driverId) || dsId.equals(providerId)) {
                return true;
            }
        }

        return false;
    }
}
