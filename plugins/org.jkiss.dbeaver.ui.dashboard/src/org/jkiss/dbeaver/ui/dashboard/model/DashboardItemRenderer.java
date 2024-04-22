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
package org.jkiss.dbeaver.ui.dashboard.model;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.dashboard.data.DashboardDataset;
import org.jkiss.dbeaver.ui.dashboard.control.DashboardViewItem;

import java.util.Date;

/**
 * Dashboard renderer
 */
public interface DashboardItemRenderer {

    /**
     * Returns composite with embedded dashboard.
     * It may be DashboardChartComposite or DashboardBrowserComposite or some other implementation of DBDashboardControl
     */
    Composite createDashboard(
        @NotNull Composite composite,
        @NotNull DashboardItemContainer container,
        @NotNull DashboardContainer viewContainer,
        @NotNull Point preferredSize);

    void fillDashboardToolbar(DashboardItemContainer itemContainer, ToolBar toolBar, Composite chartComposite, DashboardItemViewSettings dashboardConfig);

    void updateDashboardData(DashboardItemContainer container, Date lastUpdateTime, DashboardDataset dataset);

    void resetDashboardData(DashboardItemContainer dashboardItem, Date lastUpdateTime);

    void moveDashboardView(DashboardViewItem toItem, DashboardViewItem fromItem, boolean clearOriginal);

    void updateDashboardView(DashboardViewItem dashboardItem);

    void disposeDashboard(DashboardItemContainer container);

}
