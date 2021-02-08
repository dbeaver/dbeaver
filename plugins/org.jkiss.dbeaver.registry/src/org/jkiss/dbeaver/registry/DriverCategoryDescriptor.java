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

package org.jkiss.dbeaver.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.utils.CommonUtils;

/**
 * DriverCategoryDescriptor
 */
public class DriverCategoryDescriptor extends AbstractDescriptor
{
    private String id;
    private String name;
    private String description;
    private DBPImage icon;
    private boolean promoted;
    private int rank;

    public DriverCategoryDescriptor(IConfigurationElement config)
    {
        super(config.getContributor().getName());
        this.id = config.getAttribute(RegistryConstants.ATTR_ID);
        this.name = config.getAttribute(RegistryConstants.ATTR_NAME);
        this.description = config.getAttribute(RegistryConstants.ATTR_DESCRIPTION);
        this.icon = iconToImage(config.getAttribute(RegistryConstants.ATTR_ICON));
        this.promoted = CommonUtils.toBoolean(config.getAttribute(RegistryConstants.ATTR_PROMOTED));
        this.rank = CommonUtils.toInt(config.getAttribute("rank"));
    }

    public String getId()
    {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public DBPImage getIcon() {
        return icon;
    }

    public boolean isPromoted() {
        return promoted;
    }

    public int getRank() {
        return rank;
    }

    @Override
    public String toString() {
        return id;
    }
}
