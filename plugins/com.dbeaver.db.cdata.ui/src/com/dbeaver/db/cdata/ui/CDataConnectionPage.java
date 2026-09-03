/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dbeaver.db.cdata.ui;

import com.dbeaver.db.cdata.CDataLicenseUIService;
import com.dbeaver.db.cdata.registry.CDataDriverDescriptor;
import com.dbeaver.db.cdata.ui.internal.CDataUIMessages;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.jkiss.dbeaver.ext.generic.views.GenericConnectionPage;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;

public class CDataConnectionPage extends GenericConnectionPage {
    private static final String SUPPORT_URL = "https://portal.cdata.com/?a=support";
    private Label statusLabel;

    @Override
    public void createControl(Composite composite) {
        super.createControl(composite);

        Composite licenseGroup = UIUtils.createTitledComposite(
            composite,
            CDataUIMessages.license_group_title,
            4,
            GridData.FILL_HORIZONTAL
        );
        statusLabel = new Label(licenseGroup, SWT.NONE);
        statusLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Button activateButton = new Button(licenseGroup, SWT.PUSH);
        activateButton.setText(CDataUIMessages.license_activate);
        activateButton.addListener(SWT.Selection, event -> activateLicense());

        Link buyLink = new Link(licenseGroup, SWT.NONE);
        buyLink.setText(CDataUIMessages.activation_buy_link);
        buyLink.addListener(SWT.Selection, event -> UIUtils.openWebBrowser(getDriver().getDriverPurchaseURL()));

        Link supportLink = new Link(licenseGroup, SWT.NONE);
        supportLink.setText(CDataUIMessages.activation_support_link);
        supportLink.addListener(SWT.Selection, event -> UIUtils.openWebBrowser(SUPPORT_URL));
        refreshStatus();
    }

    private void activateLicense() {
        CDataLicenseUIService uiService = DBWorkbench.getService(CDataLicenseUIService.class);
        if (uiService != null && uiService.activateLicense(getDriver()) != null) {
            refreshStatus();
        }
    }

    private void refreshStatus() {
        statusLabel.setText(NLS.bind(
            CDataUIMessages.license_status,
            CDataLicenseUIUtils.getStatusText(getDriver().getLicenseStatus())
        ));
        statusLabel.getParent().layout(true, true);
    }

    private CDataDriverDescriptor getDriver() {
        return (CDataDriverDescriptor) site.getDriver();
    }
}
