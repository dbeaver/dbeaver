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
package org.jkiss.dbeaver.ui.config.easy;

import org.eclipse.jface.wizard.Wizard;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.config.easy.pages.EasyConfigWizardPage;
import org.jkiss.dbeaver.ui.config.easy.spi.EasyConfigPageDescriptor;
import org.jkiss.dbeaver.ui.config.easy.spi.EasyConfigPageRegistry;

public final class EasyConfigWizard extends Wizard {
    private static final Log log = Log.getLog(EasyConfigWizard.class);

    public EasyConfigWizard() {
        setWindowTitle("Easy Config");
    }

    @Override
    public void addPages() {
        for (EasyConfigPageDescriptor descriptor : EasyConfigPageRegistry.getInstance().getPages()) {
            EasyConfigWizardPage page;
            try {
                page = descriptor.createPage();
            } catch (DBException e) {
                log.error("Error creating easy config page " + descriptor.getId(), e);
                continue;
            }
            if (page.isPageApplicable()) {
                addPage(page);
            }
        }
    }

    @Override
    public boolean performFinish() {
        return false;
    }
}
