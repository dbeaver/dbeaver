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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardContainer;
import org.jkiss.dbeaver.ui.dashboard.registry.DashboardDescriptor;

public class DashboardItem extends Composite implements DashboardContainer {

    private DashboardDescriptor dashboardDescriptor;

    public DashboardItem(DashboardList parent, DashboardDescriptor dashboardDescriptor) {
        super(parent, SWT.BORDER);
        GridLayout layout = new GridLayout(1, true);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        setLayout(layout);

        this.dashboardDescriptor = dashboardDescriptor;

        try {
            DashboardRenderer renderer = dashboardDescriptor.getType().createRenderer();
            Control dashboardControl = renderer.createDashboard(this, this, computeSize(-1, -1));
        } catch (DBException e) {
            // Something went wrong
            Text errorLabel = new Text(this, SWT.READ_ONLY | SWT.MULTI | SWT.WRAP);
            errorLabel.setText("Error creating " + dashboardDescriptor.getLabel() + " renderer: " + e.getMessage());
            errorLabel.setLayoutData(new GridData(GridData.CENTER, GridData.CENTER, true, true));
        }
    }


    public DashboardList getParent() {
        return (DashboardList) super.getParent();
    }

    @Override
    public Point computeSize(int wHint, int hHint, boolean changed) {
        return new Point(300, 200);//super.computeSize(wHint, hHint, changed);
    }

    @Override
    public String getDashboardTitle() {
        return dashboardDescriptor.getLabel();
    }

    @Override
    public String getDashboardDescription() {
        return dashboardDescriptor.getDescription();
    }

    @Override
    public DBPDataSourceContainer getDataSourceContainer() {
        return getParent().getDataSourceContainer();
    }
}
