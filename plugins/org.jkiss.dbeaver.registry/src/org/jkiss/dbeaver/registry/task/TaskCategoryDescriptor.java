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
package org.jkiss.dbeaver.registry.task;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBPNamedObjectLocalized;
import org.jkiss.dbeaver.model.impl.AbstractContextDescriptor;
import org.jkiss.dbeaver.model.task.DBTTaskCategory;
import org.jkiss.dbeaver.model.task.DBTTaskType;
import org.jkiss.dbeaver.registry.RegistryConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * TaskTypeDescriptor
 */
public class TaskCategoryDescriptor extends AbstractContextDescriptor implements DBTTaskCategory, DBPNamedObjectLocalized {

    private final IConfigurationElement config;
    private final List<TaskTypeDescriptor> tasks = new ArrayList<>();
    private TaskCategoryDescriptor parent;
    private List<TaskCategoryDescriptor> children = new ArrayList<>();

    TaskCategoryDescriptor(TaskRegistry registry, IConfigurationElement config) {
        super(config);
        this.config = config;

        String parentId = config.getAttribute(RegistryConstants.ATTR_PARENT);
        if (parentId != null) {
            parent = registry.getTaskCategory(parentId);
            if (parent != null) {
                parent.addChild(this);
            }
        }
    }

    private void addChild(TaskCategoryDescriptor child) {
        children.add(child);
    }

    void addTask(TaskTypeDescriptor task) {
        this.tasks.add(task);
    }

    @NotNull
    @Override
    public String getId() {
        return config.getAttribute(RegistryConstants.ATTR_ID);
    }

    @Nullable
    @Override
    public TaskCategoryDescriptor getParent() {
        return parent;
    }

    @NotNull
    @Override
    public DBTTaskCategory[] getChildren() {
        return children.toArray(new DBTTaskCategory[0]);
    }

    @NotNull
    @Override
    public String getName() {
        return config.getAttribute(RegistryConstants.ATTR_NAME);
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
    public DBTTaskType[] getTaskTypes() {
        return tasks.toArray(new DBTTaskType[0]);
    }

    @Override
    public String toString() {
        return getId();
    }

    @Override
    public String getLocalizedName(String locale) {
        return config.getAttribute(RegistryConstants.ATTR_NAME, locale);
    }

}
