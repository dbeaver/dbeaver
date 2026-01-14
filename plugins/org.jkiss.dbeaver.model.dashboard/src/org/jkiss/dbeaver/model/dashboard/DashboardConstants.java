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
package org.jkiss.dbeaver.model.dashboard;

/**
 * Dashboard value calculation
 */
public class DashboardConstants {

    public static final String PREF_OPEN_SEPARATE_CONNECTION = "dashboard.openSeparateConnection";

    public static final String DS_PROP_DASHBOARDS = "dashboards";

    public static final String RS_COL_TIMESTAMP = "STAT_TIMESTAMP";

    public static final int DEF_DASHBOARD_MAXIMUM_ITEM_COUNT = 300;
    public static final long DEF_DASHBOARD_MAXIMUM_AGE = 30 * 60 * 1000; // Half of hour

    public static final DBDashboardDataType DEF_DASHBOARD_DATA_TYPE = DBDashboardDataType.timeseries;
    public static final int DEF_DASHBOARD_UPDATE_PERIOD = 1000;
    public static final float DEF_DASHBOARD_WIDTH_RATIO = 1.5f;
    public static final DBDashboardCalcType DEF_DASHBOARD_CALC_TYPE = DBDashboardCalcType.value;
    public static final DBDashboardValueType DEF_DASHBOARD_VALUE_TYPE = DBDashboardValueType.decimal;
    public static final DBDashboardFetchType DEF_DASHBOARD_FETCH_TYPE = DBDashboardFetchType.columns;

    public static final String DEF_DASHBOARD_PROVIDER = "database";
    public static final String DEF_DASHBOARD_VIEW_TYPE = "timeseries";
    public static final DBDashboardInterval DEF_DASHBOARD_INTERVAL = DBDashboardInterval.millisecond;

    public static final String DASHBOARDS_PLUGIN_ID = "org.jkiss.dbeaver.model.dashboard";
    public static final String DASHBOARDS_LEGACY_PLUGIN_ID = "org.jkiss.dbeaver.model.dashboard";
    public static final String DASHBOARDS_LEGACY_PLUGIN_ID2 = "org.jkiss.dbeaver.ui.dashboard";
    public static final String DASHBOARD_EXT = "dashboard"; //$NON-NLS-1$
}
