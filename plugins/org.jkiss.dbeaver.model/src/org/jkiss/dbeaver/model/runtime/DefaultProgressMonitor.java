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
package org.jkiss.dbeaver.model.runtime;

import org.eclipse.core.runtime.IProgressMonitor;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.ArrayUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Progress monitor default implementation
 */
public class DefaultProgressMonitor implements DBRProgressMonitor {

    private static final Log log = Log.getLog(DefaultProgressMonitor.class);

    private IProgressMonitor nestedMonitor;
    private List<DBRBlockingObject> blocks = null;
    private ProgressState[] states = new ProgressState[0];

    private static class ProgressState {
        final String taskName;
        final int totalWork;
        int progress;
        String subTask;

        ProgressState(String taskName, int totalWork) {
            this.taskName = taskName;
            this.totalWork = totalWork;
        }
    }

    public DefaultProgressMonitor(IProgressMonitor nestedMonitor)
    {
        this.nestedMonitor = nestedMonitor;
    }

    @Override
    public IProgressMonitor getNestedMonitor()
    {
        return nestedMonitor;
    }

    @Override
    public void beginTask(String name, int totalWork)
    {
        ProgressState state = new ProgressState(name, totalWork);
        states = ArrayUtils.add(ProgressState.class, states, state);

        nestedMonitor.beginTask(name, totalWork);
    }

    @Override
    public void done()
    {
        if (states.length == 0) {
            log.trace(new DBCException("Progress ended without start"));
        } else {
            states = ArrayUtils.remove(ProgressState.class, states, states.length - 1);
        }
        nestedMonitor.done();

        // Restore previous state
        if (states.length > 0) {
            ProgressState lastState = states[states.length - 1];
            nestedMonitor.beginTask(lastState.taskName, lastState.totalWork);
            if (lastState.subTask != null) {
                nestedMonitor.subTask(lastState.subTask);
            }
            if (lastState.progress > 0) {
                nestedMonitor.worked(lastState.progress);
            }
        }
    }

    @Override
    public void subTask(String name)
    {
        if (states.length == 0) {
            log.trace(new DBCException("Progress sub task without start"));
        } else {
            states[states.length - 1].subTask = name;
        }
        nestedMonitor.subTask(name);
    }

    @Override
    public void worked(int work)
    {
        if (states.length == 0) {
            log.trace(new DBCException("Progress info without start"));
        } else {
            states[states.length - 1].progress += work;
        }
        nestedMonitor.worked(work);
    }

    @Override
    public boolean isCanceled()
    {
        return nestedMonitor.isCanceled() ||
            DBWorkbench.getPlatform().isShuttingDown(); // All monitors are canceled if workbench is shutting down
    }

    @Override
    public synchronized void startBlock(DBRBlockingObject object, String taskName)
    {
        if (taskName != null) {
            subTask(taskName);
        }
        if (blocks == null) {
            blocks = new ArrayList<>();
        }
        blocks.add(object);
    }

    @Override
    public synchronized void endBlock()
    {
        if (blocks == null || blocks.isEmpty()) {
            log.warn("End block invoked while no blocking objects are in stack"); //$NON-NLS-1$
            return;
        }
        //if (blocks.size() == 1) {
        //    this.done();
        //}
        blocks.remove(blocks.size() - 1);
    }

    @Override
    public synchronized List<DBRBlockingObject> getActiveBlocks()
    {
        return blocks == null || blocks.isEmpty() ? null : new ArrayList<>(blocks);
    }

}
