package org.jkiss.dbeaver.debug.ui.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.actions.IToggleBreakpointsTargetExtension2;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.debug.core.DebugCore;
import org.jkiss.dbeaver.debug.core.breakpoints.DatabaseLineBreakpoint;
import org.jkiss.dbeaver.model.app.DBPProjectManager;

public class ToggleProcedureBreakpointTarget implements IToggleBreakpointsTargetExtension2 {
    
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
        IEditorPart editorPart = (IEditorPart) part;
        IResource resource = extractResource(editorPart, selection);
        if (resource == null) {
            return;
        }

        ITextSelection textSelection = (ITextSelection) selection;
        int lineNumber = textSelection.getStartLine();
        IBreakpoint[] breakpoints = DebugPlugin.getDefault().getBreakpointManager().getBreakpoints(DebugCore.MODEL_IDENTIFIER_DATABASE);
        for (int i = 0; i < breakpoints.length; i++) {
            IBreakpoint breakpoint = breakpoints[i];
            if (resource.equals(breakpoint.getMarker().getResource())) {
                if (((ILineBreakpoint) breakpoint).getLineNumber() == (lineNumber + 1)) {
                    DebugUITools.deleteBreakpoints(new IBreakpoint[] { breakpoint }, part.getSite().getShell(), null);
                    return;
                }
            }
        }
        int charstart = -1, charend = -1;
        // create line breakpoint (doc line numbers start at 0)
        new DatabaseLineBreakpoint(resource, lineNumber + 1, charstart, charend, true);
    }

    protected IResource extractResource(IEditorPart part, ISelection selection)
    {
        //FIXME: AF: resolve more specific resource
        DBPProjectManager projectManager = DBeaverCore.getInstance().getProjectManager();
        IProject activeProject = projectManager.getActiveProject();
        return activeProject;
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
