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

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.IDataSourceContainerProvider;
import org.jkiss.dbeaver.ui.dashboard.control.DashboardListViewer;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardViewConfiguration;
import org.jkiss.utils.CommonUtils;

public class DashboardView extends ViewPart implements IDataSourceContainerProvider {
    public static final String VIEW_ID = "org.jkiss.dbeaver.ui.dashboardView";

    private DashboardListViewer dashboardListViewer;
    private DashboardViewConfiguration configuration;
    private int viewNumber;

    public DashboardView() {
        super();
    }

    @Override
    public void createPartControl(Composite parent) {
        String secondaryId = getViewSite().getSecondaryId();
        if (CommonUtils.isEmpty(secondaryId)) {
            throw new IllegalStateException("Dashboard view requires active database connection");
        }
        int divPos = secondaryId.lastIndexOf(':');
        String dataSourceId = divPos == -1 ? secondaryId : secondaryId.substring(0, divPos);
        viewNumber = divPos == -1 ? 0 : CommonUtils.toInt(secondaryId.substring(divPos + 1));

        DBPDataSourceContainer dataSource = DBUtils.findDataSource(dataSourceId);
        if (dataSource == null) {
            throw new IllegalStateException("Database connection '" + dataSourceId + "' not found");
        }
        setPartName(dataSource.getName());

        configuration = new DashboardViewConfiguration(secondaryId);
        dashboardListViewer = new DashboardListViewer(dataSource, configuration);
        dashboardListViewer.createControl(parent);
    }

    @Override
    public void setFocus() {

    }

    @Override
    public void init(IViewSite site) throws PartInitException {
        super.init(site);

    }

    @Override
    public void dispose() {
        super.dispose();
        if (dashboardListViewer != null) {
            dashboardListViewer.dispose();
            dashboardListViewer = null;
        }
    }

    @Override
    public void saveState(IMemento memento) {
        super.saveState(memento);
    }

    public DashboardListViewer getDashboardListViewer() {
        return dashboardListViewer;
    }

    @Override
    public DBPDataSourceContainer getDataSourceContainer() {
        return null;
    }

}
