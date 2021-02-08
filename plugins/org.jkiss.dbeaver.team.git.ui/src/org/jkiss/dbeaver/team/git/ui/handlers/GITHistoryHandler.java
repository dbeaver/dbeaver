/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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


import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.egit.ui.internal.history.HistoryPageInput;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.SWT;
import org.eclipse.team.ui.history.IHistoryView;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.ArrayUtils;

import java.util.Arrays;
import java.util.stream.Collectors;

public class GITHistoryHandler extends GITAbstractHandler {


    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Repository[] repos = this.getRepositories(event);
        if (ArrayUtils.isEmpty(repos)) {
            IResource[] selectedResources = getSelectedResources(event);
            if (ArrayUtils.isEmpty(selectedResources)) {
                UIUtils.showMessageBox(HandlerUtil.getActiveShell(event),
                    "No repository",
                    "You need to select a resource to view Git history", SWT.ICON_WARNING);
            } else {
                String resourceNames = Arrays.stream(selectedResources).map(r -> r.getFullPath().toString()).collect(Collectors.joining(","));
                UIUtils.showMessageBox(HandlerUtil.getActiveShell(event),
                    "No repository",
                    "No Git repository associated with selected resource(s):\n" + resourceNames, SWT.ICON_WARNING);
            }
            return null;
        } else {
            try {
                IWorkbenchWindow activeWorkbenchWindow = HandlerUtil.getActiveWorkbenchWindow(event);
                if (activeWorkbenchWindow != null) {
                    IWorkbenchPage page = activeWorkbenchWindow.getActivePage();
                    if (page != null) {
                        IResource[] resources = this.getSelectedResources(event);
                        IHistoryView view = (IHistoryView)page.showView("org.eclipse.team.ui.GenericHistoryView");
                        if (resources.length == 1) {
                            view.showHistoryFor(resources[0]);
                            return null;
                        }

                        HistoryPageInput list = new HistoryPageInput(repos[0], resources);
                        view.showHistoryFor(list);
                    }
                }
                return null;
            } catch (PartInitException var8) {
                throw new ExecutionException(var8.getMessage(), var8);
            }
        }
    }

}
