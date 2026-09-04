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

import com.dbeaver.db.cdata.registry.*;
import com.dbeaver.db.cdata.ui.internal.CDataUIMessages;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ui.UIUtils;

final class CDataActivationDialog extends TitleAreaDialog {
    private static final String SUPPORT_URL = "https://portal.cdata.com/?a=support";

    private final CDataDriverDescriptor driver;
    private final CDataLicenseType fixedType;
    private Text nameText;
    private Text emailText;
    private Button trialButton;
    private Button purchasedButton;
    private Label productKeyLabel;
    private Text productKeyText;
    private Button eulaConsentButton;
    private CDataDriverLicense activatedLicense;

    CDataActivationDialog(
        @NotNull Shell parentShell,
        @NotNull CDataDriverDescriptor driver,
        @Nullable CDataLicenseType fixedType
    ) {
        super(parentShell);
        this.driver = driver;
        this.fixedType = fixedType;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        setTitle(CDataUIMessages.activation_dialog_title);
        setMessage(NLS.bind(CDataUIMessages.activation_dialog_message, driver.getName()));

        Composite area = (Composite) super.createDialogArea(parent);
        Composite container = new Composite(area, SWT.NONE);
        container.setLayout(new GridLayout(2, false));
        container.setLayoutData(new GridData(GridData.FILL_BOTH));

        createLabel(container, CDataUIMessages.activation_driver);
        Label driverLabel = new Label(container, SWT.NONE);
        driverLabel.setText(driver.getName());

        createLabel(container, CDataUIMessages.activation_name);
        nameText = createText(container, SWT.BORDER);

        createLabel(container, CDataUIMessages.activation_email);
        emailText = createText(container, SWT.BORDER);

        createLabel(container, CDataUIMessages.activation_type);
        Composite typeComposite = new Composite(container, SWT.NONE);
        typeComposite.setLayout(new GridLayout(2, false));
        trialButton = new Button(typeComposite, SWT.RADIO);
        trialButton.setText(CDataUIMessages.activation_trial);
        purchasedButton = new Button(typeComposite, SWT.RADIO);
        purchasedButton.setText(CDataUIMessages.activation_purchased);
        boolean existingPurchasedLicense = driver.getLicenseStatus() == CDataLicenseStatus.PURCHASED_ACTIVE ||
            driver.getLicenseStatus() == CDataLicenseStatus.PURCHASED_EXPIRING ||
            driver.getLicenseStatus() == CDataLicenseStatus.EXPIRED;
        boolean purchasedLicense = fixedType == CDataLicenseType.PURCHASED ||
            fixedType == null && existingPurchasedLicense;
        trialButton.setSelection(!purchasedLicense);
        trialButton.setEnabled(fixedType == null && !existingPurchasedLicense);
        purchasedButton.setSelection(purchasedLicense);
        purchasedButton.setEnabled(fixedType == null);

        productKeyLabel = createLabel(container, CDataUIMessages.activation_product_key);
        productKeyText = createText(container, SWT.BORDER | SWT.PASSWORD);
        setProductKeyVisible(purchasedLicense);

        Composite consentComposite = new Composite(container, SWT.NONE);
        GridLayout consentLayout = new GridLayout(2, false);
        consentLayout.marginWidth = 0;
        consentLayout.marginHeight = 0;
        consentComposite.setLayout(consentLayout);
        GridData consentData = new GridData(GridData.FILL_HORIZONTAL);
        consentData.horizontalSpan = 2;
        consentComposite.setLayoutData(consentData);

        eulaConsentButton = new Button(consentComposite, SWT.CHECK);
        eulaConsentButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));
        Link consentLink = new Link(consentComposite, SWT.WRAP);
        consentLink.setText(CDataUIMessages.activation_eula_consent);
        GridData consentLinkData = new GridData(GridData.FILL_HORIZONTAL);
        consentLinkData.widthHint = 590;
        consentLink.setLayoutData(consentLinkData);
        consentLink.addListener(SWT.Selection, event -> UIUtils.openWebBrowser(event.text));

        Composite links = new Composite(container, SWT.NONE);
        links.setLayout(new GridLayout(2, false));
        GridData linksData = new GridData(GridData.FILL_HORIZONTAL);
        linksData.horizontalSpan = 2;
        links.setLayoutData(linksData);
        Link buyLink = new Link(links, SWT.NONE);
        buyLink.setText(CDataUIMessages.activation_buy_link);
        buyLink.addListener(SWT.Selection, event -> UIUtils.openWebBrowser(driver.getDriverPurchaseURL()));
        Link supportLink = new Link(links, SWT.NONE);
        supportLink.setText(CDataUIMessages.activation_support_link);
        supportLink.addListener(SWT.Selection, event -> UIUtils.openWebBrowser(SUPPORT_URL));

        nameText.addModifyListener(event -> updateState());
        emailText.addModifyListener(event -> updateState());
        productKeyText.addModifyListener(event -> updateState());
        eulaConsentButton.addListener(SWT.Selection, event -> updateState());
        trialButton.addListener(SWT.Selection, event -> updateState());
        purchasedButton.addListener(SWT.Selection, event -> updateState());
        return area;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
        updateState();
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(CDataUIMessages.activation_dialog_title);
    }

    @Override
    protected void okPressed() {
        String validationError = validateInput();
        if (validationError != null) {
            setErrorMessage(validationError);
            return;
        }
        CDataLicenseType type = trialButton.getSelection() ? CDataLicenseType.TRIAL : CDataLicenseType.PURCHASED;
        CDataLicenseActivationRequest request = new CDataLicenseActivationRequest(
            nameText.getText().strip(),
            emailText.getText().strip(),
            type,
            type == CDataLicenseType.PURCHASED ? productKeyText.getText().strip() : null
        );
        try {
            activatedLicense = UIUtils.runWithDialog(
                monitor -> CDataLicenseActivator.activate(monitor, driver, request)
            );
            if (activatedLicense != null) {
                super.okPressed();
            }
        } catch (DBException e) {
            setErrorMessage(e.getMessage());
        }
    }

    @Nullable
    CDataDriverLicense getActivatedLicense() {
        return activatedLicense;
    }

    private void updateState() {
        if (productKeyText == null || productKeyText.isDisposed()) {
            return;
        }
        setProductKeyVisible(purchasedButton.getSelection());
        Button okButton = getButton(IDialogConstants.OK_ID);
        if (okButton != null) {
            okButton.setEnabled(validateInput() == null);
        }
        setErrorMessage(null);
    }

    private void setProductKeyVisible(boolean visible) {
        if (productKeyText.getVisible() == visible) {
            return;
        }
        productKeyLabel.setVisible(visible);
        ((GridData) productKeyLabel.getLayoutData()).exclude = !visible;
        productKeyText.setVisible(visible);
        ((GridData) productKeyText.getLayoutData()).exclude = !visible;
        productKeyText.getParent().layout(true, true);
    }

    @Nullable
    private String validateInput() {
        if (nameText.getText().isBlank() || emailText.getText().isBlank() ||
            !eulaConsentButton.getSelection() ||
            (purchasedButton.getSelection() && productKeyText.getText().isBlank())) {
            return CDataUIMessages.activation_required_fields;
        }
        if (!emailText.getText().contains("@")) {
            return CDataUIMessages.activation_invalid_email;
        }
        return null;
    }

    @NotNull
    private static Label createLabel(@NotNull Composite parent, @NotNull String text) {
        Label label = new Label(parent, SWT.NONE);
        label.setText(text);
        label.setLayoutData(new GridData());
        return label;
    }

    @NotNull
    private static Text createText(@NotNull Composite parent, int style) {
        Text text = new Text(parent, style);
        text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        return text;
    }
}
