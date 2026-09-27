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
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.cdata.CDataLicenseUIService;
import org.jkiss.dbeaver.ext.cdata.registry.CDataDriverDescriptor;
import org.jkiss.dbeaver.ext.cdata.registry.CDataLicenseType;
import org.jkiss.dbeaver.ext.cdata.ui.internal.CDataUIMessages;
import org.jkiss.dbeaver.ext.generic.views.GenericConnectionPage;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.io.IOException;
import java.nio.file.Path;

public class CDataConnectionPage extends GenericConnectionPage {
    private static final String SUPPORT_URL = "https://portal.cdata.com/?a=support";
    private static final String PREFERENCE_PAGE_ID = "org.jkiss.dbeaver.ext.cdata.preferences";
    private Label statusLabel;
    private Button activateButton;
    private Button runButton;
    private boolean builderRunning;

    @Override
    protected void createUrlControls(@NotNull Composite parent) {
        Composite configuratorGroup = UIUtils.createPlaceholder(parent, 1);
        configuratorGroup.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 4, 1));
        runButton = UIUtils.createDialogButton(
            configuratorGroup,
            CDataUIMessages.auth_native_url_builder_run,
            SelectionListener.widgetSelectedAdapter(event -> runNativeUrlBuilder())
        );
        runButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        runButton.setToolTipText(CDataUIMessages.auth_native_url_builder_description);
        UIUtils.createInfoLabel(configuratorGroup, CDataUIMessages.auth_native_url_builder_description);
    }

    @Override
    public void loadSettings() {
        super.loadSettings();
        refreshStatus();
        setRunButtonEnabled(!builderRunning);
    }

    @Override
    public void createAdvancedSettingsGroup(@NotNull Composite composite) {
        Composite licenseGroup = UIUtils.createTitledComposite(
            composite,
            CDataUIMessages.license_group_title,
            5,
            GridData.FILL_HORIZONTAL
        );
        statusLabel = new Label(licenseGroup, SWT.NONE);
        statusLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Button manageButton = new Button(licenseGroup, SWT.PUSH);
        manageButton.setText(CDataUIMessages.license_manage);
        manageButton.addListener(SWT.Selection, event -> manageLicenses());

        activateButton = new Button(licenseGroup, SWT.PUSH);
        activateButton.setLayoutData(new GridData());
        activateButton.setText(CDataUIMessages.license_activate);
        activateButton.addListener(SWT.Selection, event -> activatePurchasedLicense());

        Button buyButton = new Button(licenseGroup, SWT.PUSH);
        buyButton.setText(CDataUIMessages.activation_buy_button);
        buyButton.addListener(SWT.Selection, event -> UIUtils.openWebBrowser(getDriver().getDriverPurchaseURL()));

        Link supportLink = new Link(licenseGroup, SWT.NONE);
        supportLink.setText(CDataUIMessages.activation_support_link);
        supportLink.addListener(SWT.Selection, event -> UIUtils.openWebBrowser(SUPPORT_URL));
        refreshStatus();
    }

    private void manageLicenses() {
        Shell parent = getShell();
        PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(
            parent, PREFERENCE_PAGE_ID, new String[] {PREFERENCE_PAGE_ID}, null, PreferencesUtil.OPTION_NONE);
        if (dialog == null) {
            return;
        }
        Shell shell = dialog.getShell();
        Rectangle area = parent.getMonitor().getClientArea();
        Rectangle parentBounds = parent.getBounds();
        Rectangle bounds = shell.getBounds();
        bounds.width = Math.min(bounds.width, area.width);
        bounds.height = Math.min(bounds.height, area.height);
        bounds.x = Math.max(area.x, Math.min(parentBounds.x + (parentBounds.width - bounds.width) / 2,
            area.x + area.width - bounds.width));
        bounds.y = Math.max(area.y, Math.min(parentBounds.y + (parentBounds.height - bounds.height) / 2,
            area.y + area.height - bounds.height));
        shell.setBounds(bounds);
        dialog.open();
        refreshStatus();
    }

    private void runNativeUrlBuilder() {
        CDataDriverDescriptor driver = getDriver();
        builderRunning = true;
        setRunButtonEnabled(false);
        new AbstractJob(CDataUIMessages.auth_native_url_builder_job) {
            @NotNull
            @Override
            protected IStatus run(@NotNull DBRProgressMonitor monitor) {
                try {
                    Path driverJar = driver.resolveDriver(monitor).jarPath().toAbsolutePath();
                    if (monitor.isCanceled()) {
                        UIUtils.asyncExec(() -> finishUrlBuilder(null));
                        return Status.CANCEL_STATUS;
                    }
                    new ProcessBuilder(GeneralUtils.findJavaExecutable(), "-jar", driverJar.toString())
                        .directory(driverJar.getParent().toFile())
                        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                        .redirectError(ProcessBuilder.Redirect.DISCARD)
                        .start()
                        .onExit()
                        .thenAccept(process -> UIUtils.asyncExec(() -> {
                            finishUrlBuilder(null);
                            if (!runButton.isDisposed() && process.exitValue() != 0) {
                                DBWorkbench.getPlatformUI().showError(
                                    CDataUIMessages.auth_native_url_builder_error,
                                    NLS.bind(CDataUIMessages.auth_native_url_builder_exit_code, process.exitValue())
                                );
                            }
                        }));
                    return Status.OK_STATUS;
                } catch (DBException | IOException e) {
                    UIUtils.asyncExec(() -> finishUrlBuilder(e));
                    return Status.error(CDataUIMessages.auth_native_url_builder_error, e);
                }
            }
        }.schedule();
    }

    private void finishUrlBuilder(@Nullable Exception error) {
        builderRunning = false;
        setRunButtonEnabled(true);
        if (!runButton.isDisposed()) {
            refreshStatus();
            if (error != null) {
                DBWorkbench.getPlatformUI().showError(
                    CDataUIMessages.auth_native_url_builder_error,
                    CDataUIMessages.auth_native_url_builder_error,
                    error
                );
            }
        }
    }

    private void setRunButtonEnabled(boolean enabled) {
        if (runButton != null && !runButton.isDisposed()) {
            runButton.setEnabled(enabled);
        }
    }

    private void activatePurchasedLicense() {
        CDataLicenseUIService uiService = DBWorkbench.getService(CDataLicenseUIService.class);
        if (uiService != null && uiService.activateLicense(getDriver(), CDataLicenseType.PURCHASED) != null) {
            refreshStatus();
        }
    }

    private void refreshStatus() {
        if (!(site.getDriver() instanceof CDataDriverDescriptor driver)) {
            return;
        }
        statusLabel.setText(NLS.bind(
            CDataUIMessages.license_status,
            CDataLicenseUIUtils.getStatusText(driver.getLicenseStatus())
        ));
        UIUtils.setControlVisible(activateButton, driver.getLicenseStatus().isTrial());
        statusLabel.getParent().layout(true, true);
    }

    @NotNull
    private CDataDriverDescriptor getDriver() {
        return (CDataDriverDescriptor) site.getDriver();
    }
}
