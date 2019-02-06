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
package org.jkiss.dbeaver.ui.dashboard.view;

import org.eclipse.ui.*;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.dashboard.control.DashboardViewManager;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardContainer;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardGroupContainer;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardQuery;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardViewContainer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DashboardUpdater {

    private static final Log log = Log.getLog(DashboardUpdater.class);

    public void updateDashboards(DBRProgressMonitor monitor) {
        for (DashboardContainer dashboard : getDashboardsToUpdate()) {
            updateDashboard(monitor, dashboard);
        }

    }

    private void updateDashboard(DBRProgressMonitor monitor, DashboardContainer dashboard) {
        if (!dashboard.getDataSourceContainer().isConnected() || DBWorkbench.getPlatform().isShuttingDown()) {
            return;
        }

        List<? extends DashboardQuery> queries = dashboard.getQueryList();
        DashboardViewContainer view = dashboard.getGroup().getView();
        try (DBCSession session = view.getExecutionContext().openSession(
            monitor, DBCExecutionPurpose.UTIL, "Read dashboard '" + dashboard.getDashboardTitle() + "' data"))
        {
            for (DashboardQuery query : queries) {
                try (DBCStatement dbStat = session.prepareStatement(DBCStatementType.QUERY, query.getQueryText(), false, false, false)) {
                    if (dbStat.executeStatement()) {
                        try (DBCResultSet dbResults = dbStat.openResultSet()) {
                            fetchDashboardData(dashboard, dbResults);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Error updating dashboard " + dashboard.getDashboardId(), e);
        }
    }

    private void fetchDashboardData(DashboardContainer dashboard, DBCResultSet dbResults) throws DBCException {
        while (dbResults.nextRow()) {

        }
    }

    public List<DashboardContainer> getDashboardsToUpdate() {
        List<DashboardContainer> dashboards = new ArrayList<>();
        for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
            for (IWorkbenchPage page : window.getPages()) {
                for (IViewReference view : page.getViewReferences()) {
                    if (view.getId().equalsIgnoreCase(DashboardView.VIEW_ID)) {
                        IWorkbenchPart part = view.getPart(false);
                        if (part instanceof DashboardView) {
                            getViewDashboards((DashboardView) part, dashboards);
                        }
                    }
                }
            }
        }
        return dashboards;
    }

    private void getViewDashboards(DashboardView view, List<DashboardContainer> dashboards) {
        long currentTime = System.currentTimeMillis();
        DashboardViewManager viewManager = view.getDashboardViewManager();
        if (!viewManager.getDataSourceContainer().isConnected()) {
            return;
        }
        for (DashboardGroupContainer group : viewManager.getGroups()) {
            for (DashboardContainer dashboard : group.getItems()) {
                Date lastUpdateTime = dashboard.getLastUpdateTime();
                if (lastUpdateTime == null || (currentTime - lastUpdateTime.getTime()) >= dashboard.getUpdatePeriod()) {
                    dashboards.add(dashboard);
                }
            }
        }
    }

}