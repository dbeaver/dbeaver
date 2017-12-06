package org.jkiss.dbeaver.debug.core.model;

import org.eclipse.debug.core.IBreakpointManagerListener;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.model.IDebugTarget;

public interface IDatabaseDebugTarget extends IDebugTarget, IDebugEventSetListener, IBreakpointManagerListener {
    
}
