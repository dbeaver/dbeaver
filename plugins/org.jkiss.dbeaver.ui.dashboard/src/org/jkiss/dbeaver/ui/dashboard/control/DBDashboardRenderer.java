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

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.dashboard.data.DashboardDataset;
import org.jkiss.dbeaver.ui.dashboard.model.DBDashboardContainer;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardItemViewConfiguration;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardViewContainer;

import java.util.Date;

/**
 * Dashboard renderer
 */
public interface DBDashboardRenderer {

    /**
     * Returns composite with embedded dashboard.
     * It may be DashboardChartComposite or DashboardBrowserComposite or some other implementation of DBDashboardControl
     */
    Composite createDashboard(
        @NotNull Composite composite,
        @NotNull DBDashboardContainer container,
        @NotNull DashboardViewContainer viewContainer,
        @NotNull Point preferredSize);

    void fillDashboardToolbar(ToolBar toolBar, Composite chartComposite, DashboardItemViewConfiguration dashboardConfig);

    void updateDashboardData(DBDashboardContainer container, Date lastUpdateTime, DashboardDataset dataset);

    void resetDashboardData(DBDashboardContainer dashboardItem, Date lastUpdateTime);

    void moveDashboardView(DBDashboardItem toItem, DBDashboardItem fromItem, boolean clearOriginal);

    void updateDashboardView(DBDashboardItem dashboardItem);

    void disposeDashboard(DBDashboardContainer container);

}
