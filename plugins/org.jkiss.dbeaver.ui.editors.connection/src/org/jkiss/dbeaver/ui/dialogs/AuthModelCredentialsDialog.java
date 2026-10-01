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
package org.jkiss.dbeaver.ui.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.access.DBAAuthModel;
import org.jkiss.dbeaver.registry.configurator.UIPropertyConfiguratorDescriptor;
import org.jkiss.dbeaver.registry.configurator.UIPropertyConfiguratorRegistry;
import org.jkiss.dbeaver.ui.IObjectPropertyConfigurator;
import org.jkiss.dbeaver.ui.dialogs.connection.DatabaseNativeAuthModelConfigurator;

/**
 * Dialog for requesting auth model credentials for non-native auth models.
 * Uses the registered {@link IObjectPropertyConfigurator} to generate the credentials form.
 */
public class AuthModelCredentialsDialog extends BaseDialog implements BlockingPopupDialog {

    private static final Log log = Log.getLog(AuthModelCredentialsDialog.class);

    @NotNull
    private final DBPDataSourceContainer dataSource;
    private IObjectPropertyConfigurator<Object, DBPDataSourceContainer> configurator;

    public AuthModelCredentialsDialog(
        @NotNull Shell parentShell,
        @NotNull DBPDataSourceContainer dataSource
    ) {
        super(parentShell, "'" + dataSource.getName() + "' Connection Credentials", DBIcon.TREE_USER);
        this.dataSource = dataSource;
    }

    @NotNull
    @Override
    protected Composite createDialogArea(@NotNull Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.marginHeight = 10;
        layout.marginWidth = 10;
        composite.setLayout(layout);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        DBAAuthModel<?> authModel = dataSource.getConnectionConfiguration().getAuthModel();

        UIPropertyConfiguratorDescriptor configuratorDescriptor =
            UIPropertyConfiguratorRegistry.getInstance().getDescriptor(authModel);

        if (configuratorDescriptor != null) {
            try {
                configurator = configuratorDescriptor.createConfigurator();
            } catch (DBException e) {
                log.error("Error creating auth model configurator for " + authModel, e);
            }
        }

        if (configurator != null) {
            Object rawConfigurator = configurator;
            if (rawConfigurator instanceof DatabaseNativeAuthModelConfigurator nativeConfigurator) {
                nativeConfigurator.setCredentialsPromptMode(true);
            }
            Composite authPanel = new Composite(composite, SWT.NONE);
            authPanel.setLayout(new GridLayout(2, false));
            authPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            configurator.createControl(authPanel, authModel, () -> {});
            configurator.loadSettings(dataSource);
        } else {
            log.debug("No UI configurator found for auth model: " + authModel);
        }

        return composite;
    }

    @Override
    protected void okPressed() {
        if (configurator != null) {
            configurator.saveSettings(dataSource);
        }
        super.okPressed();
    }

    public static boolean openDialog(
        @NotNull Shell shell,
        @NotNull DBPDataSourceContainer dataSource
    ) {
        AuthModelCredentialsDialog dialog = new AuthModelCredentialsDialog(shell, dataSource);
        return dialog.open() == IDialogConstants.OK_ID;
    }
}
