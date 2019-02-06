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

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPEvent;
import org.jkiss.dbeaver.model.DBPEventListener;
import org.jkiss.dbeaver.model.IDataSourceContainerProvider;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardGroupContainer;

import java.util.Collections;
import java.util.List;

public class DashboardViewManager implements DBPEventListener, IDataSourceContainerProvider {

    private final DBPDataSourceContainer dataSourceContainer;
    private DashboardList dashContainer;
    private CLabel statusLabel;

    public DashboardViewManager(DBPDataSourceContainer dataSourceContainer) {
        this.dataSourceContainer = dataSourceContainer;
        this.dataSourceContainer.getRegistry().addDataSourceListener(this);

        if (!this.dataSourceContainer.isConnected()) {
            //DataSourceConnectHandler
        }
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

        dashContainer = new DashboardList(composite, this);
        dashContainer.setLayoutData(new GridData(GridData.FILL_BOTH));

        statusLabel = new CLabel(composite, SWT.NONE);
        statusLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        updateStatus();

        dashContainer.createDefaultDashboards();
    }

    private void updateStatus() {
        String status = dataSourceContainer.isConnected() ? "connected (" + dataSourceContainer.getConnectTime() + ")" : "disconnected";
        statusLabel.setImage(DBeaverIcons.getImage(dataSourceContainer.getDriver().getIcon()));
        statusLabel.setText(this.dataSourceContainer.getName() + ": " + status);
    }

    @Override
    public DBPDataSourceContainer getDataSourceContainer() {
        return dataSourceContainer;
    }

    public List<? extends DashboardGroupContainer> getDashboardGroups() {
        return Collections.singletonList(dashContainer);
    }
}
