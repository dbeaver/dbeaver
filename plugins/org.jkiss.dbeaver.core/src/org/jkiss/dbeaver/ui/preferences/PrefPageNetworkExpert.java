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
package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;

public class PrefPageNetworkExpert extends AbstractPrefPage implements IWorkbenchPreferencePage {
    private Combo prefIpStackCombo;
    private Combo prefIpAddressesCombo;
    private Button debugNetworkConnectionsCheck;

    @Override
    public void init(IWorkbench workbench) {
        // nothing to initialize
    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).create());
        composite.setLayoutData(new GridData());

        UIUtils.createInfoLabel(
            composite,
            CoreMessages.pref_page_network_expert_label,
            SWT.NONE,
            2,
            null,
            DBeaverIcons.getImage(DBIcon.SMALL_WARNING)
        );

        new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL)
            .setLayoutData(GridDataFactory.fillDefaults().grab(true, false).span(2, 1).create());

        prefIpStackCombo = UIUtils.createLabelCombo(
            composite,
            CoreMessages.pref_page_network_expert_preferred_ip_stack_label,
            CoreMessages.pref_page_network_expert_preferred_ip_stack_tip,
            SWT.READ_ONLY | SWT.DROP_DOWN
        );
        for (ModelPreferences.IPType type : ModelPreferences.IPType.values()) {
            prefIpStackCombo.add(type.toString());
        }
        prefIpStackCombo.setLayoutData(new GridData());
        prefIpStackCombo.select(ModelPreferences.IPType.getPreferredStack().ordinal());

        prefIpAddressesCombo = UIUtils.createLabelCombo(
            composite,
            CoreMessages.pref_page_network_expert_preferred_ip_addresses_label,
            CoreMessages.pref_page_network_expert_preferred_ip_addresses_tip,
            SWT.READ_ONLY | SWT.DROP_DOWN
        );
        for (ModelPreferences.IPType type : ModelPreferences.IPType.values()) {
            prefIpAddressesCombo.add(type.toString());
        }
        prefIpAddressesCombo.setLayoutData(new GridData());
        prefIpAddressesCombo.select(ModelPreferences.IPType.getPreferredAddresses().ordinal());

        debugNetworkConnectionsCheck = UIUtils.createCheckbox(
            composite,
            CoreMessages.pref_page_network_expert_debug_net_label,
            CoreMessages.pref_page_network_expert_debug_net_tip,
            ModelPreferences.getPreferences().getBoolean(ModelPreferences.PROP_DEBUG_NETWORK_CONNECTIONS),
            2
        );

        UIUtils.createInfoLabel(composite, CoreMessages.pref_page_ui_general_label_options_take_effect_after_restart, SWT.NONE, 2);

        return composite;
    }

    @Override
    public boolean performOk() {
        DBPPreferenceStore store = ModelPreferences.getPreferences();

        var preferredIpStack = ModelPreferences.IPType.values()[prefIpStackCombo.getSelectionIndex()];
        var preferredAddresses = ModelPreferences.IPType.values()[prefIpAddressesCombo.getSelectionIndex()];
        var debugNetwork = debugNetworkConnectionsCheck.getSelection();

        if (preferredIpStack != ModelPreferences.IPType.getPreferredStack() ||
            preferredAddresses != ModelPreferences.IPType.getPreferredAddresses() ||
            debugNetwork != ModelPreferences.getPreferences().getBoolean(ModelPreferences.PROP_DEBUG_NETWORK_CONNECTIONS)
        ) {
            store.setValue(ModelPreferences.PROP_PREFERRED_IP_STACK, preferredIpStack.name());
            store.setValue(ModelPreferences.PROP_PREFERRED_IP_ADDRESSES, preferredAddresses.name());
            store.setValue(ModelPreferences.PROP_DEBUG_NETWORK_CONNECTIONS, debugNetwork);

            if (UIUtils.confirmAction(
                getShell(),
                NLS.bind(CoreMessages.pref_page_network_expert_restart_prompt_title, GeneralUtils.getProductName()),
                NLS.bind(CoreMessages.pref_page_network_expert_restart_prompt_message, GeneralUtils.getProductName())
            )) {
                restartWorkbenchOnPrefChange();
            }
        }

        return super.performOk();
    }

    @Override
    protected void performDefaults() {
        prefIpStackCombo.select(ModelPreferences.IPType.AUTO.ordinal());
        prefIpAddressesCombo.select(ModelPreferences.IPType.AUTO.ordinal());
        debugNetworkConnectionsCheck.setSelection(false);
        super.performDefaults();
    }
}
