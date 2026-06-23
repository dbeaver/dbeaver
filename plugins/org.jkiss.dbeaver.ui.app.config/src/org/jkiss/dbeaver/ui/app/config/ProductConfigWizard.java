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

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.internal.Workbench;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.app.config.pages.ProductConfigWizardPage;
import org.jkiss.dbeaver.ui.app.config.registry.ProductConfigPageDescriptor;
import org.jkiss.dbeaver.ui.app.config.registry.ProductConfigRegistry;
import org.jkiss.dbeaver.utils.GeneralUtils;

public final class ProductConfigWizard extends Wizard {
    private static final Log log = Log.getLog(ProductConfigWizard.class);

    private boolean restartRequired = false;

    public ProductConfigWizard() {
        setWindowTitle("Product Configuration");
    }

    @Override
    public void addPages() {
        for (ProductConfigPageDescriptor descriptor : ProductConfigRegistry.getInstance().getPages()) {
            ProductConfigWizardPage page;
            try {
                page = descriptor.createPage();
            } catch (DBException e) {
                log.error("Error creating easy config page " + descriptor.getId(), e);
                continue;
            }
            if (page.isPageApplicable()) {
                addPage(page);
                page.loadSettings();
            }
        }
    }

    @Override
    public boolean performFinish() {
        applySettings();

        if (restartRequired) {
            if (UIUtils.confirmAction(
                getShell(),
                "Restart " + GeneralUtils.getProductName(),
                "You need to restart " + GeneralUtils.getProductName() + " to apply some of the changes.\nDo you want to restart now?"
            )) {
                UIUtils.asyncExec(() -> Workbench.getInstance().restart());
            }
        }

        return true;
    }

    @Override
    public boolean performCancel() {
        // Can't cancel - force the user to go through, or apply defaults by pressing "Finish"
        return false;
    }

    /**
     * Marks the wizard for restart, indicating that the changes made
     * in the wizard require a restart of the application to take effect.
     */
    public void markForRestart() {
        restartRequired = true;
    }

    private void applySettings() {
        for (IWizardPage page : getPages()) {
            if (page instanceof ProductConfigWizardPage page1) {
                page1.applySettings();
            }
        }
    }
}
