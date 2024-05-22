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
import org.jkiss.dbeaver.ui.dashboard.control.DashboardViewItem;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardItemContainer;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardViewer;
import org.jkiss.dbeaver.ui.dashboard.view.DashboardItemViewDialog;
import org.jkiss.dbeaver.ui.dashboard.view.DataSourceDashboardView;

public class HandlerDashboardViewItem extends HandlerDashboardAbstract {

    public static final String CMD_ID = "org.jkiss.dbeaver.ui.dashboard.view";

    public static void openDashboardViewDialog(DashboardItemContainer itemContainer) {
        DashboardViewItem viewItem = (DashboardViewItem) itemContainer;
        if (viewItem.getGroupContainer().getView().getWorkbenchPart() instanceof DataSourceDashboardView view) {
            DashboardItemViewDialog viewDialog = new DashboardItemViewDialog(
                view.getDashboardListViewer(),
                view.getConfigurationList(),
                viewItem);
            viewDialog.setModeless(true);
            viewDialog.open();
        }
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        DashboardViewer view = getActiveDashboardView(event);
        if (view != null) {
            DashboardItemContainer selectedDashboard = getSelectedDashboard(view);
            if (selectedDashboard != null) {
                DashboardItemViewDialog viewDialog = new DashboardItemViewDialog(
                    view.getDashboardListViewer(),
                    view.getConfigurationList(),
                    (DashboardViewItem) selectedDashboard);
                viewDialog.open();

            }
        }
        return null;
    }

}