/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.osgi.util.NLS;
import org.jkiss.dbeaver.debug.DBGException;
import org.jkiss.dbeaver.debug.DBGSession;
import org.jkiss.dbeaver.debug.DBGStackFrame;
import org.jkiss.dbeaver.debug.core.DebugUtils;
import org.jkiss.dbeaver.debug.internal.core.DebugCoreMessages;

/**
 * Delegates mostly everything to its debug target
 *
 */
public class DatabaseThread extends DatabaseDebugElement implements IThread {

    private boolean stepping = false;

    private String name = DebugCoreMessages.DatabaseThread_name;

    private List<DatabaseStackFrame> frames = new ArrayList<>(1);

    public DatabaseThread(IDatabaseDebugTarget target) {
        super(target);
    }

    @Override
    public String getName() throws DebugException {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean canResume() {
        return getDebugTarget().canResume();
    }

    @Override
    public boolean canSuspend() {
        return getDebugTarget().canSuspend();
    }

    @Override
    public boolean isSuspended() {
        return getDebugTarget().isSuspended();
    }

    @Override
    public void resume() throws DebugException {
        aboutToResume(DebugEvent.CLIENT_REQUEST, false);
        getDebugTarget().resume();
    }

    @Override
    public void suspend() throws DebugException {
        getDebugTarget().suspend();
    }

    @Override
    public boolean canStepInto() {
        return getDatabaseDebugTarget().canStepInto();
    }

    @Override
    public boolean canStepOver() {
        return getDatabaseDebugTarget().canStepOver();
    }

    @Override
    public boolean canStepReturn() {
        return getDatabaseDebugTarget().canStepReturn();
    }

    @Override
    public boolean isStepping() {
        return stepping;
    }

    @Override
    public void stepInto() throws DebugException {
        aboutToResume(DebugEvent.STEP_INTO, true);
        getDatabaseDebugTarget().stepInto();
    }

    @Override
    public void stepOver() throws DebugException {
        aboutToResume(DebugEvent.STEP_OVER, true);
        getDatabaseDebugTarget().stepOver();
    }

    @Override
    public void stepReturn() throws DebugException {
        aboutToResume(DebugEvent.STEP_RETURN, true);
        getDatabaseDebugTarget().stepReturn();
    }

    private void aboutToResume(int detail, boolean stepping) {
        frames.clear();
        setStepping(stepping);
        // setBreakpoints(null);
        fireResumeEvent(detail);
    }

    @Override
    public boolean canTerminate() {
        return getDebugTarget().canTerminate();
    }

    @Override
    public boolean isTerminated() {
        return getDebugTarget().isTerminated();
    }

    @Override
    public void terminate() throws DebugException {
        frames.clear();
        getDebugTarget().terminate();
    }

    @Override
    public IStackFrame[] getStackFrames() throws DebugException {
        if (isSuspended()) {
            if (frames.size() == 0) {
                extractStackFrames();
            }
        }
        return frames.toArray(new IStackFrame[frames.size()]);
    }

    protected void extractStackFrames() throws DebugException {
        try {
            IDatabaseDebugTarget debugTarget = getDatabaseDebugTarget();
            DBGSession session = debugTarget.getSession();
            if (session != null) {
                List<? extends DBGStackFrame> stackFrames = session.getStack();
                rebuildStack(stackFrames);
            }
        } catch (DBGException e) {
            String message = NLS.bind("Error reading stack for {0}", getName());
            IStatus status = DebugUtils.newErrorStatus(message, e);
            throw new DebugException(status);
        }
    }

    @Override
    public boolean hasStackFrames() throws DebugException {
        return true;
    }

    public void rebuildStack(List<? extends DBGStackFrame> stackFrames) {
        for (DBGStackFrame dbgStackFrame : stackFrames) {
            addFrame(dbgStackFrame);
        }
    }

    private void addFrame(DBGStackFrame stackFrameId) {
        DatabaseStackFrame frame = new DatabaseStackFrame(this, stackFrameId);
        frames.add(frame);
    }

    @Override
    public int getPriority() throws DebugException {
        // no idea for now
        return 0;
    }

    @Override
    public IStackFrame getTopStackFrame() throws DebugException {
        if (isSuspended()) {
            if (frames.size() == 0) {
                extractStackFrames();
            }
            if (frames.size() > 0) {
                return frames.get(0);
            }
        }
        return null;
    }

    @Override
    public IBreakpoint[] getBreakpoints() {
        // TODO Auto-generated method stub
        return null;
    }

    public void resumedByTarget() {
        aboutToResume(DebugEvent.CLIENT_REQUEST, false);
    }

    public void setStepping(boolean stepping) {
        this.stepping = stepping;
    }

}
