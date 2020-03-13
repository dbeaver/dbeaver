/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IWorkbenchSite;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.IDataSourceContainerProvider;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSInstance;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.UIServiceConnections;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardContainer;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardGroupContainer;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardViewConfiguration;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardViewContainer;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.util.Collections;
import java.util.List;

public class DashboardListViewer extends StructuredViewer implements IDataSourceContainerProvider, DashboardViewContainer {

    private final IWorkbenchSite site;
    private final DBPDataSourceContainer dataSourceContainer;
    private final DashboardViewConfiguration viewConfiguration;

    private volatile boolean useSeparateConnection;
    private volatile DBCExecutionContext isolatedContext;

    private DashboardList dashContainer;
    private boolean singleChartMode;
    //private CLabel statusLabel;

    public DashboardListViewer(IWorkbenchSite site, DBPDataSourceContainer dataSourceContainer, DashboardViewConfiguration viewConfiguration) {
        this.site = site;
        this.dataSourceContainer = dataSourceContainer;

        if (!this.dataSourceContainer.isConnected()) {
            //DataSourceConnectHandler
        }

        this.viewConfiguration = viewConfiguration;

        initConnection();
    }

    public void dispose() {
        if (isolatedContext != null) {
            if (isolatedContext.isConnected()) {
                isolatedContext.close();
            }
            isolatedContext = null;
        }
    }

    @Override
    public boolean isSingleChartMode() {
        return singleChartMode;
    }

    public void setSingleChartMode(boolean singleChartMode) {
        this.singleChartMode = singleChartMode;
    }

    public void createControl(Composite parent) {
        dashContainer = new DashboardList(site, parent, this);

        //dashContainer.setLayoutData(new GridData(GridData.FILL_BOTH));

//        statusLabel = new CLabel(composite, SWT.NONE);
//        statusLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        updateStatus();

    }

    public void createDashboardsFromConfiguration() {
        if (viewConfiguration.getDashboardItemConfigs().isEmpty()) {
            dashContainer.createDefaultDashboards();
        } else {
            dashContainer.createDashboardsFromConfiguration();
        }
    }

    private void updateStatus() {
//        String status = dataSourceContainer.isConnected() ? "connected (" + dataSourceContainer.getConnectTime() + ")" : "disconnected";
//        statusLabel.setImage(DBeaverIcons.getImage(dataSourceContainer.getDriver().getIcon()));
//        statusLabel.setText(this.dataSourceContainer.getName() + ": " + status);
    }

    @Override
    public DBPDataSourceContainer getDataSourceContainer() {
        return dataSourceContainer;
    }

    @Override
    public List<? extends DashboardGroupContainer> getGroups() {
        return dashContainer == null ? Collections.emptyList() : Collections.singletonList(dashContainer);
    }

    @Override
    public DBCExecutionContext getExecutionContext() {
        if (useSeparateConnection && isolatedContext != null) {
            return isolatedContext;
        }
        return DBUtils.getDefaultContext(dataSourceContainer.getDataSource().getDefaultInstance(), true);
    }

    @Override
    public DashboardViewConfiguration getViewConfiguration() {
        return viewConfiguration;
    }

    @Override
    public IWorkbenchSite getSite() {
        return site;
    }

    @Override
    protected DashboardItem doFindInputItem(Object element) {
        return null;
    }

    @Override
    protected DashboardItem doFindItem(Object element) {
        return null;
    }

    @Override
    protected void doUpdateItem(Widget item, Object element, boolean fullMap) {

    }

    @Override
    protected List getSelectionFromWidget() {
        DashboardContainer selectedItem = dashContainer.getSelectedItem();
        return selectedItem == null ? Collections.emptyList() : Collections.singletonList(selectedItem);
    }

    @Override
    protected void internalRefresh(Object element) {

    }

    @Override
    public void reveal(Object element) {
        DashboardContainer item = doFindItem(element);
        if (item != null) {
            dashContainer.showItem(item);
        }
    }

    @Override
    protected void setSelectionToWidget(List l, boolean reveal) {
        if (l.isEmpty()) {
            dashContainer.setSelection(null);
        } else {
            DashboardItem item = doFindItem(l.get(0));
            if (item != null) {
                dashContainer.setSelection(item);
            }
        }
    }

    @Override
    public Control getControl() {
        return dashContainer;
    }

    public DashboardGroupContainer getDefaultGroup() {
        return dashContainer;
    }

    private void initConnection() {
        useSeparateConnection = false;//viewConfiguration.isUseSeparateConnection();
        if (viewConfiguration.isOpenConnectionOnActivate()) {
            if (!dataSourceContainer.isConnected()) {
                UIServiceConnections serviceConnections = DBWorkbench.getService(UIServiceConnections.class);
                if (serviceConnections != null) {
                    serviceConnections.connectDataSource(dataSourceContainer, status -> {
                        if (useSeparateConnection) {
                            openSeparateContext();
                        }
                    });
                }
            } else if (useSeparateConnection) {
                openSeparateContext();
            }
        } else if (useSeparateConnection) {
            openSeparateContext();
        }
    }

    private void openSeparateContext() {
        DBPDataSource dataSource = dataSourceContainer.getDataSource();
        if (dataSource == null) {
            return;
        }
        new AbstractJob("Open connection for dashboard") {
            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                DBSInstance instance = DBUtils.getObjectOwnerInstance(dataSource);
                if (instance != null) {
                    try {
                        isolatedContext = instance.openIsolatedContext(monitor, "Dashboard connection", null);
                    } catch (DBException e) {
                        return GeneralUtils.makeExceptionStatus(e);
                    }
                }
                return Status.OK_STATUS;
            }
        }.schedule();
    }

}
