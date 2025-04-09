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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonWriter;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.impl.app.BaseProjectImpl;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.task.*;
import org.jkiss.dbeaver.registry.timezone.TimezoneRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * TaskManagerImpl
 */
public class TaskManagerImpl implements DBTTaskManager {

    private static final Log log = Log.getLog(TaskManagerImpl.class);

    private static final Gson CONFIG_GSON = new GsonBuilder()
        .setStrictness(Strictness.LENIENT)
        .serializeNulls()
        .setPrettyPrinting()
        .create();

    final SimpleDateFormat systemDateFormat;

    private final Set<TaskRunJob> runningTasks = Collections.synchronizedSet(new HashSet<>());
    private Job serviceJob;
    private final BaseProjectImpl projectMetadata;
    private final List<TaskImpl> tasks = new ArrayList<>();
    private final List<TaskFolderImpl> tasksFolders = new ArrayList<>();
    private final Path statisticsFolder;

    public TaskManagerImpl(BaseProjectImpl projectMetadata, Path statisticsFolder) {
        this.projectMetadata = projectMetadata;
        this.statisticsFolder = statisticsFolder;
        this.systemDateFormat = new SimpleDateFormat(GeneralUtils.DEFAULT_TIMESTAMP_PATTERN, Locale.ENGLISH);
        systemDateFormat.setTimeZone(TimeZone.getTimeZone(TimezoneRegistry.getUserDefaultTimezone()));
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
        @NotNull Map<String, Object> properties) throws DBException {
        if (getTaskByName(label) != null) {
            throw new DBException("Task with name '" + label + "' already exists");
        }
        Date createTime = new Date();
        String id = UUID.randomUUID().toString();
        TaskFolderImpl taskFolder = searchTaskFolderByName(taskFolderName);
        TaskImpl task = createTask(
            taskDescriptor,
            id,
            label,
            description,
            createTime,
            createTime,
            taskFolder,
            properties
        );

        return task;
    }

    @NotNull
    @Override
    public DBTTask createTemporaryTask(@NotNull DBTTaskType type, @NotNull String label) {
        return new TaskImpl(getProject(), type, TaskConstants.TEMPORARY_ID, label, label, new Date(), null, null);
    }

    @NotNull
    @Override
    public DBTTaskFolder createTaskFolder(@NotNull DBPProject project,
                                          @NotNull String folderName,
                                          @Nullable DBTTaskFolder parentFolder,
                                          @Nullable DBTTask[] folderTasks) throws DBException {
        if (!CommonUtils.isEmpty(tasksFolders)
            && tasksFolders.stream().anyMatch(taskFolder -> taskFolder.getName().equals(folderName))) {
            throw new DBException("Task folder with name '" + folderName + "' already exists");
        }
        TaskFolderImpl taskFolder = new TaskFolderImpl(folderName, parentFolder, project, folderTasks != null ?
            new ArrayList<>(Arrays.asList(folderTasks))
            : new ArrayList<>());
        synchronized (tasksFolders) {
            tasksFolders.add(taskFolder);
        }
        if (parentFolder != null) {
            parentFolder.addFolderToFoldersList(taskFolder);
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

        DBTTaskFolder parentFolder = taskFolder.getParentFolder();
        if (parentFolder != null) {
            // Remove folder from parent
            parentFolder.removeFolderFromFoldersList(taskFolder);
        }

        // Remove empty task folder or make task folder empty and then remove it
        // Move all task to the parent folder if it exists
        List<DBTTask> folderTasks = taskFolder.getTasks();
        if (!CommonUtils.isEmpty(folderTasks)) {
            for (DBTTask task : folderTasks) {
                if (task instanceof TaskImpl) {
                    ((TaskImpl) task).setTaskFolder(parentFolder);
                }
            }
        }

        synchronized (tasksFolders) {
            tasksFolders.remove(taskFolder);
        }
        saveConfiguration();

        TaskRegistry.getInstance().notifyTaskFoldersListeners(new DBTTaskFolderEvent(taskFolder, DBTTaskFolderEvent.Action.TASK_FOLDER_REMOVE));
    }

    @Override
    public boolean hasRunningTasks() {
        return !runningTasks.isEmpty();
    }

    @Override
    public void cancelRunningTasks() {
        final Job[] tasks = runningTasks.toArray(Job[]::new);
        for (Job task : tasks) {
            task.cancel();
        }
    }

    @NotNull
    @Override
    public Path getStatisticsFolder() {
        return statisticsFolder;
    }

    @NotNull
    @Override
    public Path getStatisticsFolder(@NotNull DBTTask task) {
        return statisticsFolder.resolve(task.getId());
    }

    @NotNull
    @Override
    public DBTTaskRunStatus runTask(@NotNull DBRProgressMonitor monitor, @NotNull DBTTask task, @NotNull DBTTaskExecutionListener listener) throws DBException {
        final TaskRunJob job = createJob((TaskImpl) task, listener);
        if (serviceJob == null) {
            serviceJob = new ServiceJob();
            serviceJob.schedule();
        }
        runningTasks.add(job);
        final IStatus result = job.runDirectly(monitor);
        final Throwable error = result.getException();
        if (error != null) {
            runningTasks.remove(job);
            if (error instanceof DBException e) {
                throw e;
            } else {
                throw new DBException("Error executing task", error);
            }
        }
        runningTasks.remove(job);
        return job.getTaskRunStatus();
    }

    @NotNull
    @Override
    public TaskRunJob scheduleTask(@NotNull DBTTask task, @NotNull DBTTaskExecutionListener listener) throws DBException {
        final TaskRunJob runJob = createJob((TaskImpl) task, listener);
        runJob.schedule();
        if (serviceJob == null) {
            serviceJob = new ServiceJob();
            serviceJob.schedule();
        }
        return runJob;
    }
 
    @NotNull
    private TaskRunJob createJob(@NotNull TaskImpl task, @NotNull DBTTaskExecutionListener listener) {
        TaskRunJob runJob = new TaskRunJob(task, Locale.getDefault(), listener);
        runJob.addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void aboutToRun(IJobChangeEvent event) {
                runningTasks.add((TaskRunJob) event.getJob());
            }

            @Override
            public void done(IJobChangeEvent event) {
                runningTasks.remove((TaskRunJob) event.getJob());
            }
        });
        return runJob;
    }

    private void loadConfiguration() {
        if (!getProject().hasRealmPermission(RMConstants.PERMISSION_PROJECT_DATASOURCES_VIEW)) {
            log.warn("The user has no permission to see tasks for this project: " + getProject().getDisplayName());
            return;
        }
        String configFile = null;
        try {
            configFile = loadConfigFile();
        } catch (DBException e) {
            log.error("Error loading task configuration file.", e);
        }
        if (CommonUtils.isEmpty(configFile)) {
            return;
        }
        Map<String, Object> jsonMap = JSONUtils.parseMap(CONFIG_GSON, new StringReader(configFile));
        // First read and create folders
        for (Map.Entry<String, Map<String, Object>> folderMap : JSONUtils.getNestedObjects(jsonMap, TaskConstants.TASKS_FOLDERS_TAG)) {
            String folderName = folderMap.getKey();
            if (CommonUtils.isNotEmpty(folderName)) {
                Object property = JSONUtils.getObjectProperty(folderMap.getValue(), TaskConstants.TAG_PARENT);
                TaskFolderImpl parentFolder = null;
                if (property != null) {
                    Optional<TaskFolderImpl> first = tasksFolders.stream()
                        .filter(e -> e.getName().equals(property.toString()))
                        .findFirst();
                    if (first.isPresent()) {
                        parentFolder = first.get();
                    }
                }
                try {
                    createTaskFolder(projectMetadata, folderName, parentFolder, new DBTTask[0]);
                } catch (DBException ex) {
                    log.error("Error creating tasks folder.", ex);
                }
            }
        }

        for (Map.Entry<String, Object> taskMap : jsonMap.entrySet()) {
            Map<String, Object> taskJSON = (Map<String, Object>) taskMap.getValue();

            try {
                String id = taskMap.getKey();
                if (!id.startsWith(TaskConstants.TASKS_FOLDERS_TAG)) {
                    String task = JSONUtils.getString(taskJSON, TaskConstants.TAG_TASK);
                    String label = CommonUtils.toString(JSONUtils.getString(taskJSON, TaskConstants.TAG_LABEL), id);
                    String description = JSONUtils.getString(taskJSON, TaskConstants.TAG_DESCRIPTION);
                    String taskFolderName = JSONUtils.getString(taskJSON, TaskConstants.TAG_TASK_FOLDER);
                    Date createTime = systemDateFormat.parse(JSONUtils.getString(taskJSON, TaskConstants.TAG_CREATE_TIME));
                    Date updateTime = systemDateFormat.parse(JSONUtils.getString(taskJSON, TaskConstants.TAG_UPDATE_TIME));
                    int maxExecutionTime = JSONUtils.getInteger(taskJSON, TaskConstants.TAG_MAX_EXEC_TIME);
                    Map<String, Object> state = JSONUtils.getObject(taskJSON, TaskConstants.TAG_STATE);

                    DBTTaskType taskDescriptor = getRegistry().getTaskType(task);
                    if (taskDescriptor == null) {
                        log.error("Can't find task descriptor " + task);
                        continue;
                    }

                    TaskFolderImpl taskFolder = searchTaskFolderByName(taskFolderName);
                    TaskImpl taskConfig = createTask(
                        taskDescriptor,
                        id,
                        label,
                        description,
                        createTime,
                        updateTime,
                        taskFolder,
                        state
                    );
                    taskConfig.setMaxExecutionTime(maxExecutionTime);
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

    @NotNull
    protected TaskImpl createTask(
        @NotNull DBTTaskType taskType,
        @NotNull String id,
        @NotNull String label,
        @Nullable String description,
        @NotNull Date createTime,
        @NotNull Date updateTime,
        @Nullable TaskFolderImpl taskFolder,
        @NotNull Map<String, Object> properties
    ) {
        TaskImpl taskConfig = new TaskImpl(
            getProject(),
            taskType,
            id,
            label,
            description,
            createTime,
            updateTime,
            taskFolder
        );
        taskConfig.setProperties(properties);
        return taskConfig;
    }

    protected String loadConfigFile() throws DBException {
        return DBWorkbench.getPlatform()
            .getTaskController()
            .loadTaskConfigurationFile(getProject().getId(), TaskConstants.CONFIG_FILE);
    }

    private TaskFolderImpl searchTaskFolderByName(String taskFolderName) {
        TaskFolderImpl taskFolder = null;
        if (CommonUtils.isNotEmpty(taskFolderName)) {
            taskFolder = DBUtils.findObject(tasksFolders, taskFolderName);
            if (taskFolder == null) {
                taskFolder = new TaskFolderImpl(taskFolderName, null, projectMetadata, new ArrayList<>());
                synchronized (tasksFolders) {
                    tasksFolders.add(taskFolder);
                }
            }
        }
        return taskFolder;
    }

    public void saveConfiguration() {
        DBPProject project = getProject();
        try {
            if (tasks.isEmpty() && CommonUtils.isEmpty(tasksFolders)) {
                DBWorkbench.getPlatform().getTaskController().saveTaskConfigurationFile(project.getId(), TaskConstants.CONFIG_FILE, null);
                return;
            }
        } catch (Exception e) {
            log.error("Error processing config file", e);
        }

        ByteArrayOutputStream dsConfigBuffer = new ByteArrayOutputStream(10000);
        try (OutputStreamWriter osw = new OutputStreamWriter(dsConfigBuffer, StandardCharsets.UTF_8)) {
            try (JsonWriter jsonWriter = CONFIG_GSON.newJsonWriter(osw)) {
                synchronized (tasks) {
                    serializeTasks(jsonWriter);
                }
            }
        } catch (IOException e) {
            log.error(e);
            return;
        }

        try {
            DBWorkbench.getPlatform().getTaskController().saveTaskConfigurationFile(
                project.getId(),
                TaskConstants.CONFIG_FILE, dsConfigBuffer.toString(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Error saving configuration to a file " + TaskConstants.CONFIG_FILE, e);
        }
    }

    @Override
    public void updateConfiguration() {
        saveConfiguration();
    }

    private void serializeTasks(@NotNull JsonWriter jsonWriter) throws IOException {
        jsonWriter.setIndent("\t");
        jsonWriter.beginObject();
        if (!CommonUtils.isEmpty(tasksFolders)) {
            jsonWriter.name(TaskConstants.TASKS_FOLDERS_TAG);
            jsonWriter.beginObject();
            for (TaskFolderImpl taskFolder : tasksFolders) {
                jsonWriter.name(taskFolder.getName());
                jsonWriter.beginObject();
                if (taskFolder.getParentFolder() != null) {
                    JSONUtils.field(jsonWriter, TaskConstants.TAG_PARENT, taskFolder.getParentFolder().getName());
                }
                jsonWriter.endObject();
            }
            jsonWriter.endObject();
        }
        for (TaskImpl task : tasks) {
            jsonWriter.name(task.getId());
            jsonWriter.beginObject();
            JSONUtils.field(jsonWriter, TaskConstants.TAG_TASK, task.getType().getId());
            JSONUtils.field(jsonWriter, TaskConstants.TAG_LABEL, task.getName());
            JSONUtils.field(jsonWriter, TaskConstants.TAG_DESCRIPTION, task.getDescription());
            DBTTaskFolder taskFolder = task.getTaskFolder();
            if (taskFolder != null) {
                JSONUtils.field(jsonWriter, TaskConstants.TAG_TASK_FOLDER, taskFolder.getName());
            }
            JSONUtils.field(jsonWriter, TaskConstants.TAG_CREATE_TIME, systemDateFormat.format(task.getCreateTime()));
            JSONUtils.field(jsonWriter, TaskConstants.TAG_UPDATE_TIME, systemDateFormat.format(task.getUpdateTime()));
            JSONUtils.serializeProperties(jsonWriter, TaskConstants.TAG_STATE, task.getProperties(), true);
            if (task.getMaxExecutionTime() > 0) {
                JSONUtils.field(jsonWriter, TaskConstants.TAG_MAX_EXEC_TIME, task.getMaxExecutionTime());
            }
            jsonWriter.endObject();
        }
        jsonWriter.endObject();
    }

    private class ServiceJob extends Job {
        private static final int TASK_SLEEP_TIME = 1000;

        public ServiceJob() {
            super("Task canceling job");
            setSystem(true);
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            CopyOnWriteArraySet<TaskRunJob> copyOfRunningTasks = new CopyOnWriteArraySet<>(runningTasks);
            for (TaskRunJob taskJob : copyOfRunningTasks) {
                if (taskJob.isFinished() || taskJob.isCanceled()) {
                    continue;
                }
                taskJob.cancelByTimeReached();
            }
            schedule(TASK_SLEEP_TIME);
            return Status.OK_STATUS;
        }
    }

}
