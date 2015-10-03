/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.runtime;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.ui.UIUtils;
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
                    UIUtils.showErrorDialog(null, getName(), null, e.getTargetException());
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
