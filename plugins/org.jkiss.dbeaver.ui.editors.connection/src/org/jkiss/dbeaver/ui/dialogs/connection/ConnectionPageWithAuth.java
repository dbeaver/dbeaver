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

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPAuthModelDescriptor;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.impl.auth.AuthModelDatabaseNative;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.configurator.DBPConnectionEditIntention;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIServiceConnectionEditor;
import org.jkiss.utils.CommonUtils;

/**
 * ConnectionPageWithAuth
 */

public abstract class ConnectionPageWithAuth extends ConnectionPageAbstract {

    private static final Log log = Log.getLog(DataSourceProviderRegistry.class);

    private AuthModelSelector authModelSelector;
    private UIServiceConnectionEditor serviceConnectionEditor;

    protected void createAuthPanel(Composite parent, int hSpan) {
        createAuthPanel(parent, hSpan, null);
    }

    protected void createAuthPanel(Composite parent, int hSpan, Runnable panelExtender) {
        Assert.isLegal(isAuthEnabled());
        authModelSelector = new AuthModelSelector(parent, () -> {
            // Apply font on auth mode change
            Dialog.applyDialogFont(authModelSelector);
            if (panelExtender != null) {
                panelExtender.run();
            }
        }, () -> getSite().updateButtons(), true, this.getIntention());
        authModelSelector.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        ((GridData)authModelSelector.getLayoutData()).horizontalSpan = hSpan;

        // Additional auth controls
        serviceConnectionEditor = DBWorkbench.getService(UIServiceConnectionEditor.class);
        if (serviceConnectionEditor != null) {
            serviceConnectionEditor.createControl(parent, getSite().getActiveDataSource(), () -> site.updateButtons());
        }
    }

    protected Composite getAuthPanelComposite() {
        Assert.isLegal(isAuthEnabled());
        return authModelSelector.getAuthPanelComposite();
    }

    @Override
    public void loadSettings() {
        super.loadSettings();

        if (!isAuthEnabled()) {
            return;
        }

        DBPDataSourceContainer dataSource = getSite().getActiveDataSource();

        DBPConnectionConfiguration configuration = dataSource.getConnectionConfiguration();

        if (site.isNew() && CommonUtils.isEmpty(configuration.getUserName())) {
            configuration.setUserName(dataSource.getDriver().getDefaultUser());
        }

        DBPAuthModelDescriptor selectedAuthModel = dataSource.getDriver().getDataSourceProvider().detectConnectionAuthModel(
            dataSource.getDriver(), configuration);

        if (selectedAuthModel != null) {
            DBPAuthModelDescriptor amReplace = selectedAuthModel.getReplacedBy(dataSource.getDriver());
            if (amReplace != null) {
                log.debug("Auth model '" + selectedAuthModel.getId() + "' was replaced by '" + amReplace.getId() + "'");
                selectedAuthModel = amReplace;
                configuration.setAuthModelId(selectedAuthModel.getId());
            }
        }

        authModelSelector.loadSettings(dataSource, selectedAuthModel, getDefaultAuthModelId(dataSource));

        if (serviceConnectionEditor != null) {
            serviceConnectionEditor.loadSettings(dataSource);
        }
    }

    @NotNull
    protected String getDefaultAuthModelId(DBPDataSourceContainer dataSource) {
        return AuthModelDatabaseNative.ID;
    }


    @Override
    public void saveSettings(DBPDataSourceContainer dataSource) {
        super.saveSettings(dataSource);

        if (!isAuthEnabled()) {
            return;
        }

        if (authModelSelector != null) {
            DBPAuthModelDescriptor selectedAuthModel = authModelSelector.getSelectedAuthModel();
            dataSource.getConnectionConfiguration().setAuthModelId(
                selectedAuthModel == null ? null : selectedAuthModel.getId());
            authModelSelector.saveSettings(dataSource);
        }

        if (serviceConnectionEditor != null) {
            serviceConnectionEditor.saveSettings(dataSource);
        }
    }

    @Override
    public boolean isComplete() {
        return !isAuthEnabled() || (authModelSelector != null && authModelSelector.isComplete());
    }

    @Override
    public boolean isExternalConfigurationProvided() {
        return serviceConnectionEditor != null && serviceConnectionEditor.isExternalConfigurationProvided();
    }

    protected boolean isAuthEnabled() {
        return true;
    }

    protected DBPConnectionEditIntention getIntention() {
        return DBPConnectionEditIntention.DEFAULT;
    }
}
