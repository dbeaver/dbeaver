/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPAuthModelDescriptor;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.impl.auth.AuthModelDatabaseNative;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.utils.CommonUtils;

/**
 * ConnectionPageWithAuth
 */

public abstract class ConnectionPageWithAuth extends ConnectionPageAbstract {

    private static final Log log = Log.getLog(DataSourceProviderRegistry.class);

    private AuthModelSelector authModelSelector;

    protected void createAuthPanel(Composite parent, int hSpan) {
        createAuthPanel(parent, hSpan, null);
    }

    protected void createAuthPanel(Composite parent, int hSpan, Runnable panelExtender) {
        Assert.isLegal(isAuthEnabled());
        authModelSelector = new AuthModelSelector(parent, panelExtender, () -> getSite().updateButtons());
        authModelSelector.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        ((GridData)authModelSelector.getLayoutData()).horizontalSpan = hSpan;
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

        DBPDataSourceContainer activeDataSource = getSite().getActiveDataSource();

        DBPConnectionConfiguration configuration = activeDataSource.getConnectionConfiguration();

        if (site.isNew() && CommonUtils.isEmpty(configuration.getUserName())) {
            configuration.setUserName(activeDataSource.getDriver().getDefaultUser());
        }

        DBPAuthModelDescriptor selectedAuthModel = activeDataSource.getDriver().getDataSourceProvider().detectConnectionAuthModel(
            activeDataSource.getDriver(), configuration);

        if (selectedAuthModel != null) {
            DBPAuthModelDescriptor amReplace = selectedAuthModel.getReplacedBy(activeDataSource.getDriver());
            if (amReplace != null) {
                log.debug("Auth model '" + selectedAuthModel.getId() + "' was replaced by '" + amReplace.getId() + "'");
                selectedAuthModel = amReplace;
                configuration.setAuthModelId(selectedAuthModel.getId());
            }
        }

        authModelSelector.loadSettings(getSite().getActiveDataSource(), selectedAuthModel, getDefaultAuthModelId(activeDataSource));
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

        DBPAuthModelDescriptor selectedAuthModel = authModelSelector.getSelectedAuthModel();
        dataSource.getConnectionConfiguration().setAuthModelId(
            selectedAuthModel == null ? null : selectedAuthModel.getId());
        authModelSelector.saveSettings(dataSource);
    }

    @Override
    public boolean isComplete() {
        return !isAuthEnabled() || authModelSelector.isComplete();
    }

    protected boolean isAuthEnabled() {
        return true;
    }
}
