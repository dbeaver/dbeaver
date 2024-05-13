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
package org.jkiss.dbeaver.core.ui.services;

import org.eclipse.core.commands.Command;
import org.eclipse.ui.commands.ICommandService;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.registry.BasePolicyDataProvider;

public class ApplicationPolicyService {

    private static final ApplicationPolicyService service = new ApplicationPolicyService();

    private static final String INSTALL_NEW_SOFTWARE_COMMAND = "org.eclipse.equinox.p2.ui.sdk.install"; //$NON-NLS-1$
    private static final String SHOW_MARKETPLACE_COMMAND = "org.eclipse.epp.mpc.ui.command.showMarketplaceWizard"; //$NON-NLS-1$
    private static final String UPDATE_SOFTWARE_COMMAND = "org.eclipse.equinox.p2.ui.sdk.update"; //$NON-NLS-1$
    private static final String POLICY_SOFTWARE_INSTALL_DISABLED = "policy.software.install.disabled"; //$NON-NLS-1$
    private static final String POLICY_SOFTWARE_UPDATE_DISABLED = "policy.software.update.disabled"; //$NON-NLS-1$

    private ApplicationPolicyService() {
        // no implementation
    }

    /**
     * The instance of service
     *
     * @return - instance of service
     */
    public static ApplicationPolicyService getInstance() {
        return service;
    }

    /**
     * The method designed to reconcile standard command by specific
     *
     * @param commandService - service
     */
    public void disableStandardProductModification(@NotNull ICommandService commandService) {
        if (BasePolicyDataProvider.getInstance().isPolicyEnabled(POLICY_SOFTWARE_INSTALL_DISABLED)) {
            disableCommand(commandService, INSTALL_NEW_SOFTWARE_COMMAND);
            disableCommand(commandService, SHOW_MARKETPLACE_COMMAND); 
        }
        if (BasePolicyDataProvider.getInstance().isPolicyEnabled(POLICY_SOFTWARE_UPDATE_DISABLED)) {
            disableCommand(commandService, UPDATE_SOFTWARE_COMMAND);
        }
    }

    private void disableCommand(@NotNull ICommandService commandService, @NotNull String commandName) {
        Command command = commandService.getCommand(commandName);
        if (command != null) {
            command.setEnabled(false);
            command.setHandler(null);
        }
    }

    /**
     * Return true, if software install/update policy enabled
     */
    public boolean isInstallUpdateDisabled() {
        return BasePolicyDataProvider.getInstance().isPolicyEnabled(POLICY_SOFTWARE_INSTALL_DISABLED)
            || BasePolicyDataProvider.getInstance().isPolicyEnabled(POLICY_SOFTWARE_UPDATE_DISABLED);
    }
}
