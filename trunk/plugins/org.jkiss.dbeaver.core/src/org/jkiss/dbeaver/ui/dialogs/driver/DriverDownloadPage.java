/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.preferences.PrefPageDrivers;
import org.jkiss.utils.CommonUtils;

abstract class DriverDownloadPage extends WizardPage {

    DriverDownloadPage(String pageName, String title, ImageDescriptor titleImage) {
        super(pageName, title, titleImage);
    }

    public DriverDownloadWizard getWizard() {
        return (DriverDownloadWizard) super.getWizard();
    }

    abstract void resolveLibraries();

    abstract void performFinish();

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
                        RuntimeUtils.openWebBrowser(driver.getWebURL());
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
                        PrefPageDrivers.PAGE_ID);
                }
            });
        link.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_END));
    }

}