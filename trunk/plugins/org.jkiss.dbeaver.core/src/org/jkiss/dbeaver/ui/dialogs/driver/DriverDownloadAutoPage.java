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

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.registry.DriverDescriptor;
import org.jkiss.dbeaver.registry.DriverFileDescriptor;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.preferences.PrefPageDrivers;
import org.jkiss.utils.CommonUtils;

class DriverDownloadAutoPage extends WizardPage {

    static final Log log = Log.getLog(DriverDownloadAutoPage.class);

    DriverDownloadAutoPage() {
        super("Automatic download", "Download driver files", null);
        setPageComplete(false);
    }

    @Override
    public void createControl(Composite parent) {
        DriverDownloadWizard wizard = (DriverDownloadWizard) getWizard();
        final DriverDescriptor driver = wizard.getDriver();

        setMessage("Download " + driver.getFullName() + " driver files");

        StringBuilder message = new StringBuilder();
        message.append("").append(driver.getFullName())
            .append(" driver files are missing.\nDBeaver can download these files automatically.\n\nFiles required by driver:");
        for (DriverFileDescriptor file : wizard.getFiles()) {
            message.append("\n\t-").append(file.getPath());
        }
        message.append("\n\nOr you can obtain driver files by yourself and add them in driver editor.");
        initializeDialogUnits(parent);

        Composite composite = UIUtils.createPlaceholder(parent, 1);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        Text infoText = new Text(composite, SWT.MULTI | SWT.READ_ONLY | SWT.WRAP);
        infoText.setText(message.toString());
        infoText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        //UIUtils.createHorizontalLine(composite);
        UIUtils.createPlaceholder(composite, 1).setLayoutData(new GridData(GridData.FILL_BOTH));

        {
            Composite linksGroup = UIUtils.createPlaceholder(composite, 2);
            ((GridLayout)linksGroup.getLayout()).makeColumnsEqualWidth = true;
            linksGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

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
                            DBeaverUI.getActiveWorkbenchShell(),
                            null,
                            PrefPageDrivers.PAGE_ID);
                    }
                });
            link.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_END));
        }

        setControl(composite);
    }


}