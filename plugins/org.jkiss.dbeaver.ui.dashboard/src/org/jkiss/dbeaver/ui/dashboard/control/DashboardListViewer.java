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

import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IWorkbenchSite;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPEvent;
import org.jkiss.dbeaver.model.DBPEventListener;
import org.jkiss.dbeaver.model.IDataSourceContainerProvider;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardContainer;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardGroupContainer;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardViewConfiguration;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardViewContainer;

import java.util.Collections;
import java.util.List;

public class DashboardListViewer extends StructuredViewer implements DBPEventListener, IDataSourceContainerProvider, DashboardViewContainer {

    private final IWorkbenchSite site;
    private final DBPDataSourceContainer dataSourceContainer;
    private final DashboardViewConfiguration viewConfiguration;
    private DashboardList dashContainer;
    //private CLabel statusLabel;

    public DashboardListViewer(IWorkbenchSite site, DBPDataSourceContainer dataSourceContainer, DashboardViewConfiguration viewConfiguration) {
        this.site = site;
        this.dataSourceContainer = dataSourceContainer;
        this.dataSourceContainer.getRegistry().addDataSourceListener(this);

        if (!this.dataSourceContainer.isConnected()) {
            //DataSourceConnectHandler
        }

        this.viewConfiguration = viewConfiguration;

        // Activate updater
    }

    public void dispose() {
        dataSourceContainer.getRegistry().removeDataSourceListener(this);
    }

    @Override
    public void handleDataSourceEvent(DBPEvent event) {
        if (event.getObject() != dataSourceContainer) {
            return;
        }
        switch (event.getAction()) {
            case OBJECT_UPDATE:
            case OBJECT_REMOVE:
                UIUtils.asyncExec(this::updateStatus);
                break;
        }
    }

    public void createControl(Composite parent) {
        Composite composite = UIUtils.createComposite(parent, 1);

        dashContainer = new DashboardList(site, composite, this);
        dashContainer.setLayoutData(new GridData(GridData.FILL_BOTH));

//        statusLabel = new CLabel(composite, SWT.NONE);
//        statusLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        updateStatus();

        dashContainer.createDefaultDashboards();
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
        return Collections.singletonList(dashContainer);
    }

    @Override
    public DBCExecutionContext getExecutionContext() {
        return dataSourceContainer.getDataSource().getDefaultInstance().getDefaultContext(false);
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

}
