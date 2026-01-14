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
package org.jkiss.dbeaver.ui.dashboard.control;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dashboard.DashboardUIConstants;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardItemContainer;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardItemRenderer;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardItemViewSettings;
import org.jkiss.dbeaver.ui.dashboard.navigator.HandlerDashboardViewItem;
import org.jkiss.dbeaver.ui.dashboard.view.DashboardItemViewSettingsDialog;

/**
 * Abstract chart dashboard renderer
 */
public abstract class DashboardRendererAbstract implements DashboardItemRenderer {

    public void fillDashboardToolbar(
        @NotNull DashboardItemContainer itemContainer,
        @NotNull IContributionManager manager,
        @NotNull Composite chartComposite,
        @NotNull DashboardItemViewSettings dashboardConfig
    ) {
        if (!UIUtils.isInDialog(chartComposite)) {
            manager.add(new Separator());
            manager.add(new Action("View in popup", DBeaverIcons.getImageDescriptor(UIIcon.FIT_WINDOW)) {
                @Override
                public void run() {
                    HandlerDashboardViewItem.openDashboardViewDialog(itemContainer);
                }
            });
            manager.add(new Action("Refresh chart", DBeaverIcons.getImageDescriptor(UIIcon.REFRESH)) {
                @Override
                public void run() {
                    refreshChart(itemContainer, chartComposite, dashboardConfig);
                }
            });
            manager.add(new Action("Settings", DBeaverIcons.getImageDescriptor(UIIcon.CONFIGURATION)) {
                @Override
                public void run() {
                    DashboardItemViewSettingsDialog dialog = new DashboardItemViewSettingsDialog(
                        UIUtils.getActiveShell(), itemContainer, itemContainer.getItemConfiguration().getViewConfiguration());
                    dialog.open();
                }
            });
            manager.add(new Separator());
            manager.add(new Action("Close", DBeaverIcons.getImageDescriptor(UIIcon.CLOSE)) {
                @Override
                public void run() {
                    itemContainer.getGroup().selectItem(itemContainer);
                    ActionUtils.runCommand(
                        DashboardUIConstants.CMD_REMOVE_DASHBOARD,
                        itemContainer.getGroup().getView().getWorkbenchSite());
                }
            });
        }
    }

    protected void refreshChart(DashboardItemContainer itemContainer, Composite chartComposite, DashboardItemViewSettings dashboardConfig){
        itemContainer.getGroup().selectItem(itemContainer);
        ActionUtils.runCommand(
            DashboardUIConstants.CMD_REFRESH_CHART,
            itemContainer.getGroup().getView().getWorkbenchSite());
    }

}
