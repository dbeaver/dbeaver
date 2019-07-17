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

package org.jkiss.dbeaver.runtime.utils;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.DBDDataFormatter;
import org.jkiss.dbeaver.model.impl.AbstractContextDescriptor;
import org.jkiss.dbeaver.model.impl.PropertyDescriptor;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;

import java.util.ArrayList;
import java.util.List;

/**
 * DataFormatterDescriptor
 */
public class UtilityTaskDescriptor extends AbstractContextDescriptor
{
    private static final Log log = Log.getLog(UtilityTaskDescriptor.class);

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.utils"; //$NON-NLS-1$

    private String id;
    private String name;
    private String description;
    private List<DBPPropertyDescriptor> properties = new ArrayList<>();
    private ObjectType utilityType;

    UtilityTaskDescriptor(IConfigurationElement config)
    {
        super(config);

        this.id = config.getAttribute("id");
        this.utilityType = new ObjectType(config.getAttribute("class"));
        this.name = config.getAttribute("label");
        this.description = config.getAttribute("description");

        IConfigurationElement[] propElements = config.getChildren(PropertyDescriptor.TAG_PROPERTY_GROUP);
        for (IConfigurationElement prop : propElements) {
            properties.addAll(PropertyDescriptor.extractProperties(prop));
        }
    }

    public String getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public String getDescription()
    {
        return description;
    }

    public List<DBPPropertyDescriptor> getProperties() {
        return properties;
    }

    public DBDDataFormatter createFormatter() throws IllegalAccessException, InstantiationException
    {
        Class<? extends DBDDataFormatter> clazz = utilityType.getObjectClass(DBDDataFormatter.class);
        if (clazz == null) {
            return null;
        }
        return clazz.newInstance();
    }

}
