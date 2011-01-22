/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.navigator.DBNProject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.views.navigator.project.ProjectNavigatorView;

import java.lang.reflect.InvocationTargetException;

public class NavigatorHandlerProjectSelect extends NavigatorHandlerObjectBase {

    public Object execute(ExecutionEvent event) throws ExecutionException {
        final DBeaverCore core = DBeaverCore.getInstance();
        final DBNProject projectNode = core.getNavigatorModel().getRoot().getProject(core.getProjectRegistry().getActiveProject());
        if (projectNode != null) {
            final IWorkbenchWindow workbenchWindow = HandlerUtil.getActiveWorkbenchWindow(event);
            try {
                final ProjectNavigatorView projectsView = (ProjectNavigatorView)workbenchWindow.getActivePage().showView(ProjectNavigatorView.VIEW_ID);
                if (projectsView != null) {
                    projectsView.showNode(projectNode);
                }
            } catch (PartInitException e) {
                log.error(e);
            }
        }
        return null;
    }

}