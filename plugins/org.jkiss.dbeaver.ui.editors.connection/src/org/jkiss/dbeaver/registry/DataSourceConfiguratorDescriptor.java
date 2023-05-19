/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

package org.jkiss.dbeaver.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * DataSourceViewDescriptor
 */
public class DataSourceConfiguratorDescriptor extends AbstractDescriptor {
    public static final String EXTENSION_ID = "org.jkiss.dbeaver.ui.dataSourceConfigurator"; //$NON-NLS-1$

    private final String id;
    private final List<DataSourcePageDescriptor> pages = new ArrayList<>();

    public DataSourceConfiguratorDescriptor(IConfigurationElement config) {
        super(config.getContributor().getName());
        this.id = config.getAttribute(RegistryConstants.ATTR_ID);
        for (IConfigurationElement pageCfg : config.getChildren("dataSourcePage")) {
            pages.add(new DataSourcePageDescriptor(pageCfg));
        }
    }

    public String getId() {
        return id;
    }

    public List<DataSourcePageDescriptor> getAllPages() {
        return pages;
    }

    public List<DataSourcePageDescriptor> getRootPages(DBPDataSourceContainer dataSource) {
        List<DataSourcePageDescriptor> roots = new ArrayList<>();
        for (DataSourcePageDescriptor page : pages) {
            if (CommonUtils.isEmpty(page.getParentId()) && page.appliesTo(dataSource)) {
                roots.add(page);
            }
        }
        return roots;
    }

    public List<DataSourcePageDescriptor> getChildPages(DBPDataSourceContainer dataSource, String parentId) {
        List<DataSourcePageDescriptor> children = new ArrayList<>();
        for (DataSourcePageDescriptor page : pages) {
            if (parentId.equals(page.getParentId()) && page.appliesTo(dataSource)) {
                children.add(page);
            }
        }
        return children;
    }

    @Override
    public String toString() {
        return id;
    }

    public boolean appliesTo(DBPDataSourceContainer dataSourceContainer) {
        return true;
    }
}
