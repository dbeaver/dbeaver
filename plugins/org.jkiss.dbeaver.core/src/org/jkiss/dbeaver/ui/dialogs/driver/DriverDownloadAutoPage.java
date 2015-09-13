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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.registry.DriverDescriptor;
import org.jkiss.dbeaver.registry.DriverFileDescriptor;
import org.jkiss.dbeaver.tools.transfer.wizard.DataTransferWizard;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
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
        DriverDescriptor driver = wizard.getDriver();

        setMessage("Download " + driver.getFullName() + " driver files");

        StringBuilder message = new StringBuilder();
        message.append("").append(driver.getFullName())
            .append(" driver files are missing.\nDBeaver can download these files automatically.\n\nFiles required by driver:");
        for (DriverFileDescriptor file : driver.getDriverFiles()) {
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
            linksGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            Composite configGroup = UIUtils.createPlaceholder(linksGroup, 1);
            configGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            // Maven/repo config
            UIUtils.createLink(
                configGroup,
                "<a>Repository configuration</a>",
                new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {

                    }
                });
            // Proxy config
            UIUtils.createLink(
                configGroup,
                "<a>Proxy configration</a>",
                new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {

                    }
                });
            Composite webGroup = UIUtils.createPlaceholder(linksGroup, 1);
            webGroup.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END));
            // Vendor site
            if (!CommonUtils.isEmpty(driver.getWebURL())) {
                Link link = UIUtils.createLink(
                    webGroup,
                    "Vendor's website: <a href=\"" + driver.getWebURL() + "\">" + driver.getWebURL() + "</a>",
                    new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            super.widgetSelected(e);
                        }
                    });
                link.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_END));
            }
        }

        setControl(composite);
    }


}