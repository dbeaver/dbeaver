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
package org.jkiss.dbeaver.ui.datadam.project.event;

import com.dbeaver.datadam.dam.remote.api.model.DDSharedProjectEvent;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.datadam.rpc.DDSharedProjectEventSource;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.datadam.project.DDProjectUIManager;
import org.jkiss.dbeaver.ui.datadam.project.event.actions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DDProjectEventListener implements DDSharedProjectEventSource.Subscriber {
    private static final Log log = Log.getLog(DDProjectEventListener.class);
    private final DDProjectUIManager uiManager;
    private final List<DDSharedProjectEvent> pendingEvents = new ArrayList<>();
    private State state = State.STOPPED;
    private boolean syncRunning;
    private CompletableFuture<Shell> waitingForShell;

    private enum State {
        STOPPED, SYNCHRONIZING, LISTENING
    }

    public DDProjectEventListener(@NotNull DDProjectUIManager uiManager) {
        this.uiManager = uiManager;
    }

    @Override
    public synchronized void start() {
        if (state != State.STOPPED) {
            return;
        }
        state = State.SYNCHRONIZING;
        log.debug("Started DataDam project stream sync");
        if (!syncRunning) {
            startSynchronization();
        }
    }

    @Override
    public synchronized void stop() {
        if (state != State.STOPPED) {
            log.debug("Stopped DataDam project stream sync");
        }
        state = State.STOPPED;
        pendingEvents.clear();
        if (waitingForShell != null) {
            waitingForShell.cancel(false);
            waitingForShell = null;
            syncRunning = false;
        }
    }

    @Override
    public synchronized void onEvent(@NotNull DDSharedProjectEvent event) throws DBException {
        switch (state) {
            case STOPPED -> log.debug("Ignoring shared project event " + event.change()
                + " for project " + event.projectId() + ": stream sync stopped");
            case SYNCHRONIZING -> pendingEvents.add(event);
            case LISTENING -> DDSharedProjectEventSource.Subscriber.super.onEvent(event);
            default -> throw new IllegalStateException("Unexpected project sync state: " + state);
        }
    }

    private void startSynchronization() {
        syncRunning = true;
        CompletableFuture<Shell> ready = waitForWorkbenchShell();
        waitingForShell = ready;
        ready.thenAccept(shell -> {
            synchronized (DDProjectEventListener.this) {
                if (!ready.equals(waitingForShell) || state == State.STOPPED) {
                    return;
                }
                waitingForShell = null;
                SyncAllJob job = new SyncAllJob(uiManager, shell);
                job.addJobChangeListener(new JobChangeAdapter() {
                    @Override
                    public void done(@NotNull IJobChangeEvent event) {
                        job.removeJobChangeListener(this);
                        synchronizationFinished();
                    }
                });
                job.setUser(true);
                job.schedule();
            }
        });
    }

    @NotNull
    private CompletableFuture<Shell> waitForWorkbenchShell() {
        CompletableFuture<Shell> ready = new CompletableFuture<>();
        UIUtils.asyncExec(new Runnable() {
            @Override
            public void run() {
                if (ready.isDone()) {
                    return;
                }
                if (PlatformUI.isWorkbenchRunning()) {
                    for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
                        Shell shell = window.getShell();
                        if (shell != null && !shell.isDisposed() && shell.isVisible()) {
                            ready.complete(shell);
                            return;
                        }
                    }
                }
                Display.getCurrent().timerExec(250, this);
            }
        });
        return ready;
    }

    private synchronized void synchronizationFinished() {
        syncRunning = false;
        if (state == State.STOPPED) {
            return;
        }
        state = State.LISTENING;
        for (DDSharedProjectEvent event : pendingEvents) {
            try {
                DDSharedProjectEventSource.Subscriber.super.onEvent(event);
            } catch (DBException | RuntimeException e) {
                log.warn("Could not process buffered shared project event", e);
            }
        }
        pendingEvents.clear();
    }

    @Override
    public void projectCreated(@NotNull DDSharedProjectEvent event) throws DBException {
        schedule(new ProjectCreatedAction(uiManager, event.projectId()));
    }

    @Override
    public void projectMetadataUpdated(@NotNull DDSharedProjectEvent event) throws DBException {
        log.debug("Shared project metadata updated: " + event.projectId());
        // Metadata is currently fetched through the client whenever it is needed.
    }

    @Override
    public void projectConfigurationUpdated(@NotNull DDSharedProjectEvent event) throws DBException {
        schedule(new ProjectConfigurationUpdatedAction(uiManager, event.projectId()));
    }

    @Override
    public void projectDeleted(@NotNull DDSharedProjectEvent event) throws DBException {
        schedule(new ProjectDeletedAction(uiManager, event.projectId()));
    }

    private void schedule(@NotNull AbstractProjectEventJob job) {
        job.setUser(true);
        job.schedule();
    }
}
