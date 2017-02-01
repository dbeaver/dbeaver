/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.dialogs.driver;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.runtime.WebUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.preferences.PrefPageDrivers;
import org.jkiss.dbeaver.ui.preferences.PrefPageDriversMaven;
import org.jkiss.utils.CommonUtils;

abstract class DriverDownloadPage extends WizardPage {

    DriverDownloadPage(String pageName, String title, ImageDescriptor titleImage) {
        super(pageName, title, titleImage);
    }

    public DriverDownloadWizard getWizard() {
        return (DriverDownloadWizard) super.getWizard();
    }

    abstract void resolveLibraries();

    abstract boolean performFinish();

    protected void createLinksPanel(Composite composite) {
        final DriverDescriptor driver = getWizard().getDriver();

        //UIUtils.createPlaceholder(composite, 1).setLayoutData(new GridData(GridData.FILL_BOTH));

        Composite linksGroup = UIUtils.createPlaceholder(composite, 2);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.verticalIndent = 10;
        linksGroup.setLayoutData(gd);

        // Vendor site
        if (!CommonUtils.isEmpty(driver.getWebURL())) {
            Link link = UIUtils.createLink(
                linksGroup,
                "<a>Vendor's website</a>",
                new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        WebUtils.openWebBrowser(driver.getWebURL());
                    }
                });
            link.setToolTipText(driver.getWebURL());
            link.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_BEGINNING));
        } else {
            UIUtils.createPlaceholder(linksGroup, 1).setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        }

        Link link = UIUtils.createLink(
            linksGroup,
            "<a>Download configuration</a>",
            new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    UIUtils.showPreferencesFor(
                        null,
                        null,
                        PrefPageDrivers.PAGE_ID,
                        PrefPageDriversMaven.PAGE_ID);
                }
            });
        link.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_END));
    }

}