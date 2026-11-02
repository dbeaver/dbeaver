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
package org.jkiss.dbeaver.ext.cloudflare.d1.ui.views;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.jkiss.code.DynamicCall;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.cloudflare.d1.CloudflareD1ConnectionSettings;
import org.jkiss.dbeaver.ext.cloudflare.d1.auth.CloudflareD1AuthModel;
import org.jkiss.dbeaver.ext.cloudflare.d1.model.CloudflareD1ConnectionAdapter;
import org.jkiss.dbeaver.ext.cloudflare.d1.ui.config.CloudflareD1AuthConfigurator;
import org.jkiss.dbeaver.ext.cloudflare.d1.ui.internal.CloudflareD1UIMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageAbstract;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/** Configures account credentials and discovers the D1 database selected for the initial connection. */
public class CloudflareD1ConnectionPage extends ConnectionPageAbstract {
    private final CloudflareD1AuthConfigurator authConfigurator = new CloudflareD1AuthConfigurator();
    private CloudflareD1DatabaseSelector databaseSelector;
    private Button loadDatabases;
    private Label databaseStatus;
    private boolean loadingDatabases;

    @DynamicCall
    public CloudflareD1ConnectionPage() {
    }

    @Override
    public void createControl(@NotNull Composite parent) {
        Composite group = new Composite(parent, SWT.NONE);
        group.setLayout(new GridLayout(1, false));
        group.setLayoutData(new GridData(GridData.FILL_BOTH));
        createDatabaseSelector(group);
        Composite authPanel = UIUtils.createTitledComposite(
            group, CloudflareD1UIMessages.connection_authentication, 2, GridData.FILL_HORIZONTAL);
        authConfigurator.createControl(authPanel, new CloudflareD1AuthModel(), () -> site.updateButtons());
        createDriverPanel(group);
        setControl(group);
    }

    private void createDatabaseSelector(@NotNull Composite parent) {
        Composite databaseGroup = UIUtils.createTitledComposite(
            parent, CloudflareD1UIMessages.connection_database, 2, GridData.FILL_HORIZONTAL);
        databaseSelector = new CloudflareD1DatabaseSelector(databaseGroup, () -> site.updateButtons());
        databaseSelector.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        loadDatabases = new Button(databaseGroup, SWT.PUSH);
        loadDatabases.setText(CloudflareD1UIMessages.connection_load);
        loadDatabases.addListener(SWT.Selection, event -> loadDatabases());
        databaseStatus = new Label(databaseGroup, SWT.WRAP);
        GridData layout = new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1);
        layout.widthHint = 380;
        databaseStatus.setLayoutData(layout);
        setDatabaseStatus(CloudflareD1UIMessages.connection_help);
    }

    @Override
    public void loadSettings() {
        var configuration = site.getActiveDataSource().getConnectionConfiguration();
        super.loadSettings();
        authConfigurator.loadSettings(site.getActiveDataSource());
        databaseSelector.setSelectedId(CommonUtils.notEmpty(configuration.getDatabaseName()));
        site.updateButtons();
    }

    @Override
    public boolean isComplete() {
        String id = databaseSelector == null ? "" : databaseSelector.getSelectedId();
        boolean valid = CloudflareD1ConnectionSettings.validate(id, false, "") == CloudflareD1ConnectionSettings.Validation.VALID;
        setErrorMessage(valid ? null : CloudflareD1UIMessages.connection_select_database);
        String endpoint = CommonUtils.notEmpty(site.getActiveDataSource().getConnectionConfiguration().getProperty("apiUrl"));
        setMessage(CloudflareD1ConnectionSettings.isInsecureEndpoint(endpoint)
            ? CloudflareD1UIMessages.connection_insecure : null, IMessageProvider.WARNING);
        return valid && authConfigurator.isComplete();
    }

    @Override
    public void saveSettings(@NotNull DBPDataSourceContainer dataSource) {
        String id = databaseSelector.getSelectedId();
        if (!id.isBlank()) {
            var configuration = dataSource.getConnectionConfiguration();
            String scope = CommonUtils.notEmpty(configuration.getProperty(CloudflareD1ConnectionSettings.DATABASE_IDS));
            CloudflareD1ConnectionSettings.save(configuration, id, !scope.isBlank(), scope);
        }
        super.saveSettings(dataSource);
        authConfigurator.saveSettings(dataSource);
        dataSource.getConnectionConfiguration().setAuthModelId("cloudflare_d1_native");
    }

    private void loadDatabases() {
        if (loadingDatabases || databaseSelector == null || databaseSelector.isDisposed()) {
            return;
        }
        var configuration = new DBPConnectionConfiguration(site.getActiveDataSource().getConnectionConfiguration());
        authConfigurator.copyCredentialsTo(configuration);
        if (!hasCredentials(configuration)) {
            setDatabaseStatus(CloudflareD1UIMessages.connection_credentials_required);
            return;
        }
        loadingDatabases = true;
        setDatabaseStatus(CloudflareD1UIMessages.connection_loading);
        List<CloudflareD1ConnectionAdapter.DatabaseChoice> databases = new ArrayList<>();
        Throwable error = null;
        try {
            site.getRunnableContext().run(true, true, monitor -> {
                monitor.beginTask(CloudflareD1UIMessages.connection_load_task, 1);
                try {
                    databases.addAll(CloudflareD1ConnectionAdapter.discoverDatabases(monitor, configuration));
                    if (monitor.isCanceled()) {
                        throw new InterruptedException("Database discovery was cancelled.");
                    }
                } catch (Exception e) {
                    throw new InvocationTargetException(e);
                } finally {
                    monitor.done();
                }
            });
        } catch (InvocationTargetException e) {
            error = e.getTargetException();
        } catch (InterruptedException e) {
            error = e;
        } finally {
            // The wizard restores its control/button state before run() returns.
            loadingDatabases = false;
            if (!databaseSelector.isDisposed()) {
                databaseSelector.setEnabled(true);
                loadDatabases.setEnabled(true);
            }
        }
        completeLoad(databases, error);
    }

    private void completeLoad(@NotNull List<CloudflareD1ConnectionAdapter.DatabaseChoice> databases, @Nullable Throwable error) {
        if (databaseSelector.isDisposed()) {
            return;
        }
        if (error == null) {
            databaseSelector.setDatabases(databases);
            setDatabaseStatus(databases.isEmpty() ? CloudflareD1UIMessages.connection_empty
                : NLS.bind(CloudflareD1UIMessages.connection_loaded, databases.size()));
        } else {
            setDatabaseStatus(CommonUtils.notEmpty(error.getMessage()));
        }
        site.updateButtons();
    }

    private boolean hasCredentials(@NotNull DBPConnectionConfiguration configuration) {
        return !CommonUtils.isEmpty(configuration.getUserName()) && !CommonUtils.isEmpty(configuration.getUserPassword());
    }

    private void setDatabaseStatus(@NotNull String text) {
        databaseStatus.setText(text);
        databaseStatus.getParent().layout(true, true);
    }
}
