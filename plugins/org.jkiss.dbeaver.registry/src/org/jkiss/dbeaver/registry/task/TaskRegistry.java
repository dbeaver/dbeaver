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
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.task.DBTTaskDescriptor;
import org.jkiss.dbeaver.model.task.DBTTaskRegistry;
import org.jkiss.dbeaver.model.task.DBTTaskTypeDescriptor;

import java.util.ArrayList;
import java.util.List;

public class TaskRegistry implements DBTTaskRegistry
{
    public static final String EXTENSION_ID = "org.jkiss.dbeaver.task"; //$NON-NLS-1$

    private static TaskRegistry instance = null;

    public synchronized static TaskRegistry getInstance()
    {
        if (instance == null) {
            instance = new TaskRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private final List<TaskTypeDescriptor> taskTypeDescriptors = new ArrayList<>();
    private final List<TaskDescriptor> taskDescriptors = new ArrayList<>();

    private TaskRegistry(IExtensionRegistry registry)
    {
        // Load data taskDescriptors from external plugins
        {
            IConfigurationElement[] extElements = registry.getConfigurationElementsFor(EXTENSION_ID);
            for (IConfigurationElement ext : extElements) {
                if ("type".equals(ext.getName())) {
                    TaskTypeDescriptor descriptor = new TaskTypeDescriptor(ext);
                    taskTypeDescriptors.add(descriptor);
                }
            }
            for (IConfigurationElement ext : extElements) {
                if ("task".equals(ext.getName())) {
                    String typeId = ext.getAttribute("type");
                    TaskTypeDescriptor taskType = getTaskType(typeId);
                    TaskDescriptor taskDescriptor = new TaskDescriptor(taskType, ext);
                    taskDescriptors.add(taskDescriptor);
                }
            }
        }
    }

    @Override
    public DBTTaskDescriptor[] getAllTasks() {
        return taskDescriptors.toArray(new DBTTaskDescriptor[0]);
    }

    @Override
    public DBTTaskTypeDescriptor[] getTaskTypes() {
        return taskTypeDescriptors.toArray(new DBTTaskTypeDescriptor[0]);
    }

    @Nullable
    TaskTypeDescriptor getTaskType(String id) {
        for (TaskTypeDescriptor ttd : taskTypeDescriptors) {
            if (id.equals(ttd.getId())) {
                return ttd;
            }
        }
        return null;
    }

}
