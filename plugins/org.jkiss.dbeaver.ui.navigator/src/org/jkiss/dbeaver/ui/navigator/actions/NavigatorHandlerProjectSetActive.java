/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.navigator.DBNProject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.actions.GlobalPropertyTester;

public class NavigatorHandlerProjectSetActive extends NavigatorHandlerObjectBase {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final ISelection selection = HandlerUtil.getCurrentSelection(event);

        if (selection instanceof IStructuredSelection) {
            final IStructuredSelection structSelection = (IStructuredSelection)selection;
            Object element = structSelection.getFirstElement();
            if (!(element instanceof DBNProject)) {
                return null;
            }
            DBNProject projectNode = (DBNProject)element;
            setActiveProject(projectNode.getProject());
        }
        return null;
    }

    static void setActiveProject(DBPProject project) {
        DBPWorkspace workspace = DBWorkbench.getPlatform().getWorkspace();
        if (workspace.getActiveProject() != project) {
            workspace.setActiveProject(project);

            GlobalPropertyTester.firePropertyChange(GlobalPropertyTester.PROP_HAS_ACTIVE_PROJECT);
        }
    }

}