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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.AbstractDataSourceHandler;
import org.jkiss.dbeaver.ui.actions.datasource.DataSourceMenuContributor;
import org.jkiss.dbeaver.ui.dashboard.DashboardUIConstants;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardConfiguration;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardConfigurationList;
import org.jkiss.dbeaver.ui.dashboard.view.DataSourceDashboardView;

import java.util.List;
import java.util.Map;

public class DashboardDynamicMenuContributor extends DataSourceMenuContributor {

    @Override
    protected void fillContributionItems(final List<IContributionItem> menuItems) {
        IWorkbenchWindow window = UIUtils.getActiveWorkbenchWindow();
        if (window == null) {
            return;
        }
        IWorkbenchPart activePart = window.getActivePage().getActivePart();
        ISelection selection = activePart.getSite().getSelectionProvider() != null ?
            activePart.getSite().getSelectionProvider().getSelection() : null;
        DBPDataSourceContainer ds = AbstractDataSourceHandler.getActiveDataSourceContainer(
            null,
            activePart,
            selection);
        if (ds == null) {
            ds = AbstractDataSourceHandler.getActiveDataSourceContainer(
                window.getActivePage().getActiveEditor(),
                activePart,
                selection);
        }
        if (ds != null) {
            DashboardConfigurationList configurationList = new DashboardConfigurationList(ds);
            configurationList.checkDefaultDashboardExistence();
            if (configurationList.getDashboards().isEmpty()) {
                // Add fake default dashboard
                configurationList.createDashboard(
                    DashboardConfigurationList.DEFAULT_DASHBOARD_ID,
                    DashboardConfigurationList.DEFAULT_DASHBOARD_NAME);
            }
            for (DashboardConfiguration dashboard : configurationList.getDashboards()) {
                menuItems.add(ActionUtils.makeActionContribution(new ShowDashBoardAction(dashboard), true));
            }
        }

        menuItems.add(new Separator());
        menuItems.add(ActionUtils.makeCommandContribution(
            window,
            DashboardUIConstants.CMD_CREATE_DASHBOARD,
            Map.of("datasource", String.valueOf(true))));
    }


    private static class ShowDashBoardAction extends Action {
        private final DashboardConfiguration dashboard;
        public ShowDashBoardAction(DashboardConfiguration dashboard) {
            super(dashboard.getTitle());
            this.dashboard = dashboard;
        }

        @Override
        public void run() {
            DataSourceDashboardView.openView(
                UIUtils.getActiveWorkbenchWindow(),
                dashboard.getProject(),
                dashboard.getDataSourceContainer(),
                dashboard.getDashboardId());
        }
    }
}