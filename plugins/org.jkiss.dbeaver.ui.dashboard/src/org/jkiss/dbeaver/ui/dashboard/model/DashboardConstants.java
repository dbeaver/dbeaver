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

/**
 * Dashboard value calculation
 */
public class DashboardConstants {

    public static final String PREF_OPEN_SEPARATE_CONNECTION = "dashboard.openSeparateConnection";

    public static final String RS_COL_TIMESTAMP = "STAT_TIMESTAMP";

    public static final int DEF_DASHBOARD_MAXIMUM_ITEM_COUNT = 300;
    public static final long DEF_DASHBOARD_MAXIMUM_AGE = 30 * 60 * 1000; // Half of hour

    public static final int DEF_DASHBOARD_UPDATE_PERIOD = 5000;
    public static final double DEF_DASHBOARD_WIDTH_RATIO = 1.5;
    public static final DashboardCalcType DEF_DASHBOARD_CALC_TYPE = DashboardCalcType.value;
    public static final DashboardFetchType DEF_DASHBOARD_FETCH_TYPE = DashboardFetchType.columns;

    public static final String CMD_ADD_DASHBOARD = "org.jkiss.dbeaver.ui.dashboard.add";
    public static final String CMD_REMOVE_DASHBOARD = "org.jkiss.dbeaver.ui.dashboard.remove";
    public static final String CMD_RESET_DASHBOARD = "org.jkiss.dbeaver.ui.dashboard.reset";
}
