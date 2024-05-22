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

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.ProxyProgressMonitor;
import org.jkiss.dbeaver.model.task.DBTTask;

public class TaskLoggingProgressMonitor extends ProxyProgressMonitor {

    private static final Log log = Log.getLog(TaskLoggingProgressMonitor.class);
    DBTTask task;

    public TaskLoggingProgressMonitor(DBRProgressMonitor monitor, DBTTask task) {
        super(monitor);
        this.task = task;
    }

    @Override
    public void beginTask(String name, int totalWork) {
        super.beginTask(name, totalWork);
        log.debug("" + name);
    }

    @Override
    public void subTask(String name) {
        super.subTask(name);
        log.debug("\t" + name);
    }

    public DBTTask getTask() {
        return task;
    }

}