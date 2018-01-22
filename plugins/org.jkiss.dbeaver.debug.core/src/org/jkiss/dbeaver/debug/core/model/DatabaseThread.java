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

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IVariable;
import org.jkiss.dbeaver.debug.DBGController;
import org.jkiss.dbeaver.debug.DBGException;
import org.jkiss.dbeaver.debug.DBGStackFrame;
import org.jkiss.dbeaver.debug.DBGVariable;

/**
 * Delegates mostly everything to its debug target
 *
 */
public abstract class DatabaseThread extends DatabaseDebugElement implements IThread {
    
    private final Object sessionKey;

    private boolean stepping = false;

    private boolean fRefreshProperties = true;
    private List<DatabaseStackFrame> frames = new ArrayList<>(1);
    private List<DatabaseStackFrame> cachedFrames;

    private List<DatabaseVariable> variables;

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
        aboutToResume(DebugEvent.STEP_OVER, true);
        getDatabaseDebugTarget().stepOver();
    }

    @Override
    public void stepReturn() throws DebugException {
        aboutToResume(DebugEvent.STEP_RETURN, true);
        getDatabaseDebugTarget().stepOver();
    }

    private void aboutToResume(int detail, boolean stepping) {
        fRefreshProperties = true;
        cachedFrames = new ArrayList<>(frames);
        frames.clear();
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
        List<? extends DBGStackFrame> stackFrames;
        try {
            stackFrames = getDatabaseDebugTarget().requestStackFrames();
            rebuildStack(stackFrames);
        } catch (DBGException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public boolean hasStackFrames() throws DebugException {
        return true;
    }

    public void rebuildStack(List<? extends DBGStackFrame> stackFrames) {
        //FIXME:AF:revisit this check
        if (cachedFrames != null && (stackFrames.size() - 1) / 4 != cachedFrames.size()) {
            cachedFrames.clear();
            cachedFrames = null; // stack size changed..do not preserve
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
        frames.add(frame);
    }

    private DatabaseStackFrame getOldFrame() {
        if (cachedFrames == null) {
            return null;
        }
        DatabaseStackFrame frame = cachedFrames.remove(0);
        if (cachedFrames.isEmpty()) {
            cachedFrames = null;
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

    protected IVariable[] requestVariables() throws DebugException {
        if (fRefreshProperties) {
            try {
                List<? extends DBGVariable<?>> variables = getDatabaseDebugTarget().requestVariables();
                rebuildVariables(variables);
            } catch (DBGException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return (IVariable[]) variables.toArray(new IVariable[variables.size()]);
    }

    public void rebuildVariables(List<? extends DBGVariable<?>> dbgVariables) {
        try {
            variables.clear();
            for (DBGVariable<?> dbgVariable : dbgVariables) {
                DatabaseVariable variable = new DatabaseVariable(getDatabaseDebugTarget(), dbgVariable);
                variables.add(variable);
            }
        } finally {
            fRefreshProperties = false;
        }
    }

}
