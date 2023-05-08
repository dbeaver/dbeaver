/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.task.*;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * TaskImpl
 */
public class TaskImpl implements DBTTask, DBPNamedObject2 {
    private static final Log log = Log.getLog(TaskImpl.class);
    private static final int MAX_RUNS_IN_STATS = 100;
    private static final TaskRunImpl VOID_RUN = new TaskRunImpl();
    private static final Gson gson = new GsonBuilder()
        .setLenient()
        .setDateFormat(GeneralUtils.DEFAULT_TIMESTAMP_PATTERN)
        .create();

    private final DBPProject project;
    private final String id;
    private String label;
    private String description;
    private Date createTime;
    private Date updateTime;
    private DBTTaskType type;
    private Map<String, Object> properties;
    private TaskRunImpl lastRun;
    @Nullable
    private TaskFolderImpl taskFolder;

    public TaskImpl(
        @NotNull DBPProject project,
        @NotNull DBTTaskType type,
        @NotNull String id,
        @NotNull String label,
        @Nullable String description,
        @NotNull Date createTime,
        @Nullable Date updateTime,
        @Nullable TaskFolderImpl taskFolder
    ) {
        this.project = project;
        this.id = id;
        this.label = label;
        this.description = description;
        this.createTime = createTime;
        this.updateTime = updateTime;
        this.type = type;
        this.taskFolder = taskFolder;
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    @Override
    public DBPProject getProject() {
        return project;
    }

    @NotNull
    @Override
    public String getName() {
        return label;
    }

    @Override
    public void setName(@NotNull String label) {
        this.label = label;
    }

    @Nullable
    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(@NotNull String description) {
        this.description = description;
    }

    @Nullable
    @Override
    public DBTTaskFolder getTaskFolder() {
        return taskFolder;
    }

    public void setTaskFolder(@Nullable DBTTaskFolder taskFolder) {
        this.taskFolder = (TaskFolderImpl) taskFolder;
    }

    @NotNull
    @Override
    public Date getCreateTime() {
        return createTime;
    }

    @NotNull
    @Override
    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    @NotNull
    @Override
    public DBTTaskType getType() {
        return type;
    }

    public void setType(DBTTaskType type) {
        this.type = type;
    }

    @NotNull
    @Override
    public Map<String, Object> getProperties() {
        return properties;
    }

    @Nullable
    @Override
    public DBTTaskRun getLastRun() {
        if (lastRun == null) {
            refreshRunStatistics();
        }
        return lastRun == VOID_RUN ? null : lastRun;
    }

    @NotNull
    @Override
    public DBTTaskRun[] getRunStatistics() {
        return loadRunStatistics().toArray(new DBTTaskRun[0]);
    }

    @NotNull
    @Override
    public Path getRunLog(DBTTaskRun run) {
        return getTaskStatsFolder(false).resolve(TaskUtils.buildRunLogFileName(run.getId()));
    }

    @Override
    public void removeRunLog(DBTTaskRun taskRun) {
        Path runLog = getRunLog(taskRun);

        if (Files.exists(runLog)) {
            try {
                Files.delete(runLog);
            } catch (IOException e) {
                log.error("Can't delete log file '" + runLog.toAbsolutePath() + "'", e);
            }
        }
        List<TaskRunImpl> runs = loadRunStatistics();
        runs.remove(taskRun);
        flushRunStatistics(runs);
        if (CommonUtils.equalObjects(lastRun, taskRun)) {
            lastRun = null;
        }
        TaskRegistry.getInstance().notifyTaskListeners(new DBTTaskEvent(this, DBTTaskEvent.Action.TASK_UPDATE));
    }

    @Override
    public void cleanRunStatistics() {
        Path statsFolder = getTaskStatsFolder(false);
        if (Files.exists(statsFolder)) {
            try {
                List<Path> taskFiles = Files.list(statsFolder).collect(Collectors.toList());
                for (Path file : taskFiles) {
                    try {
                        Files.delete(file);
                    } catch (IOException e) {
                        log.error("Can't delete log item '" + file.toAbsolutePath() + "'", e);
                    }
                }
                Files.delete(statsFolder);
            } catch (IOException e) {
                log.error("Can't delete logs folder '" + statsFolder.toAbsolutePath() + "'", e);
            }
        }
        flushRunStatistics(List.of());
        lastRun = null;
        TaskRegistry.getInstance().notifyTaskListeners(new DBTTaskEvent(this, DBTTaskEvent.Action.TASK_UPDATE));
    }

    @Override
    public void refreshRunStatistics() {
        try {
            synchronized (this) {
                List<TaskRunImpl> runs = loadRunStatistics();
                lastRun = runs.isEmpty() ? VOID_RUN : runs.get(runs.size() - 1);
            }
        } catch (Throwable e) {
            log.debug("Error loading task runs", e); //$NON-NLS-1$
        }
    }

    @Override
    public void setProperties(@NotNull Map<String, Object> properties) {
        this.properties = new LinkedHashMap<>(properties);
    }

    @Override
    public boolean isTemporary() {
        return TaskConstants.TEMPORARY_ID.equals(id);
    }

    @NotNull
    @Override
    public Path getRunLogFolder() {
        return getTaskStatsFolder(false);
    }

    protected Path getTaskStatsFolder(boolean create) {
        Path taskStatsFolder = project.getTaskManager().getStatisticsFolder().resolve(id);
        if (create && !Files.exists(taskStatsFolder)) {
            try {
                Files.createDirectories(taskStatsFolder);
            } catch (IOException e) {
                log.error("Can't create task log folder '" + taskStatsFolder.toAbsolutePath() + "'", e);
            }
        }
        return taskStatsFolder;
    }

    private List<TaskRunImpl> loadRunStatistics() {
        Path metaFile = getTaskStatsFolder(false).resolve(META_FILE_NAME);
        return TaskUtils.loadRunStatistics(metaFile, gson);
    }

    private void flushRunStatistics(List<TaskRunImpl> runs) {
        Path metaFile = getTaskStatsFolder(true).resolve(META_FILE_NAME);
        try (Writer writer = Files.newBufferedWriter(metaFile)) {
            String metaContent = gson.toJson(new RunStatistics(runs));
            writer.write(metaContent);
        } catch (IOException e) {
            log.error("Error writing task run statistics", e);
        }
    }

    void addNewRun(TaskRunImpl taskRun) {
        synchronized (this) {
            lastRun = taskRun;
            var runs = loadRunStatistics();
            runs.add(taskRun);
            while (runs.size() > MAX_RUNS_IN_STATS) {
                runs.remove(0);
            }
            flushRunStatistics(runs);
        }
        TaskRegistry.getInstance().notifyTaskListeners(new DBTTaskEvent(this, DBTTaskEvent.Action.TASK_UPDATE));
    }

    void updateRun(TaskRunImpl taskRun) {
        synchronized (this) {
            List<TaskRunImpl> runs = loadRunStatistics();
            for (int i = 0; i < runs.size(); i++) {
                TaskRunImpl run = runs.get(i);
                if (CommonUtils.equalObjects(run.getId(), taskRun.getId())) {
                    runs.set(i, taskRun);
                    break;
                }
            }
            flushRunStatistics(runs);
        }
        TaskRegistry.getInstance().notifyTaskListeners(new DBTTaskEvent(this, DBTTaskEvent.Action.TASK_UPDATE));
    }

    @Override
    public String toString() {
        return id + " " + label + " (" + type.getName() + ")";
    }
}
