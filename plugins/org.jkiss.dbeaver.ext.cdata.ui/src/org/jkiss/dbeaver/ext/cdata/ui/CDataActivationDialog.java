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
package org.jkiss.dbeaver.ext.cdata.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.cdata.registry.*;
import org.jkiss.dbeaver.ext.cdata.ui.internal.CDataUIMessages;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.BaseEditorColors;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

final class CDataActivationDialog extends BaseDialog {
    private final CDataDriverDescriptor driver;
    private final CDataLicenseType selectedType;
    private final CDataResolvedDriver activationTarget;
    private String endUserName;
    private String endUserEmail;
    private Composite form;
    private Text nameText;
    private Text emailText;
    private Text productKeyText;
    private Button eulaConsentButton;
    private Label nameError;
    private Label emailError;
    private Label productKeyError;
    private Label consentError;
    private Composite progressArea;
    private Composite errorArea;
    private Label errorDetails;
    private boolean submitted;
    private boolean activating;
    private AbstractJob activationJob;
    private CDataDriverLicense activatedLicense;

    CDataActivationDialog(
        @NotNull Shell parentShell,
        @NotNull CDataDriverDescriptor driver,
        @Nullable CDataLicenseType fixedType,
        @Nullable CDataResolvedDriver activationTarget,
        @NotNull String initialName,
        @NotNull String initialEmail
    ) {
        super(parentShell, fixedType == CDataLicenseType.TRIAL && driver.supportsTrialLicense()
            ? CDataUIMessages.activation_trial_title : CDataUIMessages.activation_dialog_title, null);
        this.driver = driver;
        this.selectedType = fixedType == CDataLicenseType.TRIAL && driver.supportsTrialLicense()
            ? CDataLicenseType.TRIAL : CDataLicenseType.PURCHASED;
        this.activationTarget = activationTarget;
        this.endUserName = initialName;
        this.endUserEmail = initialEmail;
    }

    @NotNull
    @Override
    protected Composite createDialogArea(@NotNull Composite parent) {
        final Composite area = new Composite(parent, SWT.NONE);
        GridLayout areaLayout = new GridLayout(1, false);
        areaLayout.marginWidth = 0;
        areaLayout.marginHeight = 0;
        areaLayout.verticalSpacing = 0;
        area.setLayout(areaLayout);
        area.setLayoutData(new GridData(GridData.FILL_BOTH));
        dialogArea = area;

        Composite banner = new Composite(area, SWT.NONE);
        GridLayout bannerLayout = new GridLayout(2, false);
        bannerLayout.marginWidth = 20;
        bannerLayout.marginHeight = 15;
        banner.setLayout(bannerLayout);
        banner.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        Composite heading = UIUtils.createPlaceholder(banner, 1, 0);
        heading.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        Label title = UIUtils.createLabel(heading, NLS.bind(isTrial()
            ? CDataUIMessages.activation_trial_heading : CDataUIMessages.activation_purchased_heading, driver.getName()));
        title.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));
        UIUtils.createLabel(heading, isTrial()
            ? CDataUIMessages.activation_trial_description
            : NLS.bind(CDataUIMessages.activation_purchased_description, driver.getName().replaceFirst("^CData\\s+", "")));
        Label logo = new Label(banner, SWT.RIGHT);
        GridData logoData = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
        logoData.widthHint = 200;
        logoData.heightHint = 80;
        logo.setLayoutData(logoData);
        driver.loadLogo(() -> UIUtils.asyncExec(() -> {
            if (!logo.isDisposed()) {
                logo.setImage(DBeaverIcons.getImage(driver.getLogoImage()));
                layoutDialog();
            }
        }));
        if (driver.getLogoImage() != null) {
            logo.setImage(DBeaverIcons.getImage(driver.getLogoImage()));
        }
        new Label(area, SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        form = new Composite(area, SWT.NONE);
        GridLayout formLayout = new GridLayout(2, false);
        formLayout.marginWidth = 20;
        formLayout.marginHeight = 20;
        formLayout.horizontalSpacing = 5;
        formLayout.verticalSpacing = 10;
        form.setLayout(formLayout);
        GridData formData = new GridData(GridData.FILL_HORIZONTAL);
        formData.widthHint = 520;
        form.setLayoutData(formData);
        nameText = createField(form, CDataUIMessages.activation_name, endUserName, SWT.NONE);
        nameError = createErrorLabel(nameText.getParent());
        emailText = createField(form, CDataUIMessages.activation_email, endUserEmail, SWT.NONE);
        emailError = createErrorLabel(emailText.getParent());
        if (!isTrial()) {
            productKeyText = createField(form, CDataUIMessages.activation_product_key, "", SWT.PASSWORD);
            productKeyError = createErrorLabel(productKeyText.getParent());
        }

        Composite consent = UIUtils.createPlaceholder(form, 2, 0);
        consent.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
        eulaConsentButton = new Button(consent, SWT.CHECK);
        eulaConsentButton.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
        eulaConsentButton.getAccessible().addAccessibleListener(new AccessibleAdapter() {
            @Override
            public void getName(@NotNull AccessibleEvent event) {
                event.result = CDataUIMessages.activation_eula_consent_accessible_name;
            }
        });
        Link consentLink = new Link(consent, SWT.WRAP);
        consentLink.setText(CDataUIMessages.activation_eula_consent);
        GridData consentData = new GridData(GridData.FILL_HORIZONTAL);
        consentData.widthHint = 460;
        consentLink.setLayoutData(consentData);
        consentLink.addListener(SWT.Selection, event -> UIUtils.openWebBrowser(event.text));
        consentError = createErrorLabel(form);
        consentError.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));

        Composite feedback = UIUtils.createPlaceholder(area, 1, 0);
        GridLayout feedbackLayout = (GridLayout) feedback.getLayout();
        feedbackLayout.marginWidth = 20;
        feedbackLayout.marginBottom = 20;
        feedback.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        progressArea = UIUtils.createPlaceholder(feedback, 2, 0);
        progressArea.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        ProgressBar progress = new ProgressBar(progressArea, SWT.INDETERMINATE);
        GridData progressData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        progressData.widthHint = 40;
        progress.setLayoutData(progressData);
        UIUtils.createLabel(progressArea, isTrial()
            ? CDataUIMessages.activation_trial_progress : CDataUIMessages.activation_purchased_progress);
        errorArea = UIUtils.createPlaceholder(feedback, 2, 0);
        errorArea.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        Label errorIcon = new Label(errorArea, SWT.NONE);
        errorIcon.setImage(DBeaverIcons.getImage(DBIcon.SMALL_ERROR));
        errorIcon.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        Label errorTitle = UIUtils.createLabel(errorArea, isTrial()
            ? CDataUIMessages.activation_trial_failed : CDataUIMessages.activation_failed);
        errorTitle.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));
        new Label(errorArea, SWT.NONE);
        errorDetails = new Label(errorArea, SWT.WRAP);
        GridData detailsData = new GridData(GridData.FILL_HORIZONTAL);
        detailsData.widthHint = 450;
        errorDetails.setLayoutData(detailsData);
        UIUtils.setControlVisible(progressArea, false);
        UIUtils.setControlVisible(errorArea, false);
        UIUtils.setControlVisible(feedback, false);

        nameText.addModifyListener(event -> inputChanged());
        emailText.addModifyListener(event -> inputChanged());
        if (productKeyText != null) {
            productKeyText.addModifyListener(event -> inputChanged());
        }
        eulaConsentButton.addListener(SWT.Selection, event -> inputChanged());
        return area;
    }

    @Override
    protected void createButtonsForLeftButtonBar(@NotNull Composite parent) {
        if (!isTrial()) {
            ((GridLayout) parent.getLayout()).numColumns++;
            Link support = new Link(parent, SWT.NONE);
            support.setText(CDataUIMessages.activation_support_link);
            support.addListener(SWT.Selection, event -> UIUtils.openWebBrowser(CDataLicenseUIUtils.SUPPORT_URL));
        }
    }

    @Override
    protected void createButtonsForButtonBar(@NotNull Composite parent) {
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
        createButton(parent, IDialogConstants.OK_ID, isTrial()
            ? CDataUIMessages.activation_start_trial : CDataUIMessages.activation_activate, true);
    }

    @Override
    protected void okPressed() {
        if (activating) {
            return;
        }
        submitted = true;
        Control invalidControl = validateInput();
        layoutDialog();
        if (invalidControl != null) {
            invalidControl.setFocus();
            return;
        }
        endUserName = nameText.getText().strip();
        endUserEmail = emailText.getText().strip();
        CDataLicenseActivationRequest request = new CDataLicenseActivationRequest(
            endUserName, endUserEmail, selectedType, isTrial() ? null : productKeyText.getText().strip());
        setActivating(true);
        activationJob = new AbstractJob(
            isTrial() ? CDataUIMessages.activation_trial_progress : CDataUIMessages.activation_purchased_progress
        ) {
            @NotNull
            @Override
            protected IStatus run(@NotNull DBRProgressMonitor monitor) {
                try {
                    CDataDriverLicense license = CDataLicenseActivator.activate(monitor, driver, activationTarget, request);
                    boolean canceled = monitor.isCanceled();
                    UIUtils.asyncExec(() -> finishActivation(canceled ? null : license, null));
                    return canceled ? Status.CANCEL_STATUS : Status.OK_STATUS;
                } catch (Exception e) {
                    boolean canceled = monitor.isCanceled();
                    UIUtils.asyncExec(() -> finishActivation(null, canceled ? null : e));
                    return canceled ? Status.CANCEL_STATUS : Status.OK_STATUS;
                }
            }
        };
        activationJob.schedule();
    }

    @Override
    public boolean close() {
        if (activating && activationJob != null) {
            activationJob.cancel();
        }
        return super.close();
    }

    private void setActivating(boolean running) {
        activating = running;
        form.setEnabled(!running);
        getButton(IDialogConstants.OK_ID).setEnabled(!running);
        UIUtils.setControlVisible(progressArea, running);
        UIUtils.setControlVisible(errorArea, false);
        UIUtils.setControlVisible(progressArea.getParent(), running);
        layoutDialog();
    }

    private void finishActivation(@Nullable CDataDriverLicense license, @Nullable Throwable error) {
        if (getShell() == null || getShell().isDisposed()) {
            return;
        }
        activationJob = null;
        setActivating(false);
        if (license != null) {
            activatedLicense = license;
            super.okPressed();
        } else if (error != null) {
            errorDetails.setText(getErrorMessage(error));
            UIUtils.setControlVisible(errorArea, true);
            UIUtils.setControlVisible(errorArea.getParent(), true);
            getButton(IDialogConstants.OK_ID).setText(CDataUIMessages.activation_try_again);
            layoutDialog();
        }
    }

    private void inputChanged() {
        if (submitted) {
            validateInput();
            layoutDialog();
        }
    }

    @Nullable
    private Control validateInput() {
        boolean missingName = nameText.getText().isBlank();
        boolean missingEmail = emailText.getText().isBlank();
        boolean invalidEmail = !missingEmail && !GeneralUtils.isValidEmail(emailText.getText().strip());
        boolean missingKey = !isTrial() && productKeyText.getText().isBlank();
        showFieldError(nameError, missingName ? CDataUIMessages.activation_name_required : null);
        showFieldError(emailError, missingEmail ? CDataUIMessages.activation_email_required
            : invalidEmail ? CDataUIMessages.activation_invalid_email : null);
        if (productKeyError != null) {
            showFieldError(productKeyError, missingKey ? CDataUIMessages.activation_key_required : null);
        }
        boolean missingConsent = !eulaConsentButton.getSelection();
        String consentMessage = isTrial()
            ? CDataUIMessages.activation_trial_consent_required : CDataUIMessages.activation_consent_required;
        showFieldError(consentError, missingConsent ? consentMessage : null);
        if (missingName) {
            return nameText;
        }
        if (missingEmail || invalidEmail) {
            return emailText;
        }
        if (missingKey) {
            return productKeyText;
        }
        return missingConsent ? eulaConsentButton : null;
    }

    private void layoutDialog() {
        Shell shell = getShell();
        if (shell != null && !shell.isDisposed()) {
            shell.layout(true, true);
            shell.setSize(shell.getSize().x, shell.computeSize(shell.getClientArea().width, SWT.DEFAULT).y);
        }
    }

    private boolean isTrial() {
        return selectedType == CDataLicenseType.TRIAL;
    }

    @NotNull
    private static String getErrorMessage(@NotNull Throwable error) {
        if (error instanceof CDataLicenseActivationException activation) {
            if (activation.getStatus() == CDataLicenseStatus.INVALID_KEY
                || activation.getStatus() == CDataLicenseStatus.WRONG_MAJOR_VERSION) {
                return CDataUIMessages.activation_invalid_product_key;
            }
            String details = activation.getDetails();
            if (details != null) {
                return details;
            }
            return activation.getStatus().isUnknown()
                ? CDataUIMessages.activation_failed : CDataLicenseUIUtils.getStatusText(activation.getStatus());
        }
        return CommonUtils.notEmpty(error.getMessage());
    }

    @Nullable
    CDataDriverLicense getActivatedLicense() {
        return activatedLicense;
    }

    @NotNull
    String getEndUserName() {
        return endUserName;
    }

    @NotNull
    String getEndUserEmail() {
        return endUserEmail;
    }

    @NotNull
    CDataLicenseType getSelectedType() {
        return selectedType;
    }

    @NotNull
    private static Text createField(@NotNull Composite parent, @NotNull String label, @NotNull String value, int style) {
        Label fieldLabel = UIUtils.createLabel(parent, label);
        GridData labelData = new GridData(SWT.LEFT, SWT.TOP, false, false);
        labelData.verticalIndent = 3;
        labelData.widthHint = 80;
        fieldLabel.setLayoutData(labelData);
        Composite field = UIUtils.createPlaceholder(parent, 1, 0);
        field.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        Text text = new Text(field, SWT.BORDER | style);
        text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        text.setText(value);
        return text;
    }

    @NotNull
    private static Label createErrorLabel(@NotNull Composite parent) {
        Label label = new Label(parent, SWT.WRAP);
        label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        UIUtils.setControlVisible(label, false);
        return label;
    }

    private static void showFieldError(@NotNull Label label, @Nullable String message) {
        label.setText(CommonUtils.notEmpty(message));
        UIUtils.setControlVisible(label, message != null);
        if (message != null) {
            label.setForeground(UIUtils.getCurrentTheme().getColorRegistry().get(BaseEditorColors.COLOR_ERROR_FOREGROUND));
        }
    }
}
