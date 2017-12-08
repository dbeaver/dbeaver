package org.jkiss.dbeaver.debug.ui.actions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.ui.actions.IToggleBreakpointsTargetExtension2;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IWorkbenchPart;

public class ToggleSqlBreakpointTarget implements IToggleBreakpointsTargetExtension2 {
    
    @Override
    public void toggleBreakpoints(IWorkbenchPart part, ISelection selection) throws CoreException
    {
        toggleLineBreakpoints(part, selection);
    }

    @Override
    public boolean canToggleBreakpoints(IWorkbenchPart part, ISelection selection)
    {
        return canToggleLineBreakpoints(part, selection);
    }

    @Override
    public void toggleLineBreakpoints(IWorkbenchPart part, ISelection selection) throws CoreException
    {
        //nothing by default
    }

    @Override
    public boolean canToggleLineBreakpoints(IWorkbenchPart part, ISelection selection)
    {
        return true;
    }

    @Override
    public void toggleMethodBreakpoints(IWorkbenchPart part, ISelection selection) throws CoreException
    {
        //nothing by default
    }

    @Override
    public boolean canToggleMethodBreakpoints(IWorkbenchPart part, ISelection selection)
    {
        return false;
    }

    @Override
    public void toggleWatchpoints(IWorkbenchPart part, ISelection selection) throws CoreException
    {
        //nothing by default
    }

    @Override
    public boolean canToggleWatchpoints(IWorkbenchPart part, ISelection selection)
    {
        return false;
    }

    @Override
    public void toggleBreakpointsWithEvent(IWorkbenchPart part, ISelection selection, Event event) throws CoreException
    {
        //nothing by default
    }

    @Override
    public boolean canToggleBreakpointsWithEvent(IWorkbenchPart part, ISelection selection, Event event)
    {
        return false;
    }

}
