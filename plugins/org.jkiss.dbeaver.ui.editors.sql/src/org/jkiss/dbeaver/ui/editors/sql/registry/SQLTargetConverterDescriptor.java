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

package org.jkiss.dbeaver.ui.editors.sql.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.impl.AbstractContextDescriptor;
import org.jkiss.dbeaver.model.impl.PropertyDescriptor;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;

import java.util.ArrayList;
import java.util.List;

/**
 * SQLTargetConverterDescriptor
 */
public class SQLTargetConverterDescriptor extends AbstractContextDescriptor {

    private final String id;
    private final String label;
    private final String description;
    private final ObjectType implClass;
    private final DBPImage icon;
    private List<DBPPropertyDescriptor> properties = new ArrayList<>();

    SQLTargetConverterDescriptor(IConfigurationElement config) {
        super(config);

        this.id = config.getAttribute("id");
        this.label = config.getAttribute("label");
        this.description = config.getAttribute("description");
        this.implClass = new ObjectType(config.getAttribute("class"));
        this.icon = iconToImage(config.getAttribute("icon"));

        this.properties.addAll(PropertyDescriptor.extractProperties(config));
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

    public List<DBPPropertyDescriptor> getProperties() {
        return properties;
    }

    public <T> T createInstance(Class<T> type) throws DBException {
        return implClass.createInstance(type);
    }

    @Override
    public String toString() {
        return id;
    }
}
