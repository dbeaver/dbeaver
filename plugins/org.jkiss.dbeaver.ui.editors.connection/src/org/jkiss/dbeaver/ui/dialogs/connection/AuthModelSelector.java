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
package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.access.DBAAuthModel;
import org.jkiss.dbeaver.model.connection.DBPAuthModelDescriptor;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.configurator.DBPConnectionEditIntention;
import org.jkiss.dbeaver.registry.configurator.UIPropertyConfiguratorDescriptor;
import org.jkiss.dbeaver.registry.configurator.UIPropertyConfiguratorRegistry;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.ui.AbstractObjectPropertyConfigurator;
import org.jkiss.dbeaver.ui.IElementFilter;
import org.jkiss.dbeaver.ui.IObjectPropertyConfigurator;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;
import org.jkiss.utils.CommonUtils;

import java.util.Comparator;
import java.util.List;

/**
 * ConnectionPageWithAuth
 */

public class AuthModelSelector extends Composite {

    private static final Log log = Log.getLog(DataSourceProviderRegistry.class);

    private IObjectPropertyConfigurator<Object, DBPDataSourceContainer> sharedConfigurator;

    private IElementFilter<DBPAuthModelDescriptor> modelFilter;
    private IElementFilter<DBPAuthModelDescriptor> modelChangeFilter;
    private List<? extends DBPAuthModelDescriptor> allAuthModels;
    private DBPDataSourceContainer activeDataSource;
    private DBPAuthModelDescriptor selectedAuthModel;
    private final Composite modelConfigPlaceholder;
    private IObjectPropertyConfigurator<Object, DBPDataSourceContainer> authModelConfigurator;
    private final Runnable panelExtender;
    private final Runnable changeListener;
    private Combo authModelCombo;
    private boolean authSettingsEnabled = true;
    private boolean isEnableSharedConfigurator = true;
    private final DBPConnectionEditIntention intention;

    public AuthModelSelector(
        Composite parent,
        Runnable panelExtender,
        Runnable changeListener,
        boolean enableShared,
        DBPConnectionEditIntention intention
    ) {
        super(parent, SWT.NONE);
        setLayout(new FillLayout());

        this.panelExtender = panelExtender;
        this.changeListener = changeListener;
        this.isEnableSharedConfigurator = enableShared;
        this.intention = intention;

        modelConfigPlaceholder = UIUtils.createControlGroup(
            this,
            UIConnectionMessages.dialog_connection_auth_group,
            2,
            GridData.FILL_HORIZONTAL,
            0
        );

        UIPropertyConfiguratorDescriptor configDescriptor = UIPropertyConfiguratorRegistry.getInstance()
            .getDescriptor(DBAAuthModel.class.getName());
        if (configDescriptor != null && isEnableSharedConfigurator) {
            try {
                sharedConfigurator = configDescriptor.createConfigurator();
            } catch (Exception e) {
                log.error("Error creating shared configurator", e);
            }
        }
    }

    public DBPAuthModelDescriptor getSelectedAuthModel() {
        return selectedAuthModel;
    }

    public DBPDataSourceContainer getActiveDataSource() {
        return activeDataSource;
    }

    Composite getAuthPanelComposite() {
        return modelConfigPlaceholder;
    }

    public void setModelFiler(IElementFilter<DBPAuthModelDescriptor> filter) {
        modelFilter = filter;
    }

    public void setModelChangeFilter(IElementFilter<DBPAuthModelDescriptor> modelChangeFilter) {
        this.modelChangeFilter = modelChangeFilter;
    }

    public void clearSettings() {
        UIUtils.disposeChildControls(modelConfigPlaceholder);
    }

    public void loadSettings(
        DBPDataSourceContainer dataSourceContainer,
        DBPAuthModelDescriptor activeAuthModel,
        String defaultAuthModelId
    ) {
        this.activeDataSource = dataSourceContainer;
        this.selectedAuthModel = activeAuthModel;
        this.authSettingsEnabled = !dataSourceContainer.isSharedCredentials();
        this.allAuthModels = activeDataSource.getDriver() == DriverDescriptor.NULL_DRIVER ?
            DataSourceProviderRegistry.getInstance().getAllAuthModels() :
            DataSourceProviderRegistry.getInstance().getApplicableAuthModels(activeDataSource.getDriver());
        this.allAuthModels.removeIf(o -> modelFilter != null && !modelFilter.isValidElement(o));
        this.allAuthModels.sort((Comparator<DBPAuthModelDescriptor>) (o1, o2) ->
            o1.isDefaultModel() ? -1 :
                o2.isDefaultModel() ? 1 :
                    o1.getName().compareTo(o2.getName()));
        if ((selectedAuthModel == null || !allAuthModels.contains(selectedAuthModel)) && !CommonUtils.isEmpty(defaultAuthModelId)) {
            // Set default to native
            for (DBPAuthModelDescriptor amd : allAuthModels) {
                if (amd.getId().equals(defaultAuthModelId)) {
                    selectedAuthModel = amd;
                    dataSourceContainer.getConnectionConfiguration().setAuthModelId(selectedAuthModel.getId());
                    break;
                }
            }
            if (selectedAuthModel == null || !allAuthModels.contains(selectedAuthModel)) {
                // First one
                selectedAuthModel = allAuthModels.get(0);
                dataSourceContainer.getConnectionConfiguration().setAuthModelId(selectedAuthModel.getId());
            }
        }
        if (sharedConfigurator != null && isEnableSharedConfigurator) {
            sharedConfigurator.loadSettings(activeDataSource);
        }

        changeAuthModel();
    }

    private void changeAuthModel() {
        showAuthModelSettings();
    }

    protected void showAuthModelSettings() {
        Composite parentFolder = UIUtils.getParentOfType(modelConfigPlaceholder, TabFolder.class);
        if (parentFolder == null) {
            parentFolder = UIUtils.getParentOfType(modelConfigPlaceholder, Shell.class);
        }
        modelConfigPlaceholder.setRedraw(false);

        UIUtils.disposeChildControls(modelConfigPlaceholder);

        Label authModelLabel = UIUtils.createControlLabel(modelConfigPlaceholder, UIConnectionMessages.dialog_connection_auth_group);
        Composite authModelComp = UIUtils.createComposite(modelConfigPlaceholder, 3);
        authModelComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        authModelCombo = new Combo(authModelComp, SWT.DROP_DOWN | SWT.READ_ONLY);
        authModelCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        authModelCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                try {
                    DBPAuthModelDescriptor newAuthModel = allAuthModels.get(authModelCombo.getSelectionIndex());
                    if (selectedAuthModel != newAuthModel) {
                        if (modelChangeFilter != null && !modelChangeFilter.isValidElement(newAuthModel)) {
                            authModelCombo.select(allAuthModels.indexOf(selectedAuthModel));
                            return;
                        }
                        selectedAuthModel = newAuthModel;
                        activeDataSource.getConnectionConfiguration().setAuthModelId(selectedAuthModel.getId());
                        showAuthModelSettings();
                    }
                    modelConfigPlaceholder.setFocus();
                    changeListener.run();
                } finally {
                    authModelCombo.setToolTipText(selectedAuthModel == null
                        ? ""
                        : CommonUtils.notEmpty(selectedAuthModel.getDescription()));
                }
                UIUtils.resizeShell(authModelCombo.getShell());
            }
        });
        UIUtils.createEmptyLabel(authModelComp, 1, 1).setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        if (sharedConfigurator != null) {
            sharedConfigurator.createControl(authModelComp, this, this::refreshCredentials);
        } else {
            UIUtils.createEmptyLabel(authModelComp, 1, 1);
        }

        for (DBPAuthModelDescriptor model : allAuthModels) {
            authModelCombo.add(model.getName());
        }
        if (selectedAuthModel != null) {
            authModelCombo.select(allAuthModels.indexOf(selectedAuthModel));
            authModelCombo.setToolTipText(CommonUtils.notEmpty(selectedAuthModel.getDescription()));
        }
        boolean authSelectorVisible = this.intention.authModelSelectionEnabled && allAuthModels.size() >= 2;
        UIUtils.setControlVisible(authModelLabel, authSelectorVisible);
        UIUtils.setControlVisible(authModelComp, authSelectorVisible);
        ((Group) modelConfigPlaceholder).setText(authSelectorVisible
            ? UIConnectionMessages.dialog_connection_auth_group
            : UIConnectionMessages.dialog_connection_auth_group + " (" + selectedAuthModel.getName() + ")");

        boolean sharedCredsProhibitedByIntention = this.intention == DBPConnectionEditIntention.CREDENTIALS_ONLY && this.activeDataSource.isSharedCredentials();
        DBAAuthModel<?> authModel = selectedAuthModel.getInstance();
        if (authSettingsEnabled && !sharedCredsProhibitedByIntention) {
            authModelConfigurator = null;
            UIPropertyConfiguratorDescriptor uiConfiguratorDescriptor = UIPropertyConfiguratorRegistry.getInstance()
                .getDescriptor(authModel);
            if (uiConfiguratorDescriptor != null) {
                try {
                    authModelConfigurator = uiConfiguratorDescriptor.createConfigurator();
                    if (authModelConfigurator instanceof AbstractObjectPropertyConfigurator<?, ?> abstractConfigurator) {
                        abstractConfigurator.setEditIntention(this.intention);
                    }
                } catch (DBException e) {
                    log.error(e);
                }
            } else {
                log.debug("No UI configurator for auth model " + selectedAuthModel.getId());
            }
        }

        if (sharedCredsProhibitedByIntention) {
            UIUtils.createInfoLabel(modelConfigPlaceholder, "Shared credentials cannot be edited", GridData.FILL_BOTH, 1);
        }

        if (authModelConfigurator != null) {
            authModelConfigurator.createControl(modelConfigPlaceholder, authModel, changeListener);
            if (activeDataSource != null) {
                if (selectedAuthModel != null) {
                    // Set selected auth model to datasource config
                    activeDataSource.getConnectionConfiguration().setAuthModelId(selectedAuthModel.getId());
                }
                authModelConfigurator.loadSettings(activeDataSource);
            }
        } else {
            if (selectedAuthModel != null && !CommonUtils.isEmpty(selectedAuthModel.getDescription())) {
                Label descLabel = new Label(modelConfigPlaceholder, SWT.NONE);
                descLabel.setText(selectedAuthModel.getDescription());
                GridData gd = new GridData(GridData.FILL_HORIZONTAL);
                gd.horizontalSpan = 2;
                descLabel.setLayoutData(gd);
            }
        }

        if (panelExtender != null) {
            panelExtender.run();
        }

        modelConfigPlaceholder.setRedraw(true);
        if (modelConfigPlaceholder.getSize().x > 0 && parentFolder != null) {
            parentFolder.layout(true, true);
        }
    }

    private void refreshCredentials() {
        if (activeDataSource instanceof DataSourceDescriptor dsd) {
            dsd.resetAllSecrets();
        }
        authModelConfigurator.loadSettings(activeDataSource);
    }

    public boolean isComplete() {
        return authModelConfigurator == null || authModelConfigurator.isComplete();
    }

    public void saveSettings(DBPDataSourceContainer dataSource) {
        if (sharedConfigurator != null) {
            sharedConfigurator.saveSettings(dataSource);
        }

        if (authModelConfigurator != null) {
            authModelConfigurator.saveSettings(dataSource);
        }
    }

    public boolean isAuthSettingsEnabled() {
        return authSettingsEnabled;
    }

    public void enableAuthSettings(boolean enable, boolean redraw) {
        if (authSettingsEnabled != enable) {
            authSettingsEnabled = enable;
            if (redraw) {
                authModelConfigurator = null;
                changeAuthModel();
            }
        }
    }

    public void setEnableSharedConfigurator(boolean isEnable) {
        this.isEnableSharedConfigurator = isEnable;
    }
}
