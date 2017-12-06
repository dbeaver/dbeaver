package org.jkiss.dbeaver.debug.core.model;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

public interface IDatabaseDebugController {

    public IStatus connect(IProgressMonitor monitor);

    public void resume();

    public void suspend();

    public void terminate();

}
