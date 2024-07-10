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
package org.jkiss.dbeaver.ui.dashboard.browser;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.dashboard.data.DashboardDataset;
import org.jkiss.dbeaver.model.dashboard.registry.DashboardItemConfiguration;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.ShellUtils;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.dashboard.control.DashboardRendererAbstract;
import org.jkiss.dbeaver.ui.dashboard.control.DashboardViewItem;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardContainer;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardItemContainer;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardItemViewSettings;
import org.jkiss.utils.CommonUtils;

import java.util.Date;

/**
 * Browser dashboard renderer
 */
public class DashboardRendererBrowser extends DashboardRendererAbstract {

    @Override
    public Composite createDashboard(@NotNull Composite composite, @NotNull DashboardItemContainer container, @NotNull DashboardContainer viewContainer, @NotNull Point preferredSize) {
        DashboardBrowserComposite browserComposite = new DashboardBrowserComposite(container, viewContainer, composite, SWT.NONE, preferredSize);
        return browserComposite;
    }

    @Override
    public void fillDashboardToolbar(
        @NotNull DashboardItemContainer itemContainer,
        @NotNull IContributionManager manager,
        @NotNull Composite chartComposite,
        @NotNull DashboardItemViewSettings dashboardConfig
    ) {
        manager.add(new Action("Open in external browser", DBeaverIcons.getImageDescriptor(UIIcon.LINK)) {
            @Override
            public void run() {
                DashboardItemConfiguration dashboard = dashboardConfig.getItemConfiguration();
                if (dashboard != null) {
                    String url = dashboard.getDashboardExternalURL();
                    if (CommonUtils.isEmpty(url)) {
                        url = dashboard.getDashboardURL();
                    }
                    if (CommonUtils.isEmpty(url)) {
                        return;
                    }
                    ShellUtils.launchProgram(dashboard.evaluateURL(url, itemContainer.getProject(), itemContainer.getDataSourceContainer()));
                }
            }
        });

        super.fillDashboardToolbar(itemContainer, manager, chartComposite, dashboardConfig);
    }

    @Override
    public void updateDashboardData(@NotNull DashboardItemContainer container, @Nullable Date lastUpdateTime, @NotNull DashboardDataset dataset) {

    }

    @Override
    public void resetDashboardData(@NotNull DashboardItemContainer dashboardItem, Date lastUpdateTime) {

    }

    @Override
    public void moveDashboardView(@NotNull DashboardViewItem toItem, @NotNull DashboardViewItem fromItem, boolean clearOriginal) {
        // Do nothing
    }

    @Override
    public void updateDashboardView(@NotNull DashboardViewItem dashboardItem) {

    }

    @Override
    public void disposeDashboard(@NotNull DashboardItemContainer container) {
        // nothing special
    }

    protected DashboardBrowserComposite getBrowserComposite(DashboardItemContainer container) {
        return (DashboardBrowserComposite) container.getDashboardControl();
    }

    @Override
    protected void refreshChart(DashboardItemContainer itemContainer, Composite chartComposite, DashboardItemViewSettings dashboardConfig) {
        if (chartComposite instanceof DashboardBrowserComposite bc) {
            DashboardItemConfiguration dashboard = dashboardConfig.getItemConfiguration();
            if (dashboard != null) {
                bc.getBrowser().setUrl(dashboard.evaluateURL(dashboard.getDashboardURL(), itemContainer.getProject(),
                    itemContainer.getDataSourceContainer()));
                itemContainer.refreshInfo();
            }
        }
    }
}
