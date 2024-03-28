/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.dashboard.navigator;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardConfigurationList;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardViewer;

public class HandlerDashboardDelete extends HandlerDashboardAbstract {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        DashboardViewer view = getActiveDashboardView(event);
        if (view != null) {
            if (UIUtils.confirmAction(
                HandlerUtil.getActiveShell(event),
                "Delete dashboard",
                "Are you sure you want to delete dashboard '" + view.getConfiguration().getTitle() + "'?")
            ) {
                if (view instanceof IViewPart viewPart) {
                    HandlerUtil.getActiveWorkbenchWindow(event).getActivePage().hideView(viewPart);
                } else if (view instanceof IEditorPart editorPart) {
                    HandlerUtil.getActiveWorkbenchWindow(event).getActivePage().closeEditor(editorPart, false);
                }
                DashboardConfigurationList configurationList;
                if (view.getDataSourceContainer() != null) {
                    configurationList = new DashboardConfigurationList(view.getDataSourceContainer());
                } else {
                    configurationList = view.getConfigurationList();
                }
                configurationList.deleteDashBoard(view.getConfiguration());
            }
        }
        return null;
    }

}