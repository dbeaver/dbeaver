package org.jkiss.dbeaver.debug.internal.ui.actions;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.debug.ui.actions.IToggleBreakpointsTarget;
import org.jkiss.dbeaver.debug.ui.actions.ToggleSqlBreakpointTarget;

public class DebugActionAdapterFactory implements IAdapterFactory {

    private static final Class<?>[] CLASSES = new Class[]{IToggleBreakpointsTarget.class};

    private final IToggleBreakpointsTarget toggleBreakpointTarget = new ToggleSqlBreakpointTarget();

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Object adaptableObject, Class<T> adapterType)
    {
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
