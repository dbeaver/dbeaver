/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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