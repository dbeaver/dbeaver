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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.dashboard.data.DashboardDataset;
import org.jkiss.dbeaver.model.dashboard.registry.DashboardItemConfiguration;
import org.jkiss.dbeaver.ui.ShellUtils;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
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
    public void fillDashboardToolbar(DashboardItemContainer itemContainer, ToolBar toolBar, Composite chartComposite, DashboardItemViewSettings dashboardConfig) {
        super.fillDashboardToolbar(itemContainer, toolBar, chartComposite, dashboardConfig);

        UIUtils.createToolItem(toolBar, "Refresh", UIIcon.REFRESH, new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (chartComposite instanceof DashboardBrowserComposite bc) {
                    DashboardItemConfiguration dashboard = dashboardConfig.getDashboardDescriptor();
                    if (dashboard != null) {
                        bc.getBrowser().setUrl(dashboard.getDashboardURL());
                    }
                }
            }
        });
        UIUtils.createToolItem(toolBar, "Open in external browser", UIIcon.BROWSER, new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                DashboardItemConfiguration dashboard = dashboardConfig.getDashboardDescriptor();
                if (dashboard != null) {
                    String url = dashboard.getDashboardExternalURL();
                    if (CommonUtils.isEmpty(url)) {
                        url = dashboard.getDashboardURL();
                    }
                    if (CommonUtils.isEmpty(url)) {
                        return;
                    }
                    ShellUtils.launchProgram(url);
                }
            }
        });
    }

    @Override
    public void updateDashboardData(DashboardItemContainer container, Date lastUpdateTime, DashboardDataset dataset) {

    }

    @Override
    public void resetDashboardData(DashboardItemContainer dashboardItem, Date lastUpdateTime) {

    }

    @Override
    public void moveDashboardView(DashboardViewItem toItem, DashboardViewItem fromItem, boolean clearOriginal) {
        // Do nothing
    }

    @Override
    public void updateDashboardView(DashboardViewItem dashboardItem) {

    }

    @Override
    public void disposeDashboard(DashboardItemContainer container) {
        // nothing special
    }

    protected DashboardBrowserComposite getBrowserComposite(DashboardItemContainer container) {
        return (DashboardBrowserComposite) container.getDashboardControl();
    }

}
