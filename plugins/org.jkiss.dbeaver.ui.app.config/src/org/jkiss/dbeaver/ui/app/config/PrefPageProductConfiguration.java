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
package org.jkiss.dbeaver.ui.app.config;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.impl.config.ProductConfigUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.app.config.nls.ProductConfigMessages;
import org.jkiss.dbeaver.ui.preferences.AbstractPrefPage;
import org.jkiss.dbeaver.utils.GeneralUtils;

public final class PrefPageProductConfiguration extends AbstractPrefPage implements IWorkbenchPreferencePage {
    private final ProductConfigFeaturesPanel features = new ProductConfigFeaturesPanel(false);
    private boolean restartRequired;

    @Override
    public void init(@NotNull IWorkbench workbench) {
    }

    @Override
    protected boolean hasAccessToPage() {
        return ProductConfigUtils.isAvailable();
    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        return features.createPreferenceControl(
            parent,
            ProductConfigMessages.preferences_features_hint,
            this::openProductConfiguration
        );
    }

    @Override
    protected void performDefaults() {
        features.resetToDefaults();
        super.performDefaults();
    }

    @Override
    public boolean performOk() {
        if (ProductConfigUtils.isAvailable()) {
            restartRequired |= features.applySettings();
        }
        promptForRestart();
        return super.performOk();
    }

    @Override
    public boolean performCancel() {
        if (!super.performCancel()) {
            return false;
        }
        promptForRestart();
        return true;
    }

    private void promptForRestart() {
        if (!restartRequired) {
            return;
        }
        restartRequired = false;
        if (UIUtils.confirmAction(
            getShell(),
            NLS.bind(ProductConfigMessages.preferences_restart_title, GeneralUtils.getProductName()),
            NLS.bind(ProductConfigMessages.preferences_restart_message, GeneralUtils.getProductName())
        )) {
            restartWorkbenchOnPrefChange();
        }
    }

    private void openProductConfiguration() {
        var dialog = new ProductConfigWizardDialog(
            UIUtils.getActiveWorkbenchWindow(),
            ProductConfigWizard.Origin.BY_USER,
            getShell()
        );
        if (dialog.open() == IDialogConstants.OK_ID) {
            features.reloadSettings();
            restartRequired |= dialog.isRestartRequired();
        }
    }
}
