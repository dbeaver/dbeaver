/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.navigator.DBNProject;
import org.jkiss.dbeaver.ui.views.navigator.project.ProjectNavigatorView;

public class NavigatorHandlerProjectSelect extends NavigatorHandlerObjectBase {

    @Override
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