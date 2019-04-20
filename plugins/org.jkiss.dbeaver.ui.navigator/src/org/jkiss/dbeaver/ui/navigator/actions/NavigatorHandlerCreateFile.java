/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.model.app.DBPProjectManager;
import org.jkiss.dbeaver.model.app.DBPResourceCreator;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.runtime.DBWorkbench;

public class NavigatorHandlerCreateFile extends NavigatorHandlerObjectBase {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final ISelection selection = HandlerUtil.getCurrentSelection(event);

        if (selection instanceof IStructuredSelection) {
            final IStructuredSelection structSelection = (IStructuredSelection)selection;
            Object element = structSelection.getFirstElement();
            if (!(element instanceof DBNResource)) {
                return null;
            }
            Shell activeShell = HandlerUtil.getActiveShell(event);

            final DBPProjectManager projectRegistry = DBWorkbench.getPlatform().getProjectManager();
            IResource resource = ((DBNResource) element).getResource();
            DBPResourceCreator handler = (DBPResourceCreator) projectRegistry.getResourceHandler(resource);
            if (resource instanceof IFolder) {
                try {
                    handler.createResource((IFolder) resource);
                } catch (Exception e) {
                    DBWorkbench.getPlatformUI().showError(
                        "Can't create file",
                        "Error during " + handler.getTypeName(resource) + " creation",
                        e);
                }
            }
        }
        return null;
    }

}