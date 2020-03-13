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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IRegisterGroup;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.osgi.util.NLS;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.debug.DBGException;
import org.jkiss.dbeaver.debug.DBGStackFrame;
import org.jkiss.dbeaver.debug.DBGVariable;
import org.jkiss.dbeaver.debug.core.DebugUtils;

public class DatabaseStackFrame extends DatabaseDebugElement implements IStackFrame {

    private static final IRegisterGroup[] NO_REGISTER_GROUPS = new IRegisterGroup[0];
    private static final IVariable[] NO_VARIABLES = new IVariable[0];

    private static Log log = Log.getLog(DatabaseStackFrame.class);

    private final List<DatabaseVariable> variables = new ArrayList<>();

    private final DatabaseThread thread;
    private final DBGStackFrame dbgStackFrame;

    private boolean refreshVariables = true;

    public DatabaseStackFrame(DatabaseThread thread, DBGStackFrame dbgStackFrame) {
        super(thread.getDatabaseDebugTarget());
        this.thread = thread;
        this.dbgStackFrame = dbgStackFrame;
    }

    @Override
    public boolean canStepInto() {
        return getThread().canStepInto();
    }

    @Override
    public boolean canStepOver() {
        return getThread().canStepOver();
    }

    @Override
    public boolean canStepReturn() {
        return getThread().canStepReturn();
    }

    @Override
    public boolean isStepping() {
        return getThread().isStepping();
    }

    @Override
    public void stepInto() throws DebugException {
        getThread().stepInto();
    }

    @Override
    public void stepOver() throws DebugException {
        getThread().stepOver();
    }

    @Override
    public void stepReturn() throws DebugException {
        getThread().canStepReturn();
    }

    @Override
    public boolean canResume() {
        return getThread().canResume();
    }

    @Override
    public boolean canSuspend() {
        return getThread().canSuspend();
    }

    @Override
    public boolean isSuspended() {
        return getThread().isSuspended();
    }

    @Override
    public void resume() throws DebugException {
        getThread().resume();
    }

    @Override
    public void suspend() throws DebugException {
        getThread().suspend();
    }

    @Override
    public boolean canTerminate() {
        return getThread().canTerminate();
    }

    @Override
    public boolean isTerminated() {
        return getThread().isTerminated();
    }

    @Override
    public void terminate() throws DebugException {
        getThread().terminate();
    }

    @Override
    public IThread getThread() {
        return thread;
    }

    @Override
    public IVariable[] getVariables() throws DebugException {
        /*if (refreshVariables)*/ {
            try {
                IDatabaseDebugTarget debugTarget = getDatabaseDebugTarget();
                List<? extends DBGVariable<?>> variables = debugTarget.getSession().getVariables(dbgStackFrame);
                rebuildVariables(variables);
            } catch (DBGException e) {
                log.debug("Error getting variables", e);
            }
        }
        if (variables.isEmpty()) {
            return NO_VARIABLES;
        }
        return variables.toArray(new IVariable[variables.size()]);
    }

    protected void invalidateVariables() {
        refreshVariables = true;
    }

    protected void rebuildVariables(List<? extends DBGVariable<?>> dbgVariables) {
        try {
            int frameLN = dbgStackFrame.getLineNumber();
            variables.clear();
            Map<String, DBGVariable<?>> filtered = new LinkedHashMap<>();
            for (DBGVariable<?> dbgVariable : dbgVariables) {
                String name = dbgVariable.getName();
                DBGVariable<?> existing = filtered.get(name);
                if (existing == null) {
                    filtered.put(name, dbgVariable);
                } else {
                    int existingLN = existing.getLineNumber();
                    int currentLN = dbgVariable.getLineNumber();
                    int delta = currentLN - existingLN;
                    if (delta >= 0) {
                        filtered.put(name, dbgVariable);
                    } else {
                        String pattern = "Already have {0} and ignored {1} for frame at {2}";
                        String message = NLS.bind(pattern, new Object[]{existing, dbgVariable, frameLN});
                        log.error(message);
                    }
                }
            }
            
            for (DBGVariable<?> dbgVariable : filtered.values()) {
                DatabaseVariable variable = new DatabaseVariable(getDatabaseDebugTarget(), dbgVariable);
                variables.add(variable);
            }
        } finally {
            refreshVariables = false;
        }
    }

    @Override
    public boolean hasVariables() throws DebugException {
        return isSuspended();
    }

    @Override
    public int getLineNumber() throws DebugException {
        return dbgStackFrame.getLineNumber();
    }

    @Override
    public int getCharStart() throws DebugException {
        // unknown
        return -1;
    }

    @Override
    public int getCharEnd() throws DebugException {
        // unknown
        return -1;
    }

    @Override
    public String getName() throws DebugException {
        String pattern = "{0} line: {1}";
        return NLS.bind(pattern, dbgStackFrame.getName(), dbgStackFrame.getLineNumber());
    }

    @Override
    public IRegisterGroup[] getRegisterGroups() throws DebugException {
        return NO_REGISTER_GROUPS;
    }

    @Override
    public boolean hasRegisterGroups() throws DebugException {
        return false;
    }

    public String getSource() throws DebugException {
        String source;
        try {
            IDatabaseDebugTarget debugTarget = getDatabaseDebugTarget();
            source = debugTarget.getSession().getSource(dbgStackFrame);
        } catch (DBGException e) {
            String message = NLS.bind("Unable to retrieve sources for stack {0}", dbgStackFrame);
            IStatus status = DebugUtils.newErrorStatus(message, e);
            throw new DebugException(status);
        }
        return source;
    }

    public Object getSourceIdentifier() {
        return dbgStackFrame.getSourceIdentifier();
    }

}
