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

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.ui.dashboard.registry.DashboardDescriptor;

public class HandlerDashboardAddItem extends HandlerDashboardAbstract {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        DashboardView view = getActiveDashboardView(event);
        if (view != null) {
            DashboardAddDialog addDialog = new DashboardAddDialog(HandlerUtil.getActiveShell(event), view.getConfiguration());
            if (addDialog.open() == IDialogConstants.OK_ID) {
                DashboardDescriptor selectedDashboard = addDialog.getSelectedDashboard();
                if (selectedDashboard != null) {
                    view.getConfiguration().readDashboardConfiguration(selectedDashboard);
                    view.getDashboardListViewer().getDefaultGroup().addItem(selectedDashboard.getId());
                }
            }
        }
        return null;
    }

}