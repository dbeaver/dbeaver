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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DefaultProgressMonitor;
import org.jkiss.dbeaver.model.task.*;
import org.jkiss.dbeaver.registry.ProjectMetadata;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * TaskManagerImpl
 */
public class TaskManagerImpl implements DBTTaskManager {

    public static final String TEMPORARY_ID = "#temp";

    private static final Log log = Log.getLog(TaskManagerImpl.class);

    public static final String CONFIG_FILE = "tasks.json";
    public static final String TASK_STATS_FOLDER = "task-stats";
    private static Gson CONFIG_GSON = new GsonBuilder()
        .setLenient()
        .serializeNulls()
        .setPrettyPrinting()
        .create();

    static final SimpleDateFormat systemDateFormat = new SimpleDateFormat(GeneralUtils.DEFAULT_TIMESTAMP_PATTERN, Locale.ENGLISH);

    private final ProjectMetadata projectMetadata;
    private final List<TaskImpl> tasks = new ArrayList<>();
    private File statisticsFolder;

    public TaskManagerImpl(ProjectMetadata projectMetadata) {
        this.projectMetadata = projectMetadata;
        this.statisticsFolder = new File(projectMetadata.getWorkspace().getMetadataFolder(), TASK_STATS_FOLDER);

        loadConfiguration();
    }

    @NotNull
    @Override
    public DBTTaskRegistry getRegistry() {
        return TaskRegistry.getInstance();
    }

    @NotNull
    @Override
    public DBPProject getProject() {
        return projectMetadata;
    }

    @NotNull
    @Override
    public DBTTask[] getAllTasks() {
        return tasks.toArray(new DBTTask[0]);
    }

    @Nullable
    @Override
    public DBTTask getTaskById(@NotNull String id) {
        for (DBTTask task : tasks) {
            if (id.equals(task.getId())) {
                return task;
            }
        }
        return null;
    }

    @Nullable
    @Override
    public DBTTask getTaskByName(@NotNull String name) {
        for (DBTTask task : tasks) {
            if (name.equals(task.getName())) {
                return task;
            }
        }
        return null;
    }

    @NotNull
    @Override
    public DBTTaskType[] getExistingTaskTypes() {
        Set<DBTTaskType> result = new LinkedHashSet<>();
        for (DBTTask tc : tasks) {
            result.add(tc.getType());
        }
        return result.toArray(new DBTTaskType[0]);
    }

    @NotNull
    @Override
    public DBTTask[] getAllTaskByType(DBTTaskType task) {
        List<DBTTask> result = new ArrayList<>();
        for (DBTTask tc : tasks) {
            if (tc.getType() == task) {
                result.add(tc);
            }
        }
        return result.toArray(new DBTTask[0]);
    }

    @NotNull
    @Override
    public DBTTask createTask(
        @NotNull DBTTaskType taskDescriptor,
        @NotNull String label,
        @Nullable String description,
        @NotNull Map<String, Object> properties) throws DBException
    {
        if (getTaskByName(label) != null) {
            throw new DBException("Task with name '" + label + "' already exists");
        }
        Date createTime = new Date();
        String id = UUID.randomUUID().toString();
        TaskImpl task = new TaskImpl(projectMetadata, taskDescriptor, id, label, description, createTime, createTime);
        task.setProperties(properties);

        return task;
    }

    @NotNull
    @Override
    public DBTTask createTemporaryTask(@NotNull DBTTaskType type, @NotNull String label) {
        return new TaskImpl(getProject(), type, TEMPORARY_ID, label, label, new Date(), null);
    }

    @Override
    public void updateTaskConfiguration(@NotNull DBTTask task) throws DBException {
        if (task.isTemporary()) {
            return;
        }
        DBTTask prevTask = getTaskByName(task.getName());
        if (prevTask != null && prevTask != task) {
            throw new DBException("Task with name '" + task.getName() + "' already exists");
        }

        boolean newTask = false;
        synchronized (tasks) {
            if (!tasks.contains(task)) {
                tasks.add((TaskImpl) task);
                newTask = true;
            }
        }

        saveConfiguration();

        TaskRegistry.getInstance().notifyTaskListeners(
            new DBTTaskEvent(
                task,
                newTask ? DBTTaskEvent.Action.TASK_ADD : DBTTaskEvent.Action.TASK_UPDATE));
    }

    @Override
    public void deleteTaskConfiguration(@NotNull DBTTask task) {
        synchronized (tasks) {
            tasks.remove(task);
        }
        saveConfiguration();

        TaskRegistry.getInstance().notifyTaskListeners(new DBTTaskEvent(task, DBTTaskEvent.Action.TASK_REMOVE));
    }

    @NotNull
    @Override
    public File getStatisticsFolder() {
        return statisticsFolder;
    }

    @Override
    public Job runTask(@NotNull DBTTask task, @NotNull DBTTaskExecutionListener listener, @NotNull Map<String, Object> options) {
        TaskRunJob runJob = new TaskRunJob((TaskImpl) task, Locale.getDefault(), listener);
        runJob.schedule();
        return runJob;
    }

    private void loadConfiguration() {
        IFile configFile = getConfigFile(false);
        if (!configFile.exists()) {
            return;
        }
        try (InputStream is = configFile.getContents()) {
            try (Reader configReader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                Map<String, Object> jsonMap = JSONUtils.parseMap(CONFIG_GSON, configReader);

                for (Map.Entry<String, Object> taskMap : jsonMap.entrySet()) {
                    Map<String, Object> taskJSON = (Map<String, Object>) taskMap.getValue();

                    try {
                        String id = taskMap.getKey();
                        String task = JSONUtils.getString(taskJSON, "task");
                        String label = CommonUtils.toString(JSONUtils.getString(taskJSON, "label"), id);
                        String description = JSONUtils.getString(taskJSON, "description");
                        Date createTime = systemDateFormat.parse(JSONUtils.getString(taskJSON, "createTime"));
                        Date updateTime = systemDateFormat.parse(JSONUtils.getString(taskJSON, "updateTime"));
                        Map<String, Object> state = JSONUtils.getObject(taskJSON, "state");

                        DBTTaskType taskDescriptor = getRegistry().getTaskType(task);
                        if (taskDescriptor == null) {
                            log.error("Can't find task descriptor " + task);
                            continue;
                        }
                        TaskImpl taskConfig = new TaskImpl(projectMetadata, taskDescriptor, id, label, description, createTime, updateTime);
                        taskConfig.setProperties(state);

                        synchronized (tasks) {
                            tasks.add(taskConfig);
                        }
                    } catch (Exception e) {
                        log.warn("Error parsing task configuration", e);
                    }

                }
            }
        } catch (Exception e) {
            log.error("Error loading tasks configuration", e);
        }
    }

    private void saveConfiguration() {
        IProgressMonitor monitor = new NullProgressMonitor();

        IFile configFile = getConfigFile(true);
        try {
            if (configFile.exists()) {
                ContentUtils.makeFileBackup(configFile);
            }
            if (tasks.isEmpty()) {
                configFile.delete(true, false, monitor);
                return;
            }
        } catch (Exception e) {
            log.error("Error processing config file", e);
        }

        ByteArrayOutputStream dsConfigBuffer = new ByteArrayOutputStream(10000);
        try (OutputStreamWriter osw = new OutputStreamWriter(dsConfigBuffer, StandardCharsets.UTF_8)) {
            try (JsonWriter jsonWriter = CONFIG_GSON.newJsonWriter(osw)) {
                synchronized (tasks) {
                    serializeTasks(new DefaultProgressMonitor(monitor), jsonWriter);
                }
            }
        } catch (IOException e) {
            log.error(e);
            return;
        }
        InputStream ifs = new ByteArrayInputStream(dsConfigBuffer.toByteArray());

        try {
            if (!configFile.exists()) {
                configFile.create(ifs, true, monitor);
                configFile.setHidden(true);
            } else {
                configFile.setContents(ifs, true, false, monitor);
            }
        } catch (Exception e) {
            log.error("Error saving configuration to a file " + configFile.getFullPath(), e);
        }
    }

    private void serializeTasks(DBRProgressMonitor monitor, JsonWriter jsonWriter) throws IOException {
        jsonWriter.setIndent("\t");
        jsonWriter.beginObject();
        for (TaskImpl task : tasks) {
            jsonWriter.name(task.getId());
            jsonWriter.beginObject();
            JSONUtils.field(jsonWriter, "task", task.getType().getId());
            JSONUtils.field(jsonWriter, "label", task.getName());
            JSONUtils.field(jsonWriter, "description", task.getDescription());
            JSONUtils.field(jsonWriter, "createTime", systemDateFormat.format(task.getCreateTime()));
            JSONUtils.field(jsonWriter, "updateTime", systemDateFormat.format(task.getUpdateTime()));
            JSONUtils.serializeProperties(jsonWriter, "state", task.getProperties());
            jsonWriter.endObject();
        }
        jsonWriter.endObject();
    }

    private IFile getConfigFile(boolean create) {
        return projectMetadata.getMetadataFolder(create).getFile(CONFIG_FILE);
    }

}
