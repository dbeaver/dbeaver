package org.jkiss.dbeaver.postgresql.internal.debug.core.model;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IThread;

public class PgSqlDebugTarget extends PgSqlDebugElement implements IDebugTarget {
    
    private final ILaunch launch;

//FIXME: AF: pass PgDebugSession here to use as controller
    public PgSqlDebugTarget(ILaunch launch)
    {
        super(null);
        this.launch = launch;
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
        return true;
    }

    @Override
    public boolean isTerminated()
    {
        return false;
    }

    @Override
    public void terminate() throws DebugException
    {
        //FIXME:AF:delegare to controller
    }

    @Override
    public boolean canResume()
    {
        return false;
    }

    @Override
    public boolean canSuspend()
    {
        return false;
    }

    @Override
    public boolean isSuspended()
    {
        return false;
    }

    @Override
    public void resume() throws DebugException
    {
        //FIXME:AF:delegare to controller
    }

    @Override
    public void suspend() throws DebugException
    {
        //FIXME:AF:delegare to controller
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
        //FIXME:AF: implement PgProcess
        return null;
    }

    @Override
    public IThread[] getThreads() throws DebugException
    {
      //FIXME: AF: implement PgSqlThread
        return new IThread[0];
    }

    @Override
    public boolean hasThreads() throws DebugException
    {
        return false;
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
