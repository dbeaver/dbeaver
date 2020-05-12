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
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPAuthModelDescriptor;
import org.jkiss.dbeaver.model.impl.auth.AuthModelDatabaseNative;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.configurator.UIPropertyConfiguratorDescriptor;
import org.jkiss.dbeaver.registry.configurator.UIPropertyConfiguratorRegistry;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
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

    private IElementFilter<DBPAuthModelDescriptor> modelFilter;
    private IElementFilter<DBPAuthModelDescriptor> modelChangeFilter;
    private List<? extends DBPAuthModelDescriptor> allAuthModels;
    private DBPDataSourceContainer activeDataSource;
    private DBPAuthModelDescriptor selectedAuthModel;
    private Composite modelConfigPlaceholder;
    private IObjectPropertyConfigurator<DBPDataSourceContainer> authModelConfigurator;
    private Runnable changeListener;

    public AuthModelSelector(Composite parent, Runnable changeListener) {
        super(parent, SWT.NONE);
        setLayout(new FillLayout());

        this.changeListener = changeListener;

        modelConfigPlaceholder = UIUtils.createControlGroup(this, UIConnectionMessages.dialog_connection_auth_group, 2, GridData.FILL_HORIZONTAL, 0);
    }

    public DBPAuthModelDescriptor getSelectedAuthModel() {
        return selectedAuthModel;
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

    public void loadSettings(DBPDataSourceContainer dataSourceContainer, DBPAuthModelDescriptor activeAuthModel, String defaultAuthModelId) {
        this.activeDataSource = dataSourceContainer;
        this.selectedAuthModel = activeAuthModel;
        this.allAuthModels = activeDataSource.getDriver() == DriverDescriptor.NULL_DRIVER ?
            DataSourceProviderRegistry.getInstance().getAllAuthModels() :
            DataSourceProviderRegistry.getInstance().getApplicableAuthModels(activeDataSource.getDriver());
        this.allAuthModels.removeIf(o -> modelFilter != null && !modelFilter.isValidElement(o));
        this.allAuthModels.sort((Comparator<DBPAuthModelDescriptor>) (o1, o2) ->
            AuthModelDatabaseNative.ID.equals(o1.getId()) ? -1 :
                (AuthModelDatabaseNative.ID.equals(o2.getId()) ? 1 :
                    o1.getName().compareTo(o2.getName())));
        if (selectedAuthModel == null && !CommonUtils.isEmpty(defaultAuthModelId)) {
            // Set default to native
            for (DBPAuthModelDescriptor amd : allAuthModels) {
                if (amd.getId().equals(defaultAuthModelId)) {
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
        Composite parentFolder = UIUtils.getParentOfType(modelConfigPlaceholder, TabFolder.class);
        if (parentFolder == null) {
            parentFolder = UIUtils.getParentOfType(modelConfigPlaceholder, Shell.class);
        }
        modelConfigPlaceholder.setRedraw(false);

        UIUtils.disposeChildControls(modelConfigPlaceholder);

        Label authModelLabel = UIUtils.createControlLabel(modelConfigPlaceholder, UIConnectionMessages.dialog_connection_auth_group);
        Composite authModelComp = UIUtils.createComposite(modelConfigPlaceholder, 2);
        Combo authModelCombo = new Combo(authModelComp, SWT.DROP_DOWN | SWT.READ_ONLY);
        authModelCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        authModelCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                DBPAuthModelDescriptor newAuthModel = allAuthModels.get(authModelCombo.getSelectionIndex());
                if (selectedAuthModel != newAuthModel) {
                    if (modelChangeFilter != null && !modelChangeFilter.isValidElement(newAuthModel)) {
                        authModelCombo.select(allAuthModels.indexOf(selectedAuthModel));
                        return;
                    }
                    selectedAuthModel = newAuthModel;
                    showAuthModelSettings();
                }
                modelConfigPlaceholder.setFocus();
                changeListener.run();
            }
        });
        Label authModelDescLabel = new Label(authModelComp, SWT.NONE);
        authModelDescLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        if (selectedAuthModel != null) {
            authModelDescLabel.setText(CommonUtils.notEmpty(selectedAuthModel.getDescription()));
        }
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
            authModelConfigurator.createControl(modelConfigPlaceholder, () -> changeListener.run());
            authModelConfigurator.loadSettings(activeDataSource);
        } else {
            if (selectedAuthModel != null && !CommonUtils.isEmpty(selectedAuthModel.getDescription())) {
                Label descLabel = new Label(modelConfigPlaceholder, SWT.NONE);
                descLabel.setText(selectedAuthModel.getDescription());
                GridData gd = new GridData(GridData.FILL_HORIZONTAL);
                gd.horizontalSpan = 2;
                descLabel.setLayoutData(gd);
            }
        }

        modelConfigPlaceholder.setRedraw(true);
        if (modelConfigPlaceholder.getSize().x > 0 && parentFolder != null) {
            parentFolder.layout(true, true);
        }
    }

    public boolean isComplete() {
        return authModelConfigurator == null || authModelConfigurator.isComplete();
    }

    public void saveSettings(DBPDataSourceContainer dataSource) {
        if (authModelConfigurator != null) {
            authModelConfigurator.saveSettings(dataSource);
        }
    }

}
