/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
 * Copyright (C) 2017-2018 Alexander Fedorov (alexander.fedorov@jkiss.org)
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

package org.jkiss.dbeaver.debug.core.model;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.*;
import org.eclipse.debug.core.model.*;
import org.eclipse.osgi.util.NLS;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.debug.*;
import org.jkiss.dbeaver.debug.core.DebugUtils;
import org.jkiss.dbeaver.debug.internal.core.DebugCoreMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DefaultProgressMonitor;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseDebugTarget extends DatabaseDebugElement implements IDatabaseDebugTarget, DBGEventHandler {

    private static final Log log = Log.getLog(DatabaseDebugTarget.class);
    public static final int BREAKPOINT_ACTION_TIMEOUT = 20000;
    public static final int SESSION_ACTION_TIMEOUT = 20000;

    private final String modelIdentifier;

    private final ILaunch launch;
    private final IProcess process;
    private final DBGController controller;
    private final List<IThread> threads;
    private final DatabaseThread thread;

    private String name;
    private String defaultName = DebugCoreMessages.DatabaseDebugTarget_name_default;

    private boolean suspended = false;
    private boolean terminated = false;

    private DBGSession session;

    public DatabaseDebugTarget(String modelIdentifier, ILaunch launch, IProcess process, DBGController controller) {
        super(null);
        this.modelIdentifier = modelIdentifier;
        this.launch = launch;
        this.process = process;
        this.controller = controller;
        this.controller.registerEventHandler(this);
        this.threads = new ArrayList<>();
        this.thread = newThread();
        this.threads.add(thread);

        DebugPlugin debugPlugin = DebugPlugin.getDefault();
        IBreakpointManager breakpointManager = debugPlugin.getBreakpointManager();
        breakpointManager.addBreakpointManagerListener(this);
        breakpointManager.addBreakpointListener(this);
        debugPlugin.addDebugEventListener(this);
    }

    public IDatabaseDebugTarget getDatabaseDebugTarget() {
        return this;
    }

    @Override
    public DBGController getController() {
        return controller;
    }

    @Override
    public DBGSession getSession() {
        return session;
    }

    protected DatabaseThread newThread() {
        return new DatabaseThread(this);
    }

    @Override
    public IDebugTarget getDebugTarget() {
        return this;
    }

    @Override
    public String getModelIdentifier() {
        return modelIdentifier;
    }

    @Override
    public ILaunch getLaunch() {
        return launch;
    }

    @Override
    public IProcess getProcess() {
        return process;
    }

    @Override
    public IThread[] getThreads() throws DebugException {
        return threads.toArray(new IThread[threads.size()]);
    }

    @Override
    public boolean hasThreads() throws DebugException {
        return !terminated && threads.size() > 0;
    }

    @Override
    public String getName() throws DebugException {
        if (name == null) {
            try {
                ILaunchConfiguration configuration = getLaunch().getLaunchConfiguration();
                name = getConfiguredName(configuration);
                if (name == null) {
                    name = getDefaultName();
                }
            } catch (CoreException e) {
                name = getDefaultName();
            }

        }
        return name;
    }

    protected String getConfiguredName(ILaunchConfiguration configuration) throws CoreException {
        return configuration.getName();
    }

    protected String getDefaultName() {
        return defaultName;
    }

    protected void setDefaultName(String defaultName) {
        this.defaultName = defaultName;
    }

    @Override
    public void handleDebugEvents(DebugEvent[] events) {
        for (DebugEvent event : events) {
            switch (event.getKind()) {
                case DebugEvent.TERMINATE:
                    if (event.getSource().equals(process)) {
                        try {
                            terminated();
                        } catch (DebugException e) {
                            log.log(e.getStatus());
                        }
                    }
                    break;
            }
        }
    }

    @Override
    public void connect(IProgressMonitor monitor) throws CoreException {
        DBRProgressMonitor dbm = new DefaultProgressMonitor(monitor);
        try {
            session = this.controller.openSession(dbm);
        } catch (DBGException e) {
            process.terminate();
            throw new CoreException(
                GeneralUtils.makeExceptionStatus(e));
        }
        // Initiate breakpoints
        IBreakpoint[] breakpoints = DebugPlugin.getDefault().getBreakpointManager().getBreakpoints(modelIdentifier);
        if (breakpoints != null) {
            for (IBreakpoint bp : breakpoints) {
                DBGBreakpointDescriptor descriptor = describeBreakpoint(bp);
                if (descriptor != null) {
                    try {
                        session.addBreakpoint(dbm, descriptor);
                    } catch (DBGException e) {
                        log.error("Can't add initial breakpoint", e);
                    }
                }
            }
        }
    }

    @Override
    public boolean canTerminate() {
        return !terminated;
    }

    @Override
    public boolean isTerminated() {
        return terminated;
    }

    @Override
    public void terminate() throws DebugException {
        terminated();
    }

    public synchronized void terminated() throws DebugException {
        if (!terminated) {
            threads.clear();
            terminated = true;
            suspended = false;
            try {
                disconnect();
                controller.unregisterEventHandler(this);
            } finally {
                controller.dispose();
            }
            DebugPlugin debugPlugin = DebugPlugin.getDefault();
            if (debugPlugin != null) {
                IBreakpointManager breakpointManager = debugPlugin.getBreakpointManager();
                breakpointManager.removeBreakpointListener(this);
                debugPlugin.removeDebugEventListener(this);
                breakpointManager.removeBreakpointManagerListener(this);
            }
            if (!getProcess().isTerminated()) {
                try {
                    process.terminate();
                } catch (DebugException e) {
                    // do nothing
                    log.debug(e);
                }
            }
            if (debugPlugin != null) {
                fireTerminateEvent();
            }
        }
    }

    @Override
    public boolean canResume() {
        return thread != null && !terminated && suspended;
    }

    @Override
    public boolean canSuspend() {
        return thread != null && !terminated && !suspended;
    }

    @Override
    public boolean isSuspended() {
        return suspended;
    }

    @Override
    public void resume() throws DebugException {
        suspended = false;
        try {
            session.resume();
        } catch (DBGException e) {
            log.error(e);
            throw new DebugException(
                GeneralUtils.makeErrorStatus(NLS.bind("Error resuming {0} - {1}", getName(), e.getMessage())));
        }
        if (thread.isSuspended()) {
            thread.resumedByTarget();
        }
        fireResumeEvent(DebugEvent.CLIENT_REQUEST);
    }

    @Override
    public void suspend() throws DebugException {
        try {
            session.suspend();
        } catch (DBGException e) {
            log.error(e);
            throw new DebugException(
                GeneralUtils.makeErrorStatus(NLS.bind("Error suspending {0} - {1}", getName(), e.getMessage())));
        }
    }

    public void suspended(int detail) {
        suspended = true;
        thread.setStepping(false);
        thread.fireSuspendEvent(detail);
    }

    @Override
    public boolean supportsBreakpoint(IBreakpoint breakpoint) {
        return breakpoint.getModelIdentifier().equals(DBGConstants.BREAKPOINT_ID_DATABASE_LINE);
    }

    @Override
    public void breakpointAdded(IBreakpoint breakpoint) {
        if (!terminated) {
            DBGBreakpointDescriptor descriptor = describeBreakpoint(breakpoint);
            if (descriptor == null) {
                log.error(NLS.bind("Unable to describe breakpoint {0}", breakpoint));
                return;
            }

            RuntimeUtils.runTask(
                monitor -> {
                    try {
                        session.addBreakpoint(monitor, descriptor);
                    } catch (DBGException e) {
                        throw new InvocationTargetException(e);
                    }
                },
                "Add session breakpoint", BREAKPOINT_ACTION_TIMEOUT);
        }
    }

    @Override
    public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
        if (!terminated) {
            DBGBreakpointDescriptor descriptor = describeBreakpoint(breakpoint);
            if (descriptor == null) {
                log.error(NLS.bind("Unable to describe breakpoint {0}", breakpoint));
                return;
            }
            RuntimeUtils.runTask(
                monitor -> {
                    try {
                        session.removeBreakpoint(monitor, descriptor);
                    } catch (DBGException e) {
                        throw new InvocationTargetException(e);
                    }
                },
                "Remove session breakpoint", BREAKPOINT_ACTION_TIMEOUT);
        }
    }

    @Override
    public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
        if (supportsBreakpoint(breakpoint)) {
            try {
                if (breakpoint.isEnabled() && DebugPlugin.getDefault().getBreakpointManager().isEnabled()) {
                    breakpointAdded(breakpoint);
                } else {
                    breakpointRemoved(breakpoint, null);
                }
            } catch (CoreException e) {
                // do nothing
            }
        }
    }

    @Override
    public void breakpointManagerEnablementChanged(boolean enabled) {
        IBreakpoint[] breakpoints = DebugPlugin.getDefault().getBreakpointManager().getBreakpoints(DBGConstants.BREAKPOINT_ID_DATABASE_LINE);
        for (IBreakpoint breakpoint : breakpoints) {
            if (enabled) {
                breakpointAdded(breakpoint);
            } else {
                breakpointRemoved(breakpoint, null);
            }
        }
    }

    protected DBGBreakpointDescriptor describeBreakpoint(IBreakpoint breakpoint) {
        Map<String, Object> description = new HashMap<>();
        try {
            Map<String, Object> attributes = breakpoint.getMarker().getAttributes();
            Map<String, Object> remote = DebugUtils.toBreakpointDescriptor(attributes);
            description.putAll(remote);
        } catch (CoreException e) {
            log.log(e.getStatus());
            return null;
        }
        return controller.describeBreakpoint(description);
    }

    @Override
    public boolean canDisconnect() {
        return session != null;
    }

    @Override
    public void disconnect() throws DebugException {
        if (session != null) {
            RuntimeUtils.runTask(
                monitor -> {
                    try {
                        session.closeSession(monitor);
                    } catch (DBGException e) {
                        throw new InvocationTargetException(e);
                    }
                },
                "Close session", SESSION_ACTION_TIMEOUT);
            session = null;
        }
    }

    @Override
    public boolean isDisconnected() {
        return session == null;
    }

    @Override
    public boolean supportsStorageRetrieval() {
        return false;
    }

    @Override
    public IMemoryBlock getMemoryBlock(long startAddress, long length) throws DebugException {
        return null;
    }

    @Override
    public void handleDebugEvent(DBGEvent event) {
        int kind = event.getKind();
        if (DBGEvent.SUSPEND == kind) {
            suspended(event.getDetails());
        }
        if (DBGEvent.TERMINATE == kind) {
            try {
                process.terminate();
            } catch (DebugException e) {
                log.log(e.getStatus());
            }
        }
    }

    public boolean canStepInto() {
        return session != null && session.canStepInto();
    }

    public boolean canStepOver() {
        return session != null && session.canStepOver();
    }

    public boolean canStepReturn() {
        return session != null && session.canStepReturn();
    }

    public void stepInto() throws DebugException {
        try {
            session.execStepInto();
        } catch (DBGException e) {
            String message = NLS.bind("Step into failed for session {0}", session.getSessionId());
            IStatus status = DebugUtils.newErrorStatus(message, e);
            throw new DebugException(status);
        }
    }

    public void stepOver() throws DebugException {
        try {
            session.execStepOver();
        } catch (DBGException e) {
            String message = NLS.bind("Step over failed for session {0}", session.getSessionId());
            IStatus status = DebugUtils.newErrorStatus(message, e);
            throw new DebugException(status);
        }
    }

    public void stepReturn() throws DebugException {
        try {
            session.execStepReturn();
        } catch (DBGException e) {
            String message = NLS.bind("Step return failed for session {0}", session.getSessionId());
            IStatus status = DebugUtils.newErrorStatus(message, e);
            throw new DebugException(status);
        }
    }

}
