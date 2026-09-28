/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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

import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.util.List;

/** Optional persistent task history, independent of query logging and saved task configuration. */
public interface DBTTaskRunStorage {
    enum Order {
        NAME, TYPE, PROJECT, STATUS, START_TIME, FINISH_TIME, DURATION
    }

    record Filter(
        @Nullable String projectId,
        @Nullable String taskId,
        @Nullable String text,
        @Nullable String typeId,
        @Nullable DBTTaskRunRecord.Status status,
        @Nullable Long fromTime,
        @Nullable Long toTime,
        @NotNull Order order,
        boolean descending
    ) {
    }

    @Nullable
    static DBTTaskRunStorage getInstance() {
        // Applications implement their own getAdapter(), which need not consult extension factories.
        return (DBTTaskRunStorage) Platform.getAdapterManager().loadAdapter(
            DBWorkbench.getPlatform().getApplication(), DBTTaskRunStorage.class.getName());
    }

    /** Inserts the initial snapshot or updates the same run when it completes. */
    void saveRun(@NotNull DBTTaskRunRecord run) throws DBException;

    @NotNull
    List<DBTTaskRunRecord> findRuns(
        @NotNull DBRProgressMonitor monitor,
        @NotNull Filter filter,
        int offset,
        int limit
    ) throws DBException;

    /** Deletes matching runs. Null task/run IDs select all tasks/runs in the project. Never deletes log files. */
    void deleteRuns(@NotNull String projectId, @Nullable String taskId, @Nullable String runId) throws DBException;
}
