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
package org.jkiss.dbeaver.model.task;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

/**
 * Task registry
 */
public interface DBTTaskRegistry {

    String EVENT_TASK_EXECUTE = "taskExecuted";
    String EVENT_PARAM_PROJECT = "project";
    String EVENT_PARAM_TASK = "taskId";

    @NotNull
    DBTTaskType[] getAllTaskTypes();

    @Nullable
    DBTTaskType getTaskType(String id);

    @NotNull
    DBTTaskCategory[] getAllCategories();

    @NotNull
    DBTTaskCategory[] getRootCategories();

    @NotNull
    DBTSchedulerDescriptor[] getAllSchedulers();

    DBTSchedulerDescriptor getActiveScheduler();

    void addTaskListener(DBTTaskListener listener);

    void removeTaskListener(DBTTaskListener listener);

}
