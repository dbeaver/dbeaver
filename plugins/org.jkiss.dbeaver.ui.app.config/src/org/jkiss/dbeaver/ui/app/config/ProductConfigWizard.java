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
import org.eclipse.osgi.util.NLS;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.app.config.nls.ProductConfigMessages;
import org.jkiss.dbeaver.ui.app.config.pages.ProductConfigWizardPage;
import org.jkiss.dbeaver.ui.app.config.registry.ProductConfigPageDescriptor;
import org.jkiss.dbeaver.ui.app.config.registry.ProductConfigWizardRegistry;
import org.jkiss.dbeaver.utils.GeneralUtils;

public final class ProductConfigWizard extends Wizard {
    public enum Origin {
        AUTOMATIC,
        BY_USER
    }

    private static final Log log = Log.getLog(ProductConfigWizard.class);

    private final Origin origin;
    private boolean restartRequired = false;

    public ProductConfigWizard(@NotNull Origin origin) {
        this.origin = origin;
        setWindowTitle("Product Configuration");
    }

    @Override
    public void addPages() {
        for (ProductConfigPageDescriptor descriptor : ProductConfigWizardRegistry.getInstance().getPages()) {
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
        return true;
    }

    @Override
    public boolean performCancel() {
        if (origin == Origin.BY_USER) {
            return true;
        }
        return UIUtils.confirmAction(
            getShell(),
            ProductConfigMessages.confirm_exit_title,
            NLS.bind(ProductConfigMessages.confirm_exit_message, GeneralUtils.getProductName())
        );
    }

    /**
     * Marks the wizard for restart, indicating that the changes made
     * in the wizard require a restart of the application to take effect.
     */
    public void markForRestart() {
        restartRequired = true;
    }

    public boolean isRestartRequired() {
        return restartRequired;
    }

    @NotNull
    public Origin getOrigin() {
        return origin;
    }

    private void applySettings() {
        for (IWizardPage page : getPages()) {
            if (page instanceof ProductConfigWizardPage page1) {
                page1.applySettings();
            }
        }
    }
}
