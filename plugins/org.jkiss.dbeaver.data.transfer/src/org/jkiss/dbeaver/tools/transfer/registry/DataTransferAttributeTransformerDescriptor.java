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

package org.jkiss.dbeaver.tools.transfer.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.impl.PropertyDescriptor;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.tools.transfer.IDataTransferAttributeTransformer;

import java.util.ArrayList;
import java.util.List;

/**
 * DataTransferAttributeTransformerDescriptor
 */
public class DataTransferAttributeTransformerDescriptor extends AbstractDescriptor {
    @NotNull
    private final String id;
    @NotNull
    private final String name;
    private final String description;
    @NotNull
    private final DBPImage icon;
    private final ObjectType implType;
    private final List<DBPPropertyDescriptor> properties = new ArrayList<>();

    public DataTransferAttributeTransformerDescriptor(IConfigurationElement config) {
        super(config);

        this.id = config.getAttribute("id");
        this.name = config.getAttribute("label");
        this.description = config.getAttribute("description");
        this.icon = iconToImage(config.getAttribute("icon"), DBIcon.TYPE_UNKNOWN);
        this.implType = new ObjectType(config.getAttribute("class"));

        for (IConfigurationElement prop : config.getChildren(PropertyDescriptor.TAG_PROPERTY_GROUP)) {
            properties.addAll(PropertyDescriptor.extractProperties(prop));
        }
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    @NotNull
    public DBPImage getIcon() {
        return icon;
    }

    @NotNull
    public List<DBPPropertyDescriptor> getProperties() {
        return properties;
    }

    public IDataTransferAttributeTransformer createTransformer() throws DBException
    {
        try {
            return implType.getObjectClass(IDataTransferAttributeTransformer.class)
                .getDeclaredConstructor().newInstance();
        } catch (Throwable e) {
            throw new DBException("Can't create attribute transformer instance", e);
        }
    }

    @Override
    public String toString() {
        return id;
    }
}
