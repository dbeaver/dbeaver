/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ui.navigator.actions;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.navigator.DBNProject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.navigator.project.ProjectNavigatorView;

public class NavigatorHandlerProjectSelect extends NavigatorHandlerObjectBase {

    private static final Log log = Log.getLog(NavigatorHandlerProjectSelect.class);

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final DBNProject projectNode = DBWorkbench.getPlatform().getNavigatorModel().getRoot().getProject(
            DBWorkbench.getPlatform().getProjectManager().getActiveProject());
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