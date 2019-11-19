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
package org.jkiss.dbeaver.ui.task;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.task.DBTScheduler;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.registry.task.TaskRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;

public abstract class TaskHandlerScheduleBase extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        DBTTask task = (DBTTask) ((IStructuredSelection)HandlerUtil.getCurrentSelection(event)).getFirstElement();
        DBTScheduler scheduler = TaskRegistry.getInstance().getActiveSchedulerInstance();
        if (task != null && scheduler != null) {
            try {
                execute(task, scheduler);
            } catch (DBException e) {
                DBWorkbench.getPlatformUI().showError("Scheduler rrror", "Scheduler error", e);
            }
        }
        return null;
    }

    protected abstract void execute(DBTTask task, DBTScheduler scheduler) throws DBException;

}