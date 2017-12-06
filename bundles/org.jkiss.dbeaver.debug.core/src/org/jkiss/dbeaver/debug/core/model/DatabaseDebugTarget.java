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

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IThread;

public abstract class DatabaseDebugTarget<C extends IDatabaseDebugController> extends DatabaseDebugElement implements IDatabaseDebugTarget {

    private final String modelIdentifier;

    private final ILaunch launch;
    private final IProcess process;
    private final C controller;
    private final DatabaseThread thread;
    private final IThread[] threads;

    private String name;

    private boolean suspended = false;
    private boolean terminated = false;

    public DatabaseDebugTarget(String modelIdentifier, ILaunch launch, IProcess process, C controller)
    {
        super(null);
        this.modelIdentifier = modelIdentifier;
        this.launch = launch;
        this.process = process;
        this.controller = controller;
        this.thread = newThread(controller);
        this.threads = new IThread[] {thread};
    }
    
    protected abstract DatabaseThread newThread(C controller);

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
        if (name == null) {
            try {
                ILaunchConfiguration configuration = getLaunch().getLaunchConfiguration();
                name = getConfiguredName(configuration);
                if (name == null) {
                    name = getDefaultName();
                }
            }
            catch (CoreException e) {
                name = getDefaultName();
            }
            
        }
        return name;
    }
    
    protected abstract String getConfiguredName(ILaunchConfiguration configuration) throws CoreException;
    protected abstract String getDefaultName();

    @Override
    public void handleDebugEvents(DebugEvent[] events)
    {
        for (int i = 0; i < events.length; i++) {
            DebugEvent event = events[i];
            if (event.getKind() == DebugEvent.TERMINATE && event.getSource().equals(process)) {
                terminated();
            }
        }
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
        terminated();
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
        return thread!= null && !terminated && suspended;
    }

    @Override
    public boolean canSuspend()
    {
        return thread!= null && !terminated && !suspended;
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
    public boolean supportsBreakpoint(IBreakpoint breakpoint)
    {
        return false;
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
    public void breakpointManagerEnablementChanged(boolean enabled)
    {
        // TODO Auto-generated method stub
        
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
