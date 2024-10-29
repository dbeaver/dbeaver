/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPPlatformEventManager;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.task.*;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TaskRegistry implements DBTTaskRegistry
{
    public static final String TASK_EXTENSION_ID = "org.jkiss.dbeaver.task"; //$NON-NLS-1$

    private static final Log log = Log.getLog(TaskRegistry.class);

    private static TaskRegistry instance = null;

    public synchronized static TaskRegistry getInstance()
    {
        if (instance == null) {
            instance = new TaskRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private final Map<String, TaskCategoryDescriptor> taskCategories = new LinkedHashMap<>();
    private final Map<String, TaskTypeDescriptor> taskDescriptors = new LinkedHashMap<>();
    private final List<DBTTaskListener> taskListeners = new ArrayList<>();
    private final List<SchedulerDescriptor> schedulers = new ArrayList<>();

    private TaskRegistry(IExtensionRegistry registry)
    {
        // Load data taskDescriptors from external plugins
        {
            IConfigurationElement[] extElements = registry.getConfigurationElementsFor(TASK_EXTENSION_ID);
            for (IConfigurationElement ext : extElements) {
                if ("category".equals(ext.getName())) {
                    TaskCategoryDescriptor descriptor = new TaskCategoryDescriptor(this, ext);
                    taskCategories.put(descriptor.getId(), descriptor);
                }
            }
            for (IConfigurationElement ext : extElements) {
                if ("task".equals(ext.getName())) {
                    String typeId = ext.getAttribute("type");
                    TaskCategoryDescriptor taskType = getTaskCategory(typeId);
                    TaskTypeDescriptor taskDescriptor = new TaskTypeDescriptor(taskType, ext);
                    taskDescriptors.put(taskDescriptor.getId(), taskDescriptor);
                }
            }

            for (IConfigurationElement ext : extElements) {
                if ("scheduler".equals(ext.getName())) {
                    SchedulerDescriptor descriptor = new SchedulerDescriptor(ext);
                    schedulers.add(descriptor);
                }
            }
        }
        if (DBWorkbench.getPlatform().getApplication().isMultiuser()) {
            return;
        }
        if (DBWorkbench.getPlatform() instanceof DBPPlatformEventManager eventManager) {
            eventManager.getGlobalEventManager().addEventListener((eventId, properties) -> {
                if (eventId.equals(EVENT_TASK_EXECUTE)) {
                    String projectName = CommonUtils.toString(properties.get(EVENT_PARAM_PROJECT));
                    String taskId = CommonUtils.toString(properties.get(EVENT_PARAM_TASK));
                    DBPProject project = DBWorkbench.getPlatform().getWorkspace().getProject(projectName);
                    if (project != null) {
                        DBTTask task = project.getTaskManager().getTaskById(taskId);
                        if (task != null) {
                            task.refreshRunStatistics();
                        }
                        DBTTaskEvent event = new DBTTaskEvent(task, DBTTaskEvent.Action.TASK_EXECUTE);
                        notifyTaskListeners(event);
                    }
                }
                if (eventId.equals(EVENT_BEFORE_PROJECT_DELETE)) {
                    final String projectName = CommonUtils.toString(properties.get(EVENT_PARAM_PROJECT));
                    final DBPProject project = DBWorkbench.getPlatform().getWorkspace().getProject(projectName);
                    if (project != null) {
                        final DBTTaskManager manager = project.getTaskManager();
                        for (DBTTask task : manager.getAllTasks()) {
                            try {
                                manager.deleteTaskConfiguration(task);
                            } catch (DBException e) {
                                log.warn("Can't delete configuration for task: " + task.getName());
                            }
                        }
                    }
                }
            });
        }
    }

    @NotNull
    @Override
    public DBTTaskType[] getAllTaskTypes() {
        return taskDescriptors.values().toArray(new DBTTaskType[0]);
    }

    @Nullable
    @Override
    public TaskTypeDescriptor getTaskType(String id) {
        return taskDescriptors.get(id);
    }

    @NotNull
    @Override
    public DBTTaskCategory[] getAllCategories() {
        return taskCategories.values().toArray(new DBTTaskCategory[0]);
    }

    @NotNull
    @Override
    public DBTTaskCategory[] getRootCategories() {
        List<DBTTaskCategory> result = new ArrayList<>();
        for (TaskCategoryDescriptor cat : taskCategories.values()) {
            if (cat.getParent() == null) {
                result.add(cat);
            }
        }
        return result.toArray(new DBTTaskCategory[0]);
    }

    @NotNull
    @Override
    public DBTSchedulerDescriptor[] getAllSchedulers() {
        return schedulers.toArray(new DBTSchedulerDescriptor[0]);
    }

    @Override
    public DBTSchedulerDescriptor getActiveScheduler() {
        return schedulers.stream()
            .filter(SchedulerDescriptor::isEnabled)
            .findFirst().orElse(null);
    }

    @Nullable
    public DBTScheduler getActiveSchedulerInstance() {
        DBTSchedulerDescriptor activeScheduler = getActiveScheduler();
        if (activeScheduler != null) {
            try {
                return activeScheduler.getInstance();
            } catch (DBException e) {
                log.error(e);
            }
        }
        return null;
    }

    @Override
    public void addTaskListener(DBTTaskListener listener) {
        synchronized (taskListeners) {
            taskListeners.add(listener);
        }
    }

    @Override
    public void removeTaskListener(DBTTaskListener listener) {
        synchronized (taskListeners) {
            if (!taskListeners.remove(listener)) {
                log.debug("Task listener " + listener + " not found");
            }
        }
    }

    void notifyTaskListeners(DBTTaskEvent event) {
        DBTTaskListener[] listenersCopy;
        synchronized (taskListeners) {
            listenersCopy = taskListeners.toArray(new DBTTaskListener[0]);
        }
        for (DBTTaskListener listener : listenersCopy) {
            listener.handleTaskEvent(event);
        }
    }

    public void notifyTaskFoldersListeners(DBTTaskFolderEvent event) {
        DBTTaskListener[] listenersCopy;
        synchronized (taskListeners) {
            listenersCopy = taskListeners.toArray(new DBTTaskListener[0]);
        }
        for (DBTTaskListener listener : listenersCopy) {
            listener.handleTaskFolderEvent(event);
        }
    }

    @Nullable
    TaskCategoryDescriptor getTaskCategory(String id) {
        return taskCategories.get(id);
    }

}
