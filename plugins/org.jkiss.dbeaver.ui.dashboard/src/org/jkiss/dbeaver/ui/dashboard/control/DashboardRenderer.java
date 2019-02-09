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
package org.jkiss.dbeaver.ui.dashboard.control;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardContainer;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardViewConfiguration;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardViewContainer;
import org.jkiss.dbeaver.ui.dashboard.model.data.DashboardDataset;

import java.util.Date;

/**
 * Dashboard renderer
 */
public interface DashboardRenderer {

    DashboardChartComposite createDashboard(Composite composite, DashboardContainer container, DashboardViewContainer viewContainer, Point preferredSize);

    void updateDashboardData(DashboardContainer container, Date lastUpdateTime, DashboardDataset dataset);

    void copyDashboardData(DashboardItem dashboardItem, DashboardItem fromItem);

    void resetDashboardData(DashboardContainer dashboardItem, Date lastUpdateTime);

    void disposeDashboard(DashboardContainer container);

}
