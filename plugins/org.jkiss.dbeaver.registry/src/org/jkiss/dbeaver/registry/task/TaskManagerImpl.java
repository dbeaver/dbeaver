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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DefaultProgressMonitor;
import org.jkiss.dbeaver.model.task.*;
import org.jkiss.dbeaver.registry.ProjectMetadata;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

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
    public static final String TASKS_FOLDERS_TAG = "##tasksFolders";
    private static Gson CONFIG_GSON = new GsonBuilder()
        .setLenient()
        .serializeNulls()
        .setPrettyPrinting()
        .create();

    static final SimpleDateFormat systemDateFormat = new SimpleDateFormat(GeneralUtils.DEFAULT_TIMESTAMP_PATTERN, Locale.ENGLISH);

    private final ProjectMetadata projectMetadata;
    private final List<TaskImpl> tasks = new ArrayList<>();
    private final List<TaskFolderImpl> tasksFolders = new ArrayList<>();
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
            if (name.equalsIgnoreCase(task.getName())) {
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

    public DBTTaskFolder[] getTasksFolders() {
        return tasksFolders.toArray(new DBTTaskFolder[0]);
    }

    @NotNull
    @Override
    public DBTTask createTask(
        @NotNull DBTTaskType taskDescriptor,
        @NotNull String label,
        @Nullable String description,
        @Nullable String taskFolderName,
        @NotNull Map<String, Object> properties) throws DBException
    {
        if (getTaskByName(label) != null) {
            throw new DBException("Task with name '" + label + "' already exists");
        }
        Date createTime = new Date();
        String id = UUID.randomUUID().toString();
        TaskFolderImpl taskFolder = searchTaskFolderByName(taskFolderName);
        TaskImpl task = new TaskImpl(projectMetadata, taskDescriptor, id, label, description, createTime, createTime, taskFolder);
        task.setProperties(properties);

        return task;
    }

    @NotNull
    @Override
    public DBTTask createTemporaryTask(@NotNull DBTTaskType type, @NotNull String label) {
        return new TaskImpl(getProject(), type, TEMPORARY_ID, label, label, new Date(), null, null);
    }

    @NotNull
    @Override
    public DBTTaskFolder createTaskFolder(@NotNull DBPProject project, @NotNull String folderName, @Nullable DBTTask[] folderTasks) throws DBException {
        if (!CommonUtils.isEmpty(tasksFolders) && tasksFolders.stream().anyMatch(taskFolder -> taskFolder.getName().equals(folderName))) {
            throw new DBException("Task folder with name '" + folderName + "' already exists");
        }
        TaskFolderImpl taskFolder = new TaskFolderImpl(folderName, project, folderTasks != null ? new ArrayList<>(Arrays.asList(folderTasks)) : new ArrayList<>());
        synchronized (tasksFolders) {
            tasksFolders.add(taskFolder);
        }

        TaskRegistry.getInstance().notifyTaskFoldersListeners(new DBTTaskFolderEvent(taskFolder, DBTTaskFolderEvent.Action.TASK_FOLDER_ADD));
        return taskFolder;
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

        TaskRegistry.getInstance().notifyTaskListeners(
            new DBTTaskEvent(
                task,
                newTask ? DBTTaskEvent.Action.TASK_ADD : DBTTaskEvent.Action.TASK_UPDATE));

        if (task.getTaskFolder() != null) {
            TaskRegistry.getInstance().notifyTaskFoldersListeners(new DBTTaskFolderEvent(task.getTaskFolder(), DBTTaskFolderEvent.Action.TASK_FOLDER_UPDATE));
        }

        saveConfiguration();
    }

    @Override
    public void deleteTaskConfiguration(@NotNull DBTTask task) throws DBException {
        DBTScheduler scheduler = TaskRegistry.getInstance().getActiveSchedulerInstance();
        if (scheduler != null) {
            DBTTaskScheduleInfo info = scheduler.getScheduledTaskInfo(task);
            if (info != null) {
                scheduler.removeTaskSchedule(task, info);
            }
        }
        synchronized (tasks) {
            tasks.remove(task);
        }
        saveConfiguration();

        TaskRegistry.getInstance().notifyTaskListeners(new DBTTaskEvent(task, DBTTaskEvent.Action.TASK_REMOVE));
    }

    @Override
    public void removeTaskFolder(@NotNull DBTTaskFolder taskFolder) throws DBException {
        if (!tasksFolders.contains(taskFolder)) {
            throw new DBException("Task folder with name '" + taskFolder.getName() + "' is missing");
        }

        // Remove empty task folder or make task folder empty and then remove it
        List<DBTTask> folderTasks = taskFolder.getTasks();
        if (!CommonUtils.isEmpty(folderTasks)) {
            for (DBTTask task : folderTasks) {
                if (task instanceof TaskImpl) {
                    ((TaskImpl)task).setTaskFolder(null);
                }
            }
        }

        synchronized (tasksFolders) {
            tasksFolders.remove(taskFolder);
        }
        saveConfiguration();

        TaskRegistry.getInstance().notifyTaskFoldersListeners(new DBTTaskFolderEvent(taskFolder, DBTTaskFolderEvent.Action.TASK_FOLDER_REMOVE));
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
        File configFile = getConfigFile(false);
        if (!configFile.exists()) {
            return;
        }
        try (InputStream is = new FileInputStream(configFile)) {
            try (Reader configReader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                Map<String, Object> jsonMap = JSONUtils.parseMap(CONFIG_GSON, configReader);

                // First read and create folders
                for (Map.Entry<String, Map<String, Object>> folderMap : JSONUtils.getNestedObjects(jsonMap, TASKS_FOLDERS_TAG)) {
                    String taskName = folderMap.getKey();
                    if (CommonUtils.isNotEmpty(taskName)) {
                        createTaskFolder(projectMetadata, taskName, new DBTTask[0]);
                    }
                }

                for (Map.Entry<String, Object> taskMap : jsonMap.entrySet()) {
                    Map<String, Object> taskJSON = (Map<String, Object>) taskMap.getValue();

                    try {
                        String id = taskMap.getKey();
                        if (!id.startsWith(TASKS_FOLDERS_TAG)) {
                            String task = JSONUtils.getString(taskJSON, "task");
                            String label = CommonUtils.toString(JSONUtils.getString(taskJSON, "label"), id);
                            String description = JSONUtils.getString(taskJSON, "description");
                            String taskFolderName = JSONUtils.getString(taskJSON, "taskFolder");
                            Date createTime = systemDateFormat.parse(JSONUtils.getString(taskJSON, "createTime"));
                            Date updateTime = systemDateFormat.parse(JSONUtils.getString(taskJSON, "updateTime"));
                            Map<String, Object> state = JSONUtils.getObject(taskJSON, "state");

                            DBTTaskType taskDescriptor = getRegistry().getTaskType(task);
                            if (taskDescriptor == null) {
                                log.error("Can't find task descriptor " + task);
                                continue;
                            }

                            TaskFolderImpl taskFolder = searchTaskFolderByName(taskFolderName);
                            TaskImpl taskConfig = new TaskImpl(projectMetadata, taskDescriptor, id, label, description, createTime, updateTime, taskFolder);
                            taskConfig.setProperties(state);
                            if (taskFolder != null) {
                                taskFolder.addTaskToFolder(taskConfig);
                                if (!tasksFolders.contains(taskFolder)) {
                                    synchronized (tasksFolders) {
                                        tasksFolders.add(taskFolder);
                                    }
                                }
                            }

                            synchronized (tasks) {
                                tasks.add(taskConfig);
                            }
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

    private TaskFolderImpl searchTaskFolderByName(String taskFolderName) {
        TaskFolderImpl taskFolder = null;
        if (CommonUtils.isNotEmpty(taskFolderName)) {
            taskFolder = DBUtils.findObject(tasksFolders, taskFolderName);
            if (taskFolder == null) {
                taskFolder = new TaskFolderImpl(taskFolderName, projectMetadata, new ArrayList<>());
                synchronized (tasksFolders) {
                    tasksFolders.add(taskFolder);
                }
            }
        }
        return taskFolder;
    }

    public void saveConfiguration() {
        IProgressMonitor monitor = new NullProgressMonitor();

        File configFile = getConfigFile(true);
        try {
            if (configFile.exists()) {
                ContentUtils.makeFileBackup(configFile);
            }
            if (tasks.isEmpty()) {
                if (!configFile.delete()) {
                    log.error("Error deleting file " + configFile.getAbsolutePath());
                }
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

        try {
            IOUtils.writeFileFromBuffer(configFile, dsConfigBuffer.toByteArray());
        } catch (Exception e) {
            log.error("Error saving configuration to a file " + configFile.getAbsolutePath(), e);
        }
    }

    private void serializeTasks(DBRProgressMonitor monitor, JsonWriter jsonWriter) throws IOException {
        jsonWriter.setIndent("\t");
        jsonWriter.beginObject();
        if (!CommonUtils.isEmpty(tasksFolders)) {
            jsonWriter.name(TASKS_FOLDERS_TAG);
            jsonWriter.beginObject();
            for (TaskFolderImpl taskFolder : tasksFolders) {
                jsonWriter.name(taskFolder.getName());
                jsonWriter.beginObject();
                jsonWriter.endObject();
            }
            jsonWriter.endObject();
        }
        for (TaskImpl task : tasks) {
            jsonWriter.name(task.getId());
            jsonWriter.beginObject();
            JSONUtils.field(jsonWriter, "task", task.getType().getId());
            JSONUtils.field(jsonWriter, "label", task.getName());
            JSONUtils.field(jsonWriter, "description", task.getDescription());
            DBTTaskFolder taskFolder = task.getTaskFolder();
            if (taskFolder != null) {
                JSONUtils.field(jsonWriter, "taskFolder", taskFolder.getName());
            }
            JSONUtils.field(jsonWriter, "createTime", systemDateFormat.format(task.getCreateTime()));
            JSONUtils.field(jsonWriter, "updateTime", systemDateFormat.format(task.getUpdateTime()));
            JSONUtils.serializeProperties(jsonWriter, "state", task.getProperties());
            jsonWriter.endObject();
        }
        jsonWriter.endObject();
    }

    private File getConfigFile(boolean create) {
        return new File(projectMetadata.getMetadataFolder(create), CONFIG_FILE);
    }

}
