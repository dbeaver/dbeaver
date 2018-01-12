package org.jkiss.dbeaver.debug.core.model;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IRegisterGroup;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IVariable;
import org.jkiss.dbeaver.debug.DBGStackFrame;

public class DatabaseStackFrame extends DatabaseDebugElement implements IStackFrame {
    
    private final DBGStackFrame dbgStackFrame;
    private final DatabaseThread thread;

    public DatabaseStackFrame(DatabaseThread thread, DBGStackFrame dbgStackFrame) {
        super(thread.getDatabaseDebugTarget());
        this.thread = thread;
        this.dbgStackFrame = dbgStackFrame;
    }

    @Override
    public boolean canStepInto() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean canStepOver() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean canStepReturn() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isStepping() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void stepInto() throws DebugException {
        // TODO Auto-generated method stub

    }

    @Override
    public void stepOver() throws DebugException {
        // TODO Auto-generated method stub

    }

    @Override
    public void stepReturn() throws DebugException {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean canResume() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean canSuspend() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isSuspended() {
        // TODO Auto-generated method stub
        return false;
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
    public boolean canTerminate() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isTerminated() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void terminate() throws DebugException {
        // TODO Auto-generated method stub

    }

    @Override
    public IThread getThread() {
        return thread;
    }

    @Override
    public IVariable[] getVariables() throws DebugException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasVariables() throws DebugException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int getLineNumber() throws DebugException {
        return dbgStackFrame.getLine();
    }

    @Override
    public int getCharStart() throws DebugException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getCharEnd() throws DebugException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getName() throws DebugException {
        return dbgStackFrame.toString();
    }

    @Override
    public IRegisterGroup[] getRegisterGroups() throws DebugException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasRegisterGroups() throws DebugException {
        // TODO Auto-generated method stub
        return false;
    }

}
