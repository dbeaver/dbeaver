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
package org.jkiss.dbeaver.ui.net.ssh;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Widget;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.ssh.SSHUtils;
import org.jkiss.dbeaver.model.net.ssh.config.SSHHostConfiguration;
import org.jkiss.dbeaver.registry.configurator.DBPConnectionEditIntention;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.IObjectPropertyConfigurator;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class SSHTunnelCredsOnlyConfiguratorUI implements IObjectPropertyConfigurator<Object, DBWHandlerConfiguration> {
    private List<SSHTunnelDefaultConfiguratorUI.ConfigurationWrapper> configurations;
    private List<SSHTunnelDefaultConfiguratorUI.CredentialsPanel> credPanels;
    private Composite credPanelsContainer;
    private Runnable propertyChangeListener;

    @Override
    public void createControl(@NotNull Composite parent, Object object, @NotNull Runnable propertyChangeListener) {
        this.credPanelsContainer = UIUtils.createControlGroup(parent, "SSH tunnel credentials", 1, GridData.FILL_HORIZONTAL, SWT.DEFAULT);
        this.propertyChangeListener = propertyChangeListener;
    }

    @Override
    public void loadSettings(@NotNull DBWHandlerConfiguration handlerConfiguration) {
        try {
            this.configurations = Arrays.stream(SSHUtils.loadHostConfigurations(handlerConfiguration, false))
                .map(SSHTunnelDefaultConfiguratorUI.ConfigurationWrapper::new).toList();
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError("SSH configuration", "Unable to load SSH configuration due to an error", e);
            return;
        }
        Arrays.stream(this.credPanelsContainer.getChildren()).forEach(Widget::dispose);

        this.credPanels = new ArrayList<>();
        for (SSHTunnelDefaultConfiguratorUI.ConfigurationWrapper cfg : this.configurations) {
            SSHTunnelDefaultConfiguratorUI.CredentialsPanel credsPanel = new SSHTunnelDefaultConfiguratorUI.CredentialsPanel(
                this.credPanelsContainer,
                this.propertyChangeListener,
                DBPConnectionEditIntention.CREDENTIALS_ONLY
            );
            this.credPanels.add(credsPanel);
            credsPanel.loadSettings(cfg, false);
        }
        this.credPanelsContainer.pack(true);
    }

    @Override
    public void saveSettings(@NotNull DBWHandlerConfiguration handlerConfiguration) {
        SSHUtils.saveHostConfigurations(
            handlerConfiguration,
            this.credPanels.stream().map(SSHTunnelDefaultConfiguratorUI.CredentialsPanel::saveSettings).toArray(SSHHostConfiguration[]::new)
        );
    }

    @Override
    public void resetSettings(@NotNull DBWHandlerConfiguration handlerConfiguration) {

    }

    @Override
    public boolean isComplete() {
        return this.credPanels.stream().map(SSHTunnelDefaultConfiguratorUI.CredentialsPanel::saveSettings)
            .map(SSHTunnelDefaultConfiguratorUI::validateConfiguration)
            .allMatch(Objects::isNull);
    }
}