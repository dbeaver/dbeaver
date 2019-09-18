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
package org.jkiss.dbeaver.registry.task;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBPNamedObjectLocalized;
import org.jkiss.dbeaver.model.impl.AbstractContextDescriptor;
import org.jkiss.dbeaver.model.impl.PropertyDescriptor;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.task.DBTTaskDescriptor;
import org.jkiss.dbeaver.model.task.DBTTaskHandler;
import org.jkiss.dbeaver.model.task.DBTTaskTypeDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.utils.ArrayUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * TaskDescriptor
 */
public class TaskDescriptor extends AbstractContextDescriptor implements DBTTaskDescriptor, DBPNamedObjectLocalized {

    private final TaskTypeDescriptor type;
    private final IConfigurationElement config;
    private final ObjectType handlerImplType;
    private final DBPPropertyDescriptor[] properties;

    TaskDescriptor(TaskTypeDescriptor type, IConfigurationElement config) {
        super(config);
        this.type = type;
        this.type.addTask(this);
        this.config = config;

        this.handlerImplType = new ObjectType(config, "handler");

        List<DBPPropertyDescriptor> props = new ArrayList<>();
        for (IConfigurationElement prop : ArrayUtils.safeArray(config.getChildren(PropertyDescriptor.TAG_PROPERTY_GROUP))) {
            props.addAll(PropertyDescriptor.extractProperties(prop));
        }
        this.properties = props.toArray(new DBPPropertyDescriptor[0]);
    }

    @NotNull
    @Override
    public String getId() {
        return config.getAttribute(RegistryConstants.ATTR_ID);
    }

    @NotNull
    @Override
    public String getName() {
        return config.getAttribute(RegistryConstants.ATTR_NAME);
    }

    @Override
    public String getLocalizedName(String locale) {
        return config.getAttribute(RegistryConstants.ATTR_LABEL, locale);
    }

    @Override
    public String getDescription() {
        return config.getAttribute(RegistryConstants.ATTR_DESCRIPTION);
    }

    @Override
    public DBPImage getIcon() {
        return iconToImage(config.getAttribute(RegistryConstants.ATTR_ICON));
    }

    @NotNull
    @Override
    public DBTTaskTypeDescriptor getType() {
        return type;
    }

    @NotNull
    @Override
    public DBPPropertyDescriptor[] getConfigurationProperties() {
        return this.properties;
    }

    @NotNull
    @Override
    public Class<?>[] getInputTypes() {
        List<Class<?>> objClasses = new ArrayList<>();
        for (ObjectType objectType : getObjectTypes()) {
            Class<?> aClass = objectType.getObjectClass();
            if (aClass != null) {
                objClasses.add(aClass);
            }
        }
        return objClasses.toArray(new Class[0]);
    }

    @NotNull
    @Override
    public DBTTaskHandler createHandler() throws DBException {
        return handlerImplType.createInstance(DBTTaskHandler.class);
    }

    @Override
    public String toString() {
        return getId();
    }

}
