/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

package org.jkiss.dbeaver.registry.formatter;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.DBDDataFormatter;
import org.jkiss.dbeaver.model.data.DBDDataFormatterSample;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.impl.PropertyDescriptor;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;

/**
 * DataFormatterDescriptor
 */
public class DataFormatterDescriptor extends AbstractDescriptor
{
    private static final Log log = Log.getLog(DataFormatterDescriptor.class);

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.dataFormatter"; //$NON-NLS-1$

    private String id;
    private String name;
    private String description;
    private DBPPropertyDescriptor[] properties;
    private DBDDataFormatterSample sample;
    private ObjectType formatterType;

    public DataFormatterDescriptor(IConfigurationElement config)
    {
        super(config);

        this.id = config.getAttribute("id");
        this.formatterType = new ObjectType(config.getAttribute("class"));
        this.name = config.getAttribute("label");
        this.description = config.getAttribute("description");
        this.properties = PropertyDescriptor.extractPropertyGroups(config);

        Class<?> objectClass = getObjectClass(config.getAttribute("sampleClass"));
        try {
            sample = (DBDDataFormatterSample)objectClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            log.error("Can't instantiate data formatter '" + getId() + "' sample");
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

    public DBDDataFormatterSample getSample()
    {
        return sample;
    }

    public DBPPropertyDescriptor[] getProperties() {
        return properties;
    }

    public DBDDataFormatter createFormatter() throws IllegalAccessException, InstantiationException
    {
        Class<? extends DBDDataFormatter> clazz = formatterType.getObjectClass(DBDDataFormatter.class);
        if (clazz == null) {
            return null;
        }
        return clazz.newInstance();
    }

}
