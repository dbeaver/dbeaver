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
package org.jkiss.dbeaver.ui.dashboard.view;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardContainer;

public abstract class HandlerDashboardAbstract extends AbstractHandler {

    protected DashboardView getActiveDashboardView(ExecutionEvent event) {
        IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
        if (activePart instanceof DashboardView) {
            return (DashboardView) activePart;
        }
        return null;
    }

    protected DashboardContainer getSelectedDashboard(DashboardView view) {
        ISelection selection = view.getSite().getSelectionProvider().getSelection();
        if (!selection.isEmpty() && selection instanceof IStructuredSelection) {
            Object firstElement = ((IStructuredSelection) selection).getFirstElement();
            if (firstElement instanceof DashboardContainer) {
                return (DashboardContainer) firstElement;
            }
        }
        return null;
    }

}