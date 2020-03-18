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
package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPAuthModelDescriptor;
import org.jkiss.dbeaver.model.impl.auth.DBAAuthDatabaseNative;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.configurator.UIPropertyConfiguratorDescriptor;
import org.jkiss.dbeaver.registry.configurator.UIPropertyConfiguratorRegistry;
import org.jkiss.dbeaver.ui.IObjectPropertyConfigurator;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * ConnectionPageWithAuth
 */

public abstract class ConnectionPageWithAuth extends ConnectionPageAbstract {

    private static final Log log = Log.getLog(DataSourceProviderRegistry.class);

    private List<? extends DBPAuthModelDescriptor> allAuthModels;
    private DBPAuthModelDescriptor selectedAuthModel;
    private Composite modelConfigPlaceholder;
    private IObjectPropertyConfigurator<DBPDataSourceContainer> authModelConfigurator;

    protected void createAuthPanel(Composite parent, int hSpan) {
        modelConfigPlaceholder = UIUtils.createControlGroup(parent, UIConnectionMessages.dialog_connection_auth_group, 2, GridData.FILL_HORIZONTAL, 0);
        ((GridData)modelConfigPlaceholder.getLayoutData()).horizontalSpan = hSpan;
    }

    @Override
    public void loadSettings() {
        super.loadSettings();

        DBPDataSourceContainer activeDataSource = getSite().getActiveDataSource();

        allAuthModels = DataSourceProviderRegistry.getInstance().getApplicableAuthModels(activeDataSource);
        allAuthModels.sort(Comparator.comparing(DBPAuthModelDescriptor::getName));
        String dsModelId = activeDataSource.getConnectionConfiguration().getAuthModelId();
        if (dsModelId != null) {
            Optional<? extends DBPAuthModelDescriptor> dsModel = allAuthModels.stream().filter(o -> o.getId().equals(dsModelId)).findFirst();
            if (dsModel.isPresent()) {
                selectedAuthModel = dsModel.get();
            } else {
                log.error("Model '" + dsModelId + "' not found");
            }
        }
        if (selectedAuthModel == null) {
            // Set default to native
            for (DBPAuthModelDescriptor amd : allAuthModels) {
                if (amd.getId().equals(DBAAuthDatabaseNative.ID)) {
                    selectedAuthModel = amd;
                    break;
                }
            }
            if (selectedAuthModel == null) {
                // First one
                selectedAuthModel = allAuthModels.get(0);
            }
        }

        changeAuthModel();
    }

    private void changeAuthModel() {
        showAuthModelSettings();
    }

    protected void showAuthModelSettings() {
        TabFolder parentFolder = UIUtils.getParentOfType(modelConfigPlaceholder, TabFolder.class);
        if (parentFolder != null) {
            parentFolder.setRedraw(false);
        }

        UIUtils.disposeChildControls(modelConfigPlaceholder);

        Label authModelLabel = UIUtils.createControlLabel(modelConfigPlaceholder, UIConnectionMessages.dialog_connection_auth_group);
        Combo authModelCombo = new Combo(modelConfigPlaceholder, SWT.DROP_DOWN | SWT.READ_ONLY);
        authModelCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        authModelCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                DBPAuthModelDescriptor newAuthModel = allAuthModels.get(authModelCombo.getSelectionIndex());
                if (selectedAuthModel != newAuthModel) {
                    selectedAuthModel = newAuthModel;
                    showAuthModelSettings();
                }
            }
        });
        for (DBPAuthModelDescriptor model : allAuthModels) {
            authModelCombo.add(model.getName());
        }
        if (selectedAuthModel != null) {
            authModelCombo.select(allAuthModels.indexOf(selectedAuthModel));
        }
        boolean authSelectorVisible = allAuthModels.size() >= 2;
        authModelLabel.setVisible(authSelectorVisible);
        ((GridData)authModelLabel.getLayoutData()).exclude = !authSelectorVisible;
        authModelCombo.setVisible(authSelectorVisible);
        ((GridData)authModelCombo.getLayoutData()).exclude = !authSelectorVisible;
        ((Group)modelConfigPlaceholder).setText(authSelectorVisible ? UIConnectionMessages.dialog_connection_auth_group : UIConnectionMessages.dialog_connection_auth_group + " (" + selectedAuthModel.getName() + ")");

        {
            authModelConfigurator = null;
            UIPropertyConfiguratorDescriptor uiConfiguratorDescriptor = UIPropertyConfiguratorRegistry.getInstance().getDescriptor(selectedAuthModel.getImplClassName());
            if (uiConfiguratorDescriptor != null) {
                try {
                    authModelConfigurator = uiConfiguratorDescriptor.createConfigurator();
                } catch (DBException e) {
                    log.error(e);
                }
            }
        }

        if (authModelConfigurator != null) {
            authModelConfigurator.createControl(modelConfigPlaceholder, () -> getSite().updateButtons());
            authModelConfigurator.loadSettings(getSite().getActiveDataSource());
        }

        if (modelConfigPlaceholder.getSize().x > 0 && parentFolder != null) {
            parentFolder.layout(true, true);
        }
        if (parentFolder != null) {
            parentFolder.setRedraw(true);
        }
    }

    @Override
    public void saveSettings(DBPDataSourceContainer dataSource) {
        super.saveSettings(dataSource);
        dataSource.getConnectionConfiguration().setAuthModelId(
            selectedAuthModel == null ? null : selectedAuthModel.getId());
        if (authModelConfigurator != null) {
            authModelConfigurator.saveSettings(dataSource);
        }
    }

    @Override
    public boolean isComplete() {
        return authModelConfigurator == null || authModelConfigurator.isComplete();
    }

}
