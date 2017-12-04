package org.jkiss.dbeaver.debug.core.model;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IThread;

public class DatabaseDebugTarget extends DatabaseDebugElement implements IDatabaseDebugTarget {

    private final String modelIdentifier;

    private final ILaunch launch;
    private final IProcess process;
    private final IDatabaseDebugController controller;
    private final DatabaseThread thread;
    private final IThread[] threads;

    private boolean suspended = false;
    private boolean terminated = false;

    public DatabaseDebugTarget(String modelIdentifier, ILaunch launch, IProcess process, IDatabaseDebugController controller)
    {
        super(null);
        this.modelIdentifier = modelIdentifier;
        this.launch = launch;
        this.process = process;
        this.controller = controller;
        this.thread = new DatabaseThread(this);
        this.threads = new IThread[] {thread};
    }
    
    @Override
    public IDebugTarget getDebugTarget()
    {
        return this;
    }
    
    @Override
    public String getModelIdentifier()
    {
        return modelIdentifier;
    }
    
    @Override
    public ILaunch getLaunch()
    {
        return launch;
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
        return !terminated && threads.length > 0;
    }

    @Override
    public String getName() throws DebugException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean supportsBreakpoint(IBreakpoint breakpoint)
    {
        return false;
    }

    @Override
    public boolean canTerminate()
    {
        return !terminated;
    }

    @Override
    public boolean isTerminated()
    {
        return terminated;
    }

    @Override
    public void terminate() throws DebugException
    {
        //FIXME:AF:delegare to controller
    }

    public synchronized void terminated() {
        if (!terminated) {
            terminated = true;
            suspended = false;
            controller.terminate();
        }
    }

    @Override
    public boolean canResume()
    {
        return !terminated && suspended;
    }

    @Override
    public boolean canSuspend()
    {
        return !terminated && !suspended;
    }

    @Override
    public boolean isSuspended()
    {
        return suspended;
    }

    @Override
    public void resume() throws DebugException
    {
        suspended = false;
        controller.resume();
        if (thread.isSuspended()) {
            thread.resumedByTarget();
        }
        fireResumeEvent(DebugEvent.CLIENT_REQUEST);
    }

    @Override
    public void suspend() throws DebugException
    {
        controller.suspend();
    }

    public void suspended(int detail) {
        suspended = true;
        thread.setStepping(false);
        thread.fireSuspendEvent(detail);
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
        return null;
    }

}
