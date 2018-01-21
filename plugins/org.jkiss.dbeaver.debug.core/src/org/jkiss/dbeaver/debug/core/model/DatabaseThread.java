/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2017 Alexander Fedorov (alexander.fedorov@jkiss.org)
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
import org.jkiss.dbeaver.debug.DBGController;
import org.jkiss.dbeaver.debug.DBGException;
import org.jkiss.dbeaver.debug.DBGStackFrame;
import org.jkiss.dbeaver.debug.core.DebugCore;

/**
 * Delegates mostly everything to its debug target
 *
 */
public abstract class DatabaseThread extends DatabaseDebugElement implements IThread {
    
    private final Object sessionKey;

    /**
     * Whether this thread is stepping
     */
    private boolean stepping = false;

    /**
     * The stackframes associated with this thread
     */
    private List<DatabaseStackFrame> fFrames = new ArrayList<>(1);

    /**
     * The stackframes to be reused on suspension
     */
    private List<DatabaseStackFrame> fOldFrames;

    public DatabaseThread(DatabaseDebugTarget target, Object sessionKey) {
        super(target);
        this.sessionKey = sessionKey;
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
        // TODO Auto-generated method stub

    }

    @Override
    public void suspend() throws DebugException {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean canStepInto() {
        DBGController controller = getController();
        return controller.canStepInto(sessionKey);
    }

    @Override
    public boolean canStepOver() {
        DBGController controller = getController();
        return controller.canStepOver(sessionKey);
    }

    @Override
    public boolean canStepReturn() {
        DBGController controller = getController();
        return controller.canStepReturn(sessionKey);
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
        DBGController controller = getController();
        try {
            controller.stepOver(sessionKey);
        } catch (DBGException e) {
            String message = NLS.bind("Step over failed for session {0}", sessionKey);
            IStatus status = DebugCore.newErrorStatus(message, e);
            throw new DebugException(status);
        }
    }

    @Override
    public void stepReturn() throws DebugException {
        DBGController controller = getController();
        try {
            controller.stepReturn(sessionKey);
        } catch (DBGException e) {
            String message = NLS.bind("Step return failed for session {0}", sessionKey);
            IStatus status = DebugCore.newErrorStatus(message, e);
            throw new DebugException(status);
        }
    }

    private void aboutToResume(int detail, boolean stepping) {
        fOldFrames = new ArrayList<>(fFrames);
        fFrames.clear();
        setStepping(stepping);
//        setBreakpoints(null);
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
        fFrames.clear();
        getDebugTarget().terminate();
    }

    @Override
    public IStackFrame[] getStackFrames() throws DebugException {
        if (isSuspended()) {
            if (fFrames.size() == 0) {
                getStackFrames0();
            }
        }

        return fFrames.toArray(new IStackFrame[fFrames.size()]);
    }

    /**
     * Retrieves the current stack frames in the thread possibly waiting until the frames are populated
     * 
     */
    private void getStackFrames0() throws DebugException {
        List<? extends DBGStackFrame> stackFrames;
        try {
            stackFrames = getDatabaseDebugTarget().getStackFrames();
            buildStack(stackFrames);
        } catch (DBGException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public boolean hasStackFrames() throws DebugException {
        return true;
    }

    public void buildStack(List<? extends DBGStackFrame> stackFrames) {
        
        if (fOldFrames != null && (stackFrames.size() - 1) / 4 != fOldFrames.size()) {
            fOldFrames.clear();
            fOldFrames = null; // stack size changed..do not preserve
        }
        for (DBGStackFrame dbgStackFrame : stackFrames) {
            addFrame(dbgStackFrame, sessionKey);
        }
        
    }

    private void addFrame(DBGStackFrame stackFrameId , Object sessionKey) {
        DatabaseStackFrame frame = getOldFrame();

        if (frame == null /*|| !frame.getFilePath().equals(filePath)*/) {
            frame = new DatabaseStackFrame(this, stackFrameId, sessionKey);
        } else {
//            frame.setFilePath(filePath);
//            frame.setId(stackFrameId);
//            frame.setLineNumber(lineNumber);
//            frame.setName(name);
        }
        fFrames.add(frame);
    }

    private DatabaseStackFrame getOldFrame() {
        if (fOldFrames == null) {
            return null;
        }
        DatabaseStackFrame frame = fOldFrames.remove(0);
        if (fOldFrames.isEmpty()) {
            fOldFrames = null;
        }
        return frame;
    }

    @Override
    public int getPriority() throws DebugException {
        // no idea for now
        return 0;
    }

    @Override
    public IStackFrame getTopStackFrame() throws DebugException {
        if (isSuspended()) {
            if (fFrames.size() == 0) {
                getStackFrames0();
            }
            if (fFrames.size() > 0) {
                return fFrames.get(0);
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
        // TODO Auto-generated method stub

    }

    public void setStepping(boolean stepping) {
        this.stepping = stepping;
    }

}
