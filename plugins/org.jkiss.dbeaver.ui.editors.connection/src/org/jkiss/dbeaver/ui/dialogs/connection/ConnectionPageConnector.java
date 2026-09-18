/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.connection.DBPDataSourceType;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.registry.driver.DriverUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

class ConnectionPageConnector extends ActiveWizardPage<NewConnectionWizard> {
    private DBPDataSourceType dataSourceType;
    private DBPDriver selectedDriver;
    private TableViewer viewer;

    ConnectionPageConnector(NewConnectionWizard wizard) {
        super("newConnectionDriverChoice");
        setTitle(UIConnectionMessages.dialog_new_connection_wizard_driver_title);
        setDescription(UIConnectionMessages.dialog_new_connection_wizard_driver_description);
    }

    @Override
    public void createControl(Composite parent) {
        Composite composite = UIUtils.createComposite(parent, 1);
        viewer = new TableViewer(composite, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION);
        viewer.getTable().setLayoutData(new GridData(GridData.FILL_BOTH));
        viewer.setContentProvider(ArrayContentProvider.getInstance());
        viewer.setLabelProvider(new LabelProvider() {
            @Override
            public Image getImage(Object element) {
                return DBeaverIcons.getImage(((DBPDriver) element).getIcon());
            }

            @Override
            public String getText(Object element) {
                DBPDriver driver = (DBPDriver) element;
                if (CommonUtils.isEmpty(driver.getDescription())) {
                    return driver.getName();
                }
                return driver.getName() + " - " + driver.getDescription();
            }
        });
        viewer.addSelectionChangedListener(event -> {
            Object element = ((IStructuredSelection) event.getSelection()).getFirstElement();
            selectedDriver = element instanceof DBPDriver driver ? driver : null;
            getContainer().updateButtons();
        });
        viewer.addDoubleClickListener(event -> {
            if (selectedDriver != null) {
                getContainer().showPage(getWizard().getNextPage(this));
            }
        });
        setControl(composite);
        refreshDrivers();
    }

    void setDataSourceType(DBPDataSourceType dataSourceType) {
        if (this.dataSourceType != dataSourceType) {
            this.dataSourceType = dataSourceType;
            this.selectedDriver = null;
            refreshDrivers();
        }
    }

    @Nullable
    DBPDriver getSelectedDriver() {
        return selectedDriver;
    }

    @Nullable
    DBPDataSourceType getDataSourceType() {
        return dataSourceType;
    }

    private void refreshDrivers() {
        if (viewer == null || dataSourceType == null) {
            return;
        }
        List<DBPDriver> drivers = new ArrayList<>(dataSourceType.getEnabledDrivers());
        if (DBWorkbench.isDistributed()) {
            drivers.removeIf(driver -> !driver.getDefaultDriverLoader().isDriverInstalled());
        }
        drivers.sort(new DriverUtils.DriverScoreComparator(DataSourceRegistry.getAllDataSources()));
        viewer.setInput(drivers);
        if (!drivers.isEmpty()) {
            viewer.setSelection(new StructuredSelection(drivers.get(0)), true);
        }
    }

    @Override
    public boolean isPageComplete() {
        return selectedDriver != null;
    }
}
