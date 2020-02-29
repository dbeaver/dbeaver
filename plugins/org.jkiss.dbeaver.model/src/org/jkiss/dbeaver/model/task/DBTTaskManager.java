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
package org.jkiss.dbeaver.model.task;

import org.eclipse.core.runtime.jobs.Job;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.app.DBPProject;

import java.io.File;
import java.util.Map;

/**
 * Task manager
 */
public interface DBTTaskManager {

    @NotNull
    DBTTaskRegistry getRegistry();

    @NotNull
    DBPProject getProject();

    @NotNull
    DBTTask[] getAllTasks();

    @Nullable
    DBTTask getTaskById(@NotNull String id);

    @Nullable
    DBTTask getTaskByName(@NotNull String name);

    @NotNull
    DBTTask[] getAllTaskByType(DBTTaskType task);

    @NotNull
    DBTTaskType[] getExistingTaskTypes();

    @NotNull
    DBTTask createTask(
        @NotNull DBTTaskType task,
        @NotNull String label,
        @Nullable String description,
        @NotNull Map<String, Object> properties) throws DBException;

    /**
     * Temporary tasks can be used to execute some task without adding to task manager registry
     */
    @NotNull
    DBTTask createTemporaryTask(
        @NotNull DBTTaskType task,
        @NotNull String label);

    void updateTaskConfiguration(@NotNull DBTTask task) throws DBException;

    void deleteTaskConfiguration(@NotNull DBTTask task);

    @NotNull
    File getStatisticsFolder();

    Job runTask(@NotNull DBTTask task, @NotNull DBTTaskExecutionListener listener, @NotNull Map<String, Object> options) throws DBException;

}
