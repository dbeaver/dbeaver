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

import org.apache.commons.jexl3.JexlExpression;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.widgets.Control;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.dashboard.DBDashboardMapQuery;
import org.jkiss.dbeaver.model.dashboard.DBDashboardQuery;
import org.jkiss.dbeaver.model.dashboard.data.DashboardDataset;
import org.jkiss.dbeaver.model.dashboard.registry.DashboardItemConfiguration;

import java.util.Date;
import java.util.List;

/**
 * Dashboard container
 */
public interface DashboardItemContainer {

    DashboardItemConfiguration getItemDescriptor();

    DashboardItemViewSettings getItemConfiguration();

    /**
     * Maximum item counts
     */
    int getDashboardMaxItems();

    /**
     * Maximum age in ms
     */
    long getDashboardMaxAge();

    /**
     * Dashboard update period in seconds
     */
    long getUpdatePeriod();

    DBPProject getProject();

    DBPDataSourceContainer getDataSourceContainer();

    DashboardGroupContainer getGroup();

    DBDashboardMapQuery getMapQuery();

    String[] getMapKeys();

    String[] getMapLabels();

    JexlExpression getMapFormula();

    List<? extends DBDashboardQuery> getQueryList();

    Date getLastUpdateTime();

    void updateDashboardData(DashboardDataset dataset);

    void resetDashboardData();

    void updateDashboardView();

    boolean isAutoUpdateEnabled();

    void disableAutoUpdate();

    Control getDashboardControl();

    void fillDashboardContextMenu(
        @NotNull IMenuManager manager,
        boolean singleChartMode);

    void refreshInfo();
}
