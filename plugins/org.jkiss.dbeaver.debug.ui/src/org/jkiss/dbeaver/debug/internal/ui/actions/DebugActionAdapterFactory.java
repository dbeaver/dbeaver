package org.jkiss.dbeaver.debug.internal.ui.actions;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.debug.ui.actions.ILaunchable;
import org.eclipse.debug.ui.actions.IToggleBreakpointsTarget;
import org.jkiss.dbeaver.debug.ui.actions.ToggleProcedureBreakpointTarget;

public class DebugActionAdapterFactory implements IAdapterFactory {

    private static final Class<?>[] CLASSES = new Class[] { ILaunchable.class, IToggleBreakpointsTarget.class };

    private static final ILaunchable LAUNCHABLE = new ILaunchable() {
    };

    private final IToggleBreakpointsTarget toggleBreakpointTarget = new ToggleProcedureBreakpointTarget();

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Object adaptableObject, Class<T> adapterType)
    {
        if (adapterType == ILaunchable.class) {
            return (T) LAUNCHABLE;
        }
        if (adapterType == IToggleBreakpointsTarget.class) {
            return (T) toggleBreakpointTarget;
        }
        return null;
    }

    @Override
    public Class<?>[] getAdapterList()
    {
        return CLASSES;
    }

}
