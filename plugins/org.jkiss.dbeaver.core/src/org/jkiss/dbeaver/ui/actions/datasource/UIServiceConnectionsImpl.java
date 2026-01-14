/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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

package org.jkiss.dbeaver.ui.actions.datasource;

import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.model.runtime.DBRProgressListener;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.secret.DBSSecretController;
import org.jkiss.dbeaver.runtime.DBServiceConnections;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.UIServiceConnections;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.EditConnectionDialog;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import java.net.PasswordAuthentication;

/**
 * UIServiceConnectionsImpl
 */
public class UIServiceConnectionsImpl implements DBServiceConnections, UIServiceConnections {

    private final IProxyService proxyService;

    public UIServiceConnectionsImpl() {
        BundleContext bundleContext = DBeaverActivator.getInstance().getBundle().getBundleContext();
        ServiceReference<IProxyService> proxyServiceRef = bundleContext.getServiceReference(IProxyService.class);
        if (proxyServiceRef != null) {
            proxyService = bundleContext.getService(proxyServiceRef);
        } else {
            proxyService = null;
        }
    }

    @Override
    public void openConnectionEditor(@NotNull DBPDataSourceContainer dataSourceContainer, String defaultPageName) {
        if (dataSourceContainer.getProject().hasRealmPermission(RMConstants.PERMISSION_PROJECT_DATASOURCES_EDIT)) {
            if (dataSourceContainer.getProject().isUseSecretStorage() && !dataSourceContainer.isSharedCredentials()) {
                try {
                    DBSSecretController secretController = DBSSecretController.getProjectSecretController(dataSourceContainer.getProject());
                    dataSourceContainer.resolveSecrets(secretController);
                } catch (DBException e) {
                    DBWorkbench.getPlatformUI().showError("Secret resolve", "Error loading connection secrets", e);
                }
            }
            EditConnectionDialog.openEditConnectionDialog(UIUtils.getActiveWorkbenchWindow(), dataSourceContainer, defaultPageName);
        } else {
            // Cannot edit connection. Let's open its contents
            DBWorkbench.getPlatformUI().openEntityEditor(dataSourceContainer);
        }
    }

    @Override
    public void connectDataSource(@NotNull DBPDataSourceContainer dataSourceContainer, DBRProgressListener onFinish) {
        DataSourceHandler.connectToDataSource(null, dataSourceContainer, onFinish);
    }

    @Override
    public void disconnectDataSource(@NotNull DBPDataSourceContainer dataSourceContainer) {
        DataSourceHandler.disconnectDataSource(dataSourceContainer, null);
    }

    @Override
    public void closeActiveTransaction(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext context, boolean commitTxn) {
        DataSourceHandler.closeActiveTransaction(monitor, context, commitTxn);
    }

    @Override
    public boolean confirmTransactionsClose(@NotNull DBCExecutionContext[] contexts) {
        return DataSourceHandler.confirmTransactionsClose(contexts);
    }

    @Override
    public boolean checkAndCloseActiveTransaction(@NotNull DBCExecutionContext[] contexts) {
        return DataSourceHandler.checkAndCloseActiveTransaction(contexts, false);
    }

    @Nullable
    @Override
    public PasswordAuthentication getGlobalProxyConfiguration(@NotNull String requestingProtocol, @Nullable String requestingHost, int requestingPort) {
        if (proxyService != null) {
            // Try to use Eclipse proxy config for global proxies
            IProxyData[] proxyData = proxyService.getProxyData();
            if (proxyData != null) {
                for (IProxyData pd : proxyData) {
                    if (requestingProtocol.startsWith(pd.getType()) && pd.getUserId() != null && pd.getHost() != null && pd.getPort() == requestingPort && pd.getHost().equalsIgnoreCase(requestingHost)) {
                        return new PasswordAuthentication(pd.getUserId(), pd.getPassword().toCharArray());
                    }
                }

            }
        }
        return null;
    }

    @Override
    public void initConnection(DBRProgressMonitor monitor, DBPDataSourceContainer dataSourceContainer, DBRProgressListener onFinish) {
        DataSourceHandler.connectToDataSource(monitor, dataSourceContainer, onFinish);
    }

}
