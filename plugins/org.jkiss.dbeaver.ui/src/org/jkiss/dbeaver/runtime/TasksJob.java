/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.runtime;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.ui.dialogs.exec.ExecutionQueueErrorJob;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Job running specified tasks in queue
 */
public class TasksJob extends AbstractJob
{
    private final List<DBRRunnableWithProgress> tasks;

    private TasksJob(String name, Collection<DBRRunnableWithProgress> tasks) {
        super(name);
        setUser(true);
        this.tasks = new ArrayList<>(tasks);
    }

    private TasksJob(String name, DBRRunnableWithProgress task) {
        this(name, Collections.singletonList(task));
    }

    @Override
    protected IStatus run(DBRProgressMonitor monitor) {
        monitor.beginTask(getName(), tasks.size());
        boolean ignoreErrors = false;
        for (int i = 0; i < tasks.size(); ) {
            DBRRunnableWithProgress task = tasks.get(i);
            if (monitor.isCanceled()) {
                break;
            }
            try {
                task.run(monitor);
            } catch (InvocationTargetException e) {
                if (tasks.size() == 1) {
                    DBWorkbench.getPlatformUI().showError(getName(), null, e.getTargetException());
                } else if (!ignoreErrors) {
                    boolean keepRunning = true;
                    switch (ExecutionQueueErrorJob.showError(getName(), e.getTargetException(), true)) {
                        case STOP:
                            keepRunning = false;
                            break;
                        case RETRY:
                            // just make it again
                            continue;
                        case IGNORE:
                            // Just do nothing
                            break;
                        case IGNORE_ALL:
                            ignoreErrors = true;
                            break;
                    }
                    if (!keepRunning) {
                        break;
                    }
                }

            } catch (InterruptedException e) {
                // Ignore
            }
            monitor.worked(1);
            i++;
        }
        monitor.done();
        return Status.OK_STATUS;
    }

    public static void runTasks(String name, Collection<DBRRunnableWithProgress> tasks) {
        new TasksJob(name, tasks).schedule();
    }

    public static void runTask(String name, DBRRunnableWithProgress task) {
        new TasksJob(name, task).schedule();
    }

}
