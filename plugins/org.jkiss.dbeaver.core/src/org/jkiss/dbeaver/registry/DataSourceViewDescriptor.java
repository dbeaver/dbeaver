/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;

import java.util.Arrays;
import java.util.List;

/**
 * DataSourceViewDescriptor
 */
public class DataSourceViewDescriptor extends AbstractDescriptor
{
    private String id;
    private String targetID;
    private List<String> dataSourceIds;
    private String label;
    private ObjectType viewType;
    private DBPImage icon;

    public DataSourceViewDescriptor(IConfigurationElement config)
    {
        super(config.getContributor().getName());
        this.id = config.getAttribute(RegistryConstants.ATTR_ID);
        this.targetID = config.getAttribute(RegistryConstants.ATTR_TARGET_ID);
        this.dataSourceIds = Arrays.asList(config.getAttribute(RegistryConstants.ATTR_DATA_SOURCE).split(","));
        this.label = config.getAttribute(RegistryConstants.ATTR_LABEL);
        this.viewType = new ObjectType(config.getAttribute(RegistryConstants.ATTR_CLASS));
        this.icon = iconToImage(config.getAttribute(RegistryConstants.ATTR_ICON));
    }

    public String getId()
    {
        return id;
    }

    public String getTargetID()
    {
        return targetID;
    }

    public List<String> getDataSources() {
        return dataSourceIds;
    }

    public String getLabel()
    {
        return label;
    }

    public DBPImage getIcon()
    {
        return icon;
    }

    public <T> T createView(Class<T> implementsClass)
    {
        try {
            return viewType.createInstance(implementsClass);
        }
        catch (Throwable ex) {
            throw new IllegalStateException("Can't create view '" + viewType.getImplName() + "'", ex);
        }
    }

    @Override
    public String toString() {
        return id;
    }
}
