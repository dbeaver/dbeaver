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
package org.jkiss.dbeaver.team.git.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.egit.ui.internal.operations.GitScopeUtil;
import org.eclipse.egit.ui.internal.selection.SelectionUtils;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;

public abstract class GITAbstractHandler extends AbstractHandler {

    protected IResource[] getResourcesInScope(ExecutionEvent event)
        throws ExecutionException {
        try {
            IResource[] selectedResources = getSelectedResources(event);
            if (selectedResources.length > 0) {
                IWorkbenchPart part = HandlerUtil.getActivePart(event);
                return GitScopeUtil.getRelatedChanges(part, selectedResources);
            } else {
                return new IResource[0];
            }
        } catch (InterruptedException e) {
            // ignore, we will not show the commit dialog in case the user
            // cancels the scope operation
            return null;
        }
    }

    protected Repository getRepository(boolean warn, ExecutionEvent event) {
        IStructuredSelection selection = HandlerUtil.getCurrentStructuredSelection(event);
        if (warn) {
            Shell shell = HandlerUtil.getActiveShell(event);
            return SelectionUtils.getRepositoryOrWarn(selection, shell);
        } else {
            return SelectionUtils.getRepository(selection);
        }
    }

    protected IResource[] getSelectedResources(ExecutionEvent event) {
        IStructuredSelection selection = HandlerUtil.getCurrentStructuredSelection(event);
        return SelectionUtils.getSelectedResources(selection);
    }

    protected Repository[] getRepositories(ExecutionEvent event) {
        IStructuredSelection selection = HandlerUtil.getCurrentStructuredSelection(event);
        return SelectionUtils.getRepositories(selection);
    }

}
