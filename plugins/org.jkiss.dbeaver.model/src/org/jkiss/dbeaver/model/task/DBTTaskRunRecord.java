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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.Date;

/** An execution snapshot. Deliberately excludes task settings, results, errors and log contents. */
public record DBTTaskRunRecord(
    @NotNull String id,
    @NotNull String taskId,
    @NotNull String taskName,
    @NotNull String taskTypeId,
    @NotNull String taskTypeName,
    @NotNull String projectId,
    @NotNull String projectName,
    long startTime,
    long finishTime,
    @NotNull String startUser,
    @NotNull String startedBy,
    @NotNull Status status,
    boolean hasLog
) implements DBTTaskRun {
    public enum Status {
        RUNNING, SUCCESS, FAILED, CANCELED
    }

    @NotNull
    public DBTTaskRunRecord finished(long time, @NotNull Status outcome) {
        return new DBTTaskRunRecord(id, taskId, taskName, taskTypeId, taskTypeName, projectId, projectName,
            startTime, time, startUser, startedBy, outcome, hasLog);
    }

    @NotNull
    @Override
    public String getId() {
        return id;
    }

    @NotNull
    @Override
    public Date getStartTime() {
        return new Date(startTime);
    }

    @NotNull
    @Override
    public String getStartUser() {
        return startUser;
    }

    @NotNull
    @Override
    public String getStartedBy() {
        return startedBy;
    }

    @Override
    public long getRunDuration() {
        return isFinished() ? Math.max(0, finishTime - startTime) : -1;
    }

    @Override
    public boolean isRunSuccess() {
        return status == Status.SUCCESS;
    }

    @Override
    public boolean isFinished() {
        return status != Status.RUNNING;
    }

    @Nullable
    @Override
    public String getErrorMessage() {
        return switch (status) {
            case FAILED -> "Failed";
            case CANCELED -> "Canceled";
            default -> null;
        };
    }

    @Nullable
    @Override
    public String getErrorStackTrace() {
        return null;
    }

    @Nullable
    @Override
    public String getExtraMessage() {
        return null;
    }
}
