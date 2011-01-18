/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.ui.UIUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;

public class NavigatorHandlerProjectDelete extends NavigatorHandlerObjectBase {

    public Object execute(ExecutionEvent event) throws ExecutionException {

        final ISelection selection = HandlerUtil.getCurrentSelection(event);

        if (selection instanceof IStructuredSelection) {
            IStructuredSelection structSelection = (IStructuredSelection)selection;
            for (Iterator iter = structSelection.iterator(); iter.hasNext(); ) {
                Object element = iter.next();
                IProject project = (IProject)Platform.getAdapterManager().getAdapter(element, IProject.class);
                if (project != null) {
                    deleteProject(HandlerUtil.getActiveWorkbenchWindow(event), project);
                }
            }
        }
        return null;
    }

    private void deleteProject(IWorkbenchWindow workbenchWindow, final IProject project)
    {
        if (project == DBeaverCore.getInstance().getProjectRegistry().getActiveProject()) {
            UIUtils.showErrorDialog(workbenchWindow.getShell(), "Delete project", "Active project cannot be deleted");
            return;
        }
        DBeaverCore.getInstance().runAndWait(new DBRRunnableWithProgress() {
            public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
            {
                try {
                    project.delete(true, monitor.getNestedMonitor());
                } catch (CoreException e) {
                    throw new InvocationTargetException(e);
                }
            }
        });
    }

}