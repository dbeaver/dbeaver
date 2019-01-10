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
package org.jkiss.dbeaver.registry.datatype;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.data.DBDAttributeTransformer;
import org.jkiss.dbeaver.model.data.DBDAttributeTransformerDescriptor;
import org.jkiss.dbeaver.model.impl.PropertyDescriptor;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * AttributeTransformerDescriptor
 */
public class AttributeTransformerDescriptor extends DataTypeAbstractDescriptor<DBDAttributeTransformer> implements DBDAttributeTransformerDescriptor
{
    private final String name;
    private final String description;
    private boolean applyByDefault;
    private boolean custom;
    private final DBPImage icon;
    private List<DBPPropertyDescriptor> properties = new ArrayList<>();

    public AttributeTransformerDescriptor(IConfigurationElement config)
    {
        super(config, DBDAttributeTransformer.class);

        this.name = config.getAttribute(RegistryConstants.ATTR_NAME);
        this.description = config.getAttribute(RegistryConstants.ATTR_DESCRIPTION);
        this.applyByDefault = "true".equals(config.getAttribute("applyByDefault"));
        this.custom = "true".equals(config.getAttribute("custom"));
        this.icon = iconToImage(config.getAttribute(RegistryConstants.ATTR_ICON));

        for (IConfigurationElement prop : config.getChildren(PropertyDescriptor.TAG_PROPERTY_GROUP)) {
            properties.addAll(PropertyDescriptor.extractProperties(prop));
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public boolean isApplicableByDefault() {
        return applyByDefault;
    }

    @Override
    public boolean isCustom() {
        return custom;
    }

    @Override
    public List<DBPPropertyDescriptor> getProperties() {
        return properties;
    }

    @Override
    public DBPImage getIcon() {
        return icon;
    }

}