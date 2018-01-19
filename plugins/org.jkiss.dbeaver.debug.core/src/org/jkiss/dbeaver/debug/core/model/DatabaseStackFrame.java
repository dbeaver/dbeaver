package org.jkiss.dbeaver.debug.core.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IRegisterGroup;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.osgi.util.NLS;
import org.jkiss.dbeaver.debug.DBGException;
import org.jkiss.dbeaver.debug.DBGSession;
import org.jkiss.dbeaver.debug.DBGStackFrame;
import org.jkiss.dbeaver.debug.DBGVariable;

public class DatabaseStackFrame extends DatabaseDebugElement implements IStackFrame {
    
    private static final IRegisterGroup[] NO_REGISTER_GROUPS = new IRegisterGroup[0];
    private static final IVariable[] NO_VARIABLES = new IVariable[0];
    private final DBGStackFrame dbgStackFrame;
    private final DatabaseThread thread;
    private final Object sessionKey;

    public DatabaseStackFrame(DatabaseThread thread, DBGStackFrame dbgStackFrame, Object sessionKey) {
        super(thread.getDatabaseDebugTarget());
        this.thread = thread;
        this.dbgStackFrame = dbgStackFrame;
        this.sessionKey = sessionKey;
    }

    @Override
    public boolean canStepInto() {
        return thread.canStepInto();
    }

    @Override
    public boolean canStepOver() {
       return thread.canStepOver();
    }

    @Override
    public boolean canStepReturn() {
        return thread.canStepReturn();
    }

    @Override
    public boolean isStepping() {
        return thread.isStepping();
    }

    @Override
    public void stepInto() throws DebugException {
        thread.stepInto();
    }

    @Override
    public void stepOver() throws DebugException {
        thread.stepOver();
    }

    @Override
    public void stepReturn() throws DebugException {
        thread.canStepReturn();
    }

    @Override
    public boolean canResume() {
        return thread.canResume();
    }

    @Override
    public boolean canSuspend() {
        return thread.canSuspend();
    }

    @Override
    public boolean isSuspended() {
        return thread.isSuspended();
    }

    @Override
    public void resume() throws DebugException {
        thread.resume();
    }

    @Override
    public void suspend() throws DebugException {
        thread.suspend();
    }

    @Override
    public boolean canTerminate() {
        return thread.canTerminate();
    }

    @Override
    public boolean isTerminated() {
        return thread.isTerminated();
    }

    @Override
    public void terminate() throws DebugException {
        thread.terminate();
    }

    @Override
    public IThread getThread() {
        return thread;
    }

    @Override
    public IVariable[] getVariables() throws DebugException {
        try {
            DBGSession debugSession = getController().findSession(sessionKey);
            List<? extends DBGVariable<?>> dbgVariables = debugSession.getVariables();
            if (dbgVariables.size() == 0) {
                return NO_VARIABLES;
            }
            List<DatabaseVariable> variables = new ArrayList<DatabaseVariable>();
            for (DBGVariable<?> dbgVariable : dbgVariables) {
                DatabaseVariable e = new DatabaseVariable(getDatabaseDebugTarget(), dbgVariable);
                variables.add(e);
            }
            return (DatabaseVariable[]) variables.toArray(new DatabaseVariable[variables.size()]);
        } catch (DBGException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return NO_VARIABLES;
    }

    @Override
    public boolean hasVariables() throws DebugException {
        return true;
    }

    @Override
    public int getLineNumber() throws DebugException {
        return dbgStackFrame.getLine();
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
        String name = NLS.bind(pattern, dbgStackFrame.getName(), dbgStackFrame.getLine());
        return name;
    }

    @Override
    public IRegisterGroup[] getRegisterGroups() throws DebugException {
        return NO_REGISTER_GROUPS;
    }

    @Override
    public boolean hasRegisterGroups() throws DebugException {
        return false;
    }

}
