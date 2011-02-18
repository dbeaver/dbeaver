/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.navigator.DBNProject;
import org.jkiss.dbeaver.registry.ProjectRegistry;

public class NavigatorHandlerProjectSetActive extends NavigatorHandlerObjectBase {

    public Object execute(ExecutionEvent event) throws ExecutionException {
        final ISelection selection = HandlerUtil.getCurrentSelection(event);

        if (selection instanceof IStructuredSelection) {
            final IStructuredSelection structSelection = (IStructuredSelection)selection;
            Object element = structSelection.getFirstElement();
            if (!(element instanceof DBNProject)) {
                return null;
            }
            DBNProject projectNode = (DBNProject)element;
            final ProjectRegistry projectRegistry = DBeaverCore.getInstance().getProjectRegistry();
            if (projectRegistry.getActiveProject() != projectNode.getProject()) {
                projectRegistry.setActiveProject(projectNode.getProject());
            }
        }
        return null;
    }

}