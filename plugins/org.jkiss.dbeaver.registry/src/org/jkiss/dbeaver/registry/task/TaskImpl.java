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
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.task.*;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * TaskImpl
 */
public class TaskImpl implements DBTTask, DBPNamedObject2 {
    public static String META_FILE_NAME = "meta.json";

    private static final Log log = Log.getLog(TaskImpl.class);
    private static final int MAX_RUNS_IN_STATS = 100;
    private static final Gson gson = new GsonBuilder()
        .setStrictness(Strictness.LENIENT)
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
    private volatile List<DBTTaskRun> runs;
    private DBTTaskFolder taskFolder;
    private int maxExecutionTime;

    protected TaskImpl(
        @NotNull DBPProject project,
        @NotNull DBTTaskType type,
        @NotNull String id,
        @NotNull String label,
        @Nullable String description,
        @NotNull Date createTime,
        @Nullable Date updateTime,
        @Nullable DBTTaskFolder folder
    ) {
        this.project = project;
        this.id = id;
        this.label = label;
        this.description = description;
        this.createTime = createTime;
        this.updateTime = updateTime;
        this.type = type;
        this.taskFolder = folder;
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
        this.taskFolder = taskFolder;
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
        loadRunsIfNeeded();
        return runs.isEmpty() ? null : runs.get(runs.size() - 1);
    }

    @NotNull
    @Override
    public DBTTaskRun[] getAllRuns() {
        loadRunsIfNeeded();
        return runs.toArray(DBTTaskRun[]::new);
    }

    @Nullable
    @Override
    public Path getRunLog(@NotNull DBTTaskRun run) {
        return getTaskStatsFolder(false).resolve(TaskUtils.buildRunLogFileName(run.getId()));
    }

    @NotNull
    @Override
    public InputStream getRunLogInputStream(@NotNull DBTTaskRun run) throws DBException, IOException {
        return Files.newInputStream(Objects.requireNonNull(getRunLog(run)));
    }

    @Override
    public void removeRun(DBTTaskRun taskRun) {
        synchronized (this) {
            loadRunsIfNeeded();

            if (!runs.remove(taskRun)) {
                return;
            }

            Path runLog = getRunLog(taskRun);

            if (runLog != null) {
                try {
                    Files.deleteIfExists(runLog);
                } catch (IOException e) {
                    log.error("Can't delete log file '" + runLog.toAbsolutePath() + "'", e);
                }
            }

            flushRunStatistics(runs);
        }

        TaskRegistry.getInstance().notifyTaskListeners(new DBTTaskEvent(this, DBTTaskEvent.Action.TASK_UPDATE));
    }

    @Override
    public void cleanRunStatistics() {
        Path statsFolder = getTaskStatsFolder(false);
        if (Files.exists(statsFolder)) {
            try (Stream<Path> list = Files.list(statsFolder)) {
                List<Path> taskFiles = list.toList();
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
        if (runs != null) {
            runs.clear();
        }
        flushRunStatistics(List.of());
        TaskRegistry.getInstance().notifyTaskListeners(new DBTTaskEvent(this, DBTTaskEvent.Action.TASK_UPDATE));
    }

    @Override
    public void refreshRunStatistics() {
        runs = new ArrayList<>(loadRunStatistics());
    }

    @Override
    public void setProperties(@NotNull Map<String, Object> properties) {
        this.properties = new LinkedHashMap<>(properties);
    }

    @Override
    public boolean isTemporary() {
        return TaskConstants.TEMPORARY_ID.equals(id);
    }

    public int getMaxExecutionTime() {
        return maxExecutionTime;
    }

    public void setMaxExecutionTime(int maxExecutionTime) {
        this.maxExecutionTime = maxExecutionTime;
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

    void addNewRun(@NotNull DBTTaskRun taskRun) {
        synchronized (this) {
            loadRunsIfNeeded();

            runs.add(taskRun);

            while (runs.size() > MAX_RUNS_IN_STATS) {
                runs.remove(0);
            }

            flushRunStatistics(runs);
        }

        TaskRegistry.getInstance().notifyTaskListeners(new DBTTaskEvent(this, DBTTaskEvent.Action.TASK_UPDATE));
    }

    void updateRun(@NotNull TaskRunImpl taskRun) {
        synchronized (this) {
            loadRunsIfNeeded();

            for (int i = 0; i < runs.size(); i++) {
                if (runs.get(i).getId().equals(taskRun.getId())) {
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

    @NotNull
    protected List<? extends DBTTaskRun> loadRunStatistics() {
        return TaskUtils.loadRunStatistics(getTaskStatsFolder(false).resolve(META_FILE_NAME), gson);
    }

    protected void flushRunStatistics(@NotNull List<? extends DBTTaskRun> runs) {
        Path metaFile = getTaskStatsFolder(true).resolve(META_FILE_NAME);
        try (Writer writer = Files.newBufferedWriter(metaFile)) {
            final List<TaskRunImpl> filteredRuns = runs.stream()
                .filter(run -> run instanceof TaskRunImpl)
                .map(run -> (TaskRunImpl) run)
                .collect(Collectors.toList());
            writer.write(gson.toJson(new RunStatistics(filteredRuns)));
        } catch (IOException e) {
            log.error("Error writing task run statistics", e);
        }
    }

    private void loadRunsIfNeeded() {
        if (runs == null) {
            synchronized (this) {
                if (runs == null) {
                    runs = new ArrayList<>(loadRunStatistics());
                }
            }
        }
    }
}
