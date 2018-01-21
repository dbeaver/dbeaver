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

package org.jkiss.dbeaver.ext.mockdata.model;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.impl.PropertyDescriptor;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.dbeaver.registry.datatype.DataTypeAbstractDescriptor;

import java.util.ArrayList;
import java.util.List;

/**
 * MockGeneratorDescriptor
 */
public class MockGeneratorDescriptor extends DataTypeAbstractDescriptor {

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.mockGenerator"; //$NON-NLS-1$

    private final String label;
    private final String description;
    private final DBPImage icon;
    private List<DBPPropertyDescriptor> properties = new ArrayList<>();

    public MockGeneratorDescriptor(IConfigurationElement config)
    {
        super(config, MockValueGenerator.class);
        this.label = config.getAttribute(RegistryConstants.ATTR_LABEL);
        this.description = config.getAttribute(RegistryConstants.ATTR_DESCRIPTION);
        this.icon = iconToImage(config.getAttribute(RegistryConstants.ATTR_ICON));

        for (IConfigurationElement prop : config.getChildren(PropertyDescriptor.TAG_PROPERTY_GROUP)) {
            properties.addAll(PropertyDescriptor.extractProperties(prop));
        }
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

    public List<DBPPropertyDescriptor> getProperties() {
        return properties;
    }

}
