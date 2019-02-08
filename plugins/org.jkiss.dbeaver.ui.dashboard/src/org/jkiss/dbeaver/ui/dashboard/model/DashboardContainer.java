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
package org.jkiss.dbeaver.ui.dashboard.model;

import org.eclipse.swt.widgets.Control;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.ui.dashboard.model.data.DashboardDataset;

import java.util.Date;
import java.util.List;

/**
 * Dashboard container
 */
public interface DashboardContainer {

    String getDashboardId();

    String getDashboardTitle();

    String getDashboardDescription();

    DashboardType getDashboardType();

    DashboardCalcType getDashboardCalcType();

    DashboardValueType getDashboardValueType();

    DashboardFetchType getDashboardFetchType();

    /**
     * Maximum item counts
     */
    int getDashboardMaxItems();

    /**
     * Maximum age in ms
     */
    long getDashboardMaxAge();

    DBPDataSourceContainer getDataSourceContainer();

    DashboardGroupContainer getGroup();

    List<? extends DashboardQuery> getQueryList();

    Date getLastUpdateTime();

    void updateDashboardData(DashboardDataset dataset);

    void resetDashboardData();

    /**
     * Dashboard update period in seconds
     */
    long getUpdatePeriod();

    Control getDashboardControl();

}
