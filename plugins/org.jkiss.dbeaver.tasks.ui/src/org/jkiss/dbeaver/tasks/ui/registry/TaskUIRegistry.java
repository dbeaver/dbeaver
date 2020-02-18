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
package org.jkiss.dbeaver.tasks.ui.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.task.DBTTaskType;
import org.jkiss.dbeaver.registry.task.TaskRegistry;
import org.jkiss.dbeaver.registry.task.TaskTypeDescriptor;
import org.jkiss.dbeaver.tasks.ui.DBTTaskConfigurator;

import java.util.LinkedHashMap;
import java.util.Map;

public class TaskUIRegistry {
    public static final String TASK_EXTENSION_ID = "org.jkiss.dbeaver.task.ui"; //$NON-NLS-1$

    private static final Log log = Log.getLog(TaskUIRegistry.class);

    private static TaskUIRegistry instance = null;

    public synchronized static TaskUIRegistry getInstance() {
        if (instance == null) {
            instance = new TaskUIRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private final Map<DBTTaskType, TaskConfiguratorDescriptor> taskConfigurators = new LinkedHashMap<>();

    private TaskUIRegistry(IExtensionRegistry registry) {
        IConfigurationElement[] extElements = registry.getConfigurationElementsFor(TASK_EXTENSION_ID);

        for (IConfigurationElement ext : extElements) {
            if ("configurator".equals(ext.getName())) {
                String typeId = ext.getAttribute("type");
                TaskTypeDescriptor taskType = TaskRegistry.getInstance().getTaskType(typeId);
                if (taskType == null) {
                    log.debug("Task type '" + typeId + "' not found. Skip configurator.");
                } else {
                    TaskConfiguratorDescriptor configDescriptor = new TaskConfiguratorDescriptor(taskType, ext);
                    taskConfigurators.put(taskType, configDescriptor);
                }
            }
        }
    }

    public boolean supportsConfigurator(DBTTaskType taskType) {
        return taskConfigurators.containsKey(taskType);
    }

    public DBTTaskConfigurator createConfigurator(DBTTaskType taskType) throws DBCException {
        TaskConfiguratorDescriptor configuratorDescriptor = taskConfigurators.get(taskType);
        if (configuratorDescriptor == null) {
            throw new DBCException("Task configurator not supported for " + taskType.getName());
        }
        try {
            return configuratorDescriptor.createConfigurator();
        } catch (DBException e) {
            throw new DBCException("Task configurator create error", e);
        }
    }

}
