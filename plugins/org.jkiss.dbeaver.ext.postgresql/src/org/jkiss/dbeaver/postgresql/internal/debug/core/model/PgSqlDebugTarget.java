package org.jkiss.dbeaver.postgresql.internal.debug.core.model;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IThread;
import org.jkiss.dbeaver.debug.core.model.IDatabaseDebugController;
import org.jkiss.dbeaver.debug.core.model.IDatabaseDebugTarget;

public class PgSqlDebugTarget extends PgSqlDebugElement implements IDatabaseDebugTarget {
    
    private final ILaunch launch;
    private final IProcess process;
    private final IDatabaseDebugController controller;
    private final PgSqlThread thread;
    private final IThread[] threads;

    private boolean fSuspended = false;
    private boolean fTerminated = false;

    public PgSqlDebugTarget(ILaunch launch, IProcess process, IDatabaseDebugController controller)
    {
        super(null);
        this.launch = launch;
        this.process = process;
        this.controller = controller;
        this.thread = new PgSqlThread(this);
        this.threads = new IThread[] {thread};
    }

    @Override
    public IDebugTarget getDebugTarget()
    {
        return this;
    }

    @Override
    public ILaunch getLaunch()
    {
        return launch;
    }

    @Override
    public boolean canTerminate()
    {
        return !fTerminated;
    }

    @Override
    public boolean isTerminated()
    {
        return fTerminated;
    }

    @Override
    public void terminate() throws DebugException
    {
        //FIXME:AF:delegare to controller
    }

    @Override
    public boolean canResume()
    {
        return !fTerminated && fSuspended;
    }

    @Override
    public boolean canSuspend()
    {
        return !fTerminated && !fSuspended;
    }

    @Override
    public boolean isSuspended()
    {
        return fSuspended;
    }

    @Override
    public void resume() throws DebugException
    {
        fSuspended = false;
        controller.resume();
        if (thread.isSuspended()) {
            thread.resumedByTarget();
        }
        fireResumeEvent(DebugEvent.CLIENT_REQUEST);
    }

    public void suspended(int detail) {
        fSuspended = true;
        thread.setStepping(false);
        thread.fireSuspendEvent(detail);
    }

    @Override
    public void suspend() throws DebugException
    {
        controller.suspend();
    }

    public synchronized void terminated() {
        if (!fTerminated) {
            fTerminated = true;
            fSuspended = false;
            controller.terminate();
        }
    }

    @Override
    public void breakpointAdded(IBreakpoint breakpoint)
    {
        //FIXME:AF:delegare to controller
    }

    @Override
    public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta)
    {
        //FIXME:AF:delegare to controller
    }

    @Override
    public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta)
    {
        //FIXME:AF:delegare to controller
    }

    @Override
    public boolean canDisconnect()
    {
        return false;
    }

    @Override
    public void disconnect() throws DebugException
    {
        //FIXME:AF:delegare to controller
    }

    @Override
    public boolean isDisconnected()
    {
        return false;
    }

    @Override
    public boolean supportsStorageRetrieval()
    {
        return false;
    }

    @Override
    public IMemoryBlock getMemoryBlock(long startAddress, long length) throws DebugException
    {
        //nothing for now
        return null;
    }

    @Override
    public IProcess getProcess()
    {
        return process;
    }

    @Override
    public IThread[] getThreads() throws DebugException
    {
        return threads;
    }

    @Override
    public boolean hasThreads() throws DebugException
    {
        return !fTerminated && threads.length > 0;
    }

    @Override
    public String getName() throws DebugException
    {
        return "PL/pgSQL Debug";
    }

    @Override
    public boolean supportsBreakpoint(IBreakpoint breakpoint)
    {
        return false;
    }

}
