/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.task.DBTTaskRun;
import org.jkiss.utils.CommonUtils;

import java.util.Date;

/**
 * TaskRunImpl
 */
class TaskRunImpl implements DBTTaskRun {

    static final String RUN_LOG_PREFIX = "run_";
    static final String RUN_LOG_EXT = "log";

    private String id;
    private Date startTime;
    private String startUser;
    private String startedBy;
    private long duration;
    private String errorMessage;
    private String errorStackTrace;

    TaskRunImpl() {
        this.id = "void";
    }

    TaskRunImpl(String id, Date startTime, String startUser, String startedBy, long duration, String errorMessage, String errorStackTrace) {
        this.id = id;
        this.startTime = startTime;
        this.startUser = startUser;
        this.startedBy = startedBy;
        this.duration = duration;
        this.errorMessage = errorMessage;
        this.errorStackTrace = errorStackTrace;
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    @Override
    public Date getStartTime() {
        return startTime;
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
        return duration;
    }

    public void setRunDuration(long duration) {
        this.duration = duration;
    }

    @Override
    public boolean isRunSuccess() {
        return errorMessage == null;
    }

    @Nullable
    @Override
    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Nullable
    @Override
    public String getErrorStackTrace() {
        return errorStackTrace;
    }

    public void setErrorStackTrace(String errorStackTrace) {
        this.errorStackTrace = errorStackTrace;
    }

    @Override
    public String toString() {
        return id + "; " + startUser + "; " + startedBy + "; " + (isRunSuccess() ? "Success" : CommonUtils.notEmpty(errorMessage));
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TaskRunImpl && id.equals(((TaskRunImpl) obj).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
