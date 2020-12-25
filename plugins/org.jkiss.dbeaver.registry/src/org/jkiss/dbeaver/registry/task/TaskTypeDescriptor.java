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
package org.jkiss.dbeaver.registry.task;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBPNamedObjectLocalized;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.impl.PropertyDescriptor;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.struct.DBSEntityElement;
import org.jkiss.dbeaver.model.task.DBTTaskCategory;
import org.jkiss.dbeaver.model.task.DBTTaskHandler;
import org.jkiss.dbeaver.model.task.DBTTaskType;
import org.jkiss.dbeaver.registry.DataSourceBindingDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * TaskTypeDescriptor
 */
public class TaskTypeDescriptor extends DataSourceBindingDescriptor implements DBTTaskType, DBPNamedObjectLocalized {

    private final TaskCategoryDescriptor category;
    private final IConfigurationElement config;
    private final ObjectType handlerImplType;
    private final DBPPropertyDescriptor[] properties;
    private Boolean matchesEntityElements;

    TaskTypeDescriptor(TaskCategoryDescriptor category, IConfigurationElement config) {
        super(config);
        this.category = category;
        this.category.addTask(this);
        this.config = config;

        this.handlerImplType = new ObjectType(config, "handler");

        this.properties = PropertyDescriptor.extractPropertyGroups(config);
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
    public DBTTaskCategory getCategory() {
        return category;
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

    @Override
    public boolean supportsVariables() {
        return CommonUtils.toBoolean(config.getAttribute("supportsVariables"));
    }

    @NotNull
    @Override
    public DBTTaskHandler createHandler() throws DBException {
        return handlerImplType.createInstance(DBTTaskHandler.class);
    }

    @Override
    public Class<? extends DBTTaskHandler> getHandlerClass() {
        return handlerImplType.getObjectClass(DBTTaskHandler.class);
    }

    @Override
    public boolean isObjectApplicable(Object object) {
        return object instanceof DBPObject && appliesTo((DBPObject) object);
    }

    public synchronized boolean matchesEntityElements() {
        if (matchesEntityElements != null) {
            return matchesEntityElements;
        }
        for (ObjectType ot : getObjectTypes()) {
            if (DBSEntityElement.class.isAssignableFrom(ot.getObjectClass())) {
                matchesEntityElements = true;
                break;
            }
        }
        if (matchesEntityElements == null) {
            matchesEntityElements = false;
        }
        return matchesEntityElements;
    }

    @Override
    public String toString() {
        return getId();
    }

}
