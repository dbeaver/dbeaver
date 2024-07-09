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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchSite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.dashboard.registry.DashboardRegistry;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSInstance;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.UIServiceConnections;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dashboard.model.*;
import org.jkiss.dbeaver.ui.dashboard.view.catalogpanel.DashboardCatalogPanel;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class DashboardListViewer extends StructuredViewer implements DBPDataSourceContainerProvider, DashboardContainer {

    @NotNull
    private final IWorkbenchSite site;
    @Nullable
    private final IWorkbenchPart part;
    @NotNull
    private final DashboardConfigurationList configuration;
    @NotNull
    private final DashboardConfiguration viewConfiguration;

    private volatile boolean useSeparateConnection;
    @Nullable
    private volatile DBCExecutionContext isolatedContext;

    private DashboardListControl dashContainer;
    private boolean singleChartMode;
    //private CLabel statusLabel;

    private final Consumer<Object> dashboardsConfigChangedListener = a -> UIUtils.asyncExec(() -> {
        dashContainer.setRedraw(false);

        dashContainer.clear();
        dashContainer.createDefaultDashboards();

        dashContainer.layout(true, true);
        dashContainer.setRedraw(true);
    });
    private SashForm dashDivider;
    private DashboardCatalogPanel catalogPanel;
    private boolean isCatalogPanelVisible;

    public DashboardListViewer(
        @NotNull IWorkbenchSite site,
        @Nullable IWorkbenchPart part,
        @NotNull DashboardConfigurationList configuration,
        @NotNull DashboardConfiguration viewConfiguration
    ) {
        this.site = site;
        this.part = part;
        this.configuration = configuration;
        this.viewConfiguration = viewConfiguration;

        initConnection();
    }

    public void dispose() {
        WorkspaceConfigEventManager.removeConfigChangedListener(DashboardRegistry.CONFIG_FILE_NAME, dashboardsConfigChangedListener);

        DBCExecutionContext context = isolatedContext;
        if (context != null) {
            if (context.isConnected()) {
                context.close();
            }
            isolatedContext = null;
        }
    }

    @Override
    @NotNull
    public DashboardConfigurationList getConfiguration() {
        return configuration;
    }

    @Override
    public boolean isSingleChartMode() {
        return singleChartMode;
    }

    public void setSingleChartMode(boolean singleChartMode) {
        this.singleChartMode = singleChartMode;
    }

    public void createControl(Composite parent) {
        dashDivider = UIUtils.createPartDivider(part, parent, SWT.HORIZONTAL);
        dashContainer = new DashboardListControl(site, dashDivider, this);

        catalogPanel = new DashboardCatalogPanel(
            dashDivider,
            viewConfiguration.getProject(),
            viewConfiguration.getDataSourceContainer(),
            item -> viewConfiguration.getItemConfig(item.getId()) != null,
            true) {
            @Override
            protected void handleChartSelected() {
                //enableButton(IDialogConstants.OK_ID, getSelectedDashboard() != null);
            }

            @Override
            protected void handleChartSelectedFinal() {
                dashContainer.addItem(getSelectedDashboard());
            }
        };

        dashDivider.setWeights(650, 350);
        dashDivider.setMaximizedControl(dashContainer);

        updateStatus();

    }

    @Override
    public ISelection getSelection() {
        return getStructuredSelection();
    }

    @Override
    public IStructuredSelection getStructuredSelection() {
        DashboardViewItem selectedItem = dashContainer.getSelectedItem();
        return selectedItem == null ? new StructuredSelection() : new StructuredSelection(selectedItem);
    }

    public void createDashboardsFromConfiguration() {
        if (viewConfiguration.getDashboardItemConfigs().isEmpty()) {
            dashContainer.createDefaultDashboards();
            WorkspaceConfigEventManager.addConfigChangedListener(DashboardRegistry.CONFIG_FILE_NAME, dashboardsConfigChangedListener); 
        } else {
            dashContainer.createDashboardsFromConfiguration();
        }
        if (viewConfiguration.getDashboardItemConfigs().isEmpty()) {
            dashDivider.setMaximizedControl(null);
        }
    }

    private void updateStatus() {
//        String status = dataSourceContainer.isConnected() ? "connected (" + dataSourceContainer.getConnectTime() + ")" : "disconnected";
//        statusLabel.setImage(DBeaverIcons.getImage(dataSourceContainer.getDriver().getIcon()));
//        statusLabel.setText(this.dataSourceContainer.getName() + ": " + status);
    }

    @Nullable
    @Override
    public DBPDataSourceContainer getDataSourceContainer() {
        return configuration.getDataSourceContainer();
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
        DBPDataSourceContainer dataSourceContainer = getDataSourceContainer();
        if (dataSourceContainer == null) {
            return null;
        }
        return DBUtils.getDefaultContext(dataSourceContainer.getDataSource().getDefaultInstance(), true);
    }

    @NotNull
    @Override
    public DashboardConfiguration getViewConfiguration() {
        return viewConfiguration;
    }

    @NotNull
    @Override
    public IWorkbenchSite getWorkbenchSite() {
        return site;
    }

    @Nullable
    @Override
    public IWorkbenchPart getWorkbenchPart() {
        return part;
    }

    @Override
    public void updateSelection() {
        fireSelectionChanged(new SelectionChangedEvent(this, getSelection()));
    }

    @Override
    public void showChartCatalog() {
        if (dashDivider.getMaximizedControl() != null) {
            dashDivider.setMaximizedControl(null);
        } else if (dashDivider.getWeights()[1] == 0) {
            dashDivider.setWeights(650, 350);
        }
        catalogPanel.setFocus();
         isCatalogPanelVisible = true;
    }

    @Override
    public void hideChartCatalog() {
        if (dashDivider.getMaximizedControl() != null) {
            dashDivider.setMaximizedControl(null);
        } else {
            dashDivider.setWeights(100, 0);
        }
        isCatalogPanelVisible = false;
    }

    @Override
    public void saveChanges() {
        try {
            configuration.saveConfiguration();
        } catch (IOException e) {
            DBWorkbench.getPlatformUI().showError("Save error", null, e);
        }
    }

    @Override
    protected DashboardViewItem doFindInputItem(Object element) {
        return null;
    }

    @Override
    protected DashboardViewItem doFindItem(Object element) {
        return null;
    }

    @Override
    protected void doUpdateItem(Widget item, Object element, boolean fullMap) {

    }

    @Override
    protected List<?> getSelectionFromWidget() {
        DashboardItemContainer selectedItem = dashContainer.getSelectedItem();
        return selectedItem == null ? Collections.emptyList() : Collections.singletonList(selectedItem);
    }

    @Override
    protected void internalRefresh(Object element) {

    }

    @Override
    public void reveal(Object element) {
        DashboardItemContainer item = doFindItem(element);
        if (item != null) {
            dashContainer.showItem(item);
        }
    }

    @Override
    protected void setSelectionToWidget(List l, boolean reveal) {
        if (l.isEmpty()) {
            dashContainer.setSelection(null);
        } else {
            DashboardViewItem item = doFindItem(l.get(0));
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
        useSeparateConnection = viewConfiguration.isUseSeparateConnection();
        if (viewConfiguration.isOpenConnectionOnActivate()) {
            DBPDataSourceContainer dataSourceContainer = getDataSourceContainer();
            if (dataSourceContainer != null && !dataSourceContainer.isConnected()) {
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
        DBPDataSourceContainer dataSourceContainer = getDataSourceContainer();
        if (dataSourceContainer == null) {
            return;
        }
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

    /**
     * Gets visibility flag 
     */
    public boolean isVisible() {
        return isCatalogPanelVisible;
    }

}
