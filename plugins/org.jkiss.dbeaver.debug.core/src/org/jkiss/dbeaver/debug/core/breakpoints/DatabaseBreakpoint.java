package org.jkiss.dbeaver.debug.core.breakpoints;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.Breakpoint;
import org.jkiss.dbeaver.debug.core.DebugCore;

public class DatabaseBreakpoint extends Breakpoint implements IDatabaseBreakpoint {

    @Override
    public String getModelIdentifier() {
        return DebugCore.MODEL_IDENTIFIER_DATABASE;
    }

    protected void register(boolean register) throws CoreException {
        if (register) {
            DebugPlugin plugin = DebugPlugin.getDefault();
            if (plugin != null) {
                plugin.getBreakpointManager().addBreakpoint(this);
            }
        } else {
            setRegistered(false);
        }
    }
}
