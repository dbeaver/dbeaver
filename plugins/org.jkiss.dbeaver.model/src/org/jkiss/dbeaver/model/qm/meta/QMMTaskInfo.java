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
package org.jkiss.dbeaver.model.qm.meta;

import org.jkiss.code.NotNull;

/** A task lifecycle snapshot. Contains no task settings, results or logs. */
public class QMMTaskInfo extends QMMObject {
    public static final String TASK_DATASOURCE_PREFIX = "qm-task:";

    public enum Status {
        STARTED, SUCCESS, FAILED, CANCELED
    }

    private final String executionId;
    private final String taskName;
    private final String taskTypeId;
    private final String taskTypeName;
    private final Status status;
    private final QMMConnectionInfo connection;

    public QMMTaskInfo(
        @NotNull String executionId,
        @NotNull String taskName,
        @NotNull String taskTypeId,
        @NotNull String taskTypeName,
        @NotNull QMMConnectionInfo connection,
        long startTime,
        long finishTime,
        @NotNull Status status
    ) {
        super(QMMetaObjectType.TASK_INFO, startTime, finishTime);
        this.executionId = executionId;
        this.taskName = taskName;
        this.taskTypeId = taskTypeId;
        this.taskTypeName = taskTypeName;
        this.connection = connection;
        this.status = status;
    }

    @NotNull
    public QMMTaskInfo finished(long finishTime, @NotNull Status outcome) {
        return new QMMTaskInfo(executionId, taskName, taskTypeId, taskTypeName, connection, getOpenTime(), finishTime, outcome);
    }

    @NotNull
    public String getExecutionId() {
        return executionId;
    }

    @NotNull
    public String getTaskName() {
        return taskName;
    }

    @NotNull
    public String getTaskTypeId() {
        return taskTypeId;
    }

    @NotNull
    public String getTaskTypeName() {
        return taskTypeName;
    }

    @NotNull
    public Status getStatus() {
        return status;
    }

    @NotNull
    @Override
    public String getText() {
        return taskName + " / " + taskTypeName;
    }

    @NotNull
    @Override
    public QMMConnectionInfo getConnection() {
        return connection;
    }
}
