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
package org.jkiss.dbeaver.ui.datadam;

import com.dbeaver.datadam.dam.remote.api.DDRemoteDamSettingKeys;
import com.dbeaver.datadam.dam.remote.api.model.DDConnectedRemoteDam;
import com.dbeaver.datadam.dam.remote.api.model.DDRemoteDam;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.datadam.DDAccountClient;
import org.jkiss.dbeaver.model.datadam.DDEndpoints;
import org.jkiss.dbeaver.model.datadam.DDProductConfig;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DBDamAccessListener;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DBDamAccessService;
import org.jkiss.dbeaver.model.datadam.rpc.DBDamSessionService;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.BaseEditorColors;
import org.jkiss.dbeaver.ui.BaseThemeSettings;
import org.jkiss.dbeaver.ui.UIExecutionQueue;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.css.CSSUtils;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIMessages;
import org.jkiss.dbeaver.ui.datadam.management.DDManagementActions;
import org.jkiss.dbeaver.ui.datadam.project.keys.DDProjectKeyUIManager;
import org.jkiss.dbeaver.ui.forms.UIAlignX;
import org.jkiss.dbeaver.ui.forms.UIGrowX;
import org.jkiss.dbeaver.ui.forms.UIObservable;
import org.jkiss.dbeaver.ui.forms.UIPanelBuilder;
import org.jkiss.dbeaver.ui.preferences.AbstractPrefPage;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;

public class DDSyncPreferencePage extends AbstractPrefPage implements IWorkbenchPreferencePage {

    private static final Log log = Log.getLog(DDSyncPreferencePage.class);

    private static final String SYNC_TITLE = DDTrackingUIMessages.sync_preference_page_title;
    private static final String MCP_SETTINGS_URL = "remote-dams";
    private final DBDamAccessService accessService = DBDamAccessService.getInstance();
    private final DBDamAccessListener accessListener = new DBDamAccessListener() {
        @Override
        public void accessChanging() {
        }

        @Override
        public void accessChanged() {
            UIUtils.asyncExec(() -> {
                if (content != null && !content.isDisposed()) {
                    refresh();
                }
            });
        }
    };
    private final UIObservable<Boolean> mcpLoading = UIObservable.of(false);
    private final UIObservable<Boolean> mcpUnavailable = UIObservable.of(false);
    private final UIObservable<Boolean> mcpGloballyDisabled = UIObservable.of(false);
    private final UIObservable<Boolean> mcpVisible = UIObservable.of(false);
    @Nullable
    private AbstractJob damNameJob;
    @Nullable
    private AbstractJob mcpJob;
    @Nullable
    private DDAccountClient.DamSettingsState mcpSettings;

    private Composite content;
    private Button mcpCheck;
    private Text userText;
    private Button accountActionButton;
    private Text damIdText;
    private Text damNameText;
    private Composite damNameRow;
    private boolean canRenameDam = true;
    private boolean settingDamName;
    private boolean damNameEdited;
    @Nullable
    private DDConnectedRemoteDam dam;

    public DDSyncPreferencePage() {
        noDefaultAndApplyButton();
    }

    @Override
    public void init(@NotNull IWorkbench workbench) {
        //empty
    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        content = UIUtils.createPlaceholder(parent, 1);
        content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        ((GridLayout) content.getLayout()).verticalSpacing = 8;
        Button integrationCheck = UIUtils.createCheckbox(content, DDTrackingUIMessages.integration_enabled,
            DDProductConfig.isIntegrationEnabled());
        integrationCheck.setEnabled(DDProductConfig.isIntegrationAvailable());
        integrationCheck.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
            DDManagementActions.setIntegrationEnabled(integrationCheck.getSelection());
            refresh();
        }));
        Label description = DDUIUtils.create(
            content, DDTrackingUIMessages.sync_preference_page_description,
            DDTrackingUIMessages.sync_preference_page_about_link, DDUIConstants.DATADAM_DOCS_URL
        );
        Composite descriptionRow = description.getParent();
        descriptionRow.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        GridLayout descriptionLayout = (GridLayout) descriptionRow.getLayout();
        descriptionLayout.marginLeft = 0;
        descriptionLayout.marginTop = 8;
        descriptionLayout.marginBottom = 0;
        GridData descriptionData = (GridData) description.getLayoutData();
        descriptionData.widthHint = 660;
        descriptionData.horizontalAlignment = SWT.BEGINNING;
        descriptionData.grabExcessHorizontalSpace = false;
        Control form = UIPanelBuilder.build(content, pb -> pb
            .margins(0, 0)
            .spacing(0, 8)
            .row(rb -> rb.control(this::createAuthenticationGroup, cb -> cb.align(UIAlignX.FILL).grow(UIGrowX.ALWAYS)))
            .row(rb -> rb.visible(mcpVisible)
                .titledPanel(DDTrackingUIMessages.sync_preference_page_ai_integration_group, panel -> panel
                    .margins(0, 0)
                    .row(row -> row.visible(mcpGloballyDisabled).control(this::createMcpInfo))
                    .row(row -> row.control(this::createMcpCheck))
                    .row(row -> row.visible(mcpLoading)
                        .label(DDTrackingUIMessages.sync_preference_page_mcp_loading))
                    .row(row -> row.visible(mcpUnavailable)
                        .label(DDTrackingUIMessages.sync_preference_page_mcp_unavailable))
                ))
        );
        GridData formLayout = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
        formLayout.verticalIndent = 16;
        form.setLayoutData(formLayout);

        accessService.addAccessListener(accessListener);
        refresh();
        return content;
    }

    @NotNull
    private Composite createMcpInfo(@NotNull Composite parent) {
        return DDUIUtils.create(
            parent, DDTrackingUIMessages.sync_preference_page_mcp_globally_disabled,
            DDTrackingUIMessages.sync_preference_page_mcp_settings_link,
            URI.create(DDEndpoints.getAccountBaseUrl()).resolve("/" + MCP_SETTINGS_URL).toString()
        ).getParent();
    }

    @NotNull
    private Button createMcpCheck(@NotNull Composite parent) {
        mcpCheck = UIUtils.createCheckbox(parent,
            DDTrackingUIMessages.sync_preference_page_mcp_enabled,
            DDTrackingUIMessages.sync_preference_page_mcp_hint, false, 1);
        mcpCheck.setEnabled(false);
        mcpCheck.addSelectionListener(SelectionListener.widgetSelectedAdapter(e ->
            updateMcpStatus(mcpCheck.getSelection())));
        return mcpCheck;
    }

    @NotNull
    private Control createAuthenticationGroup(@NotNull Composite parent) {
        Composite authenticationGroup = UIUtils.createTitledComposite(
            parent, DDTrackingUIMessages.sync_preference_page_authentication_group,
            1, GridData.FILL_HORIZONTAL, SWT.DEFAULT
        );
        userText = new Text(authenticationGroup, SWT.READ_ONLY);
        UIUtils.fixReadonlyTextBackground(userText);
        GridData statusLayout = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
        userText.setLayoutData(statusLayout);
        createDamNameRow(authenticationGroup);
        Composite accountActions = UIUtils.createComposite(authenticationGroup, 2);
        accountActions.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        accountActionButton = UIUtils.createPushButton(
            accountActions, DDTrackingUIMessages.sync_preference_page_desktop_sign_in, null,
            SelectionListener.widgetSelectedAdapter(e -> {
                DBDamAccessService.Status status = accessService.getStatus();
                if (status == DBDamAccessService.Status.SIGNED_IN) {
                    logOut();
                } else {
                    logIn();
                }
            })
        );
        BaseThemeSettings.instance.addPropertyListener(
            BaseEditorColors.COLOR_ERROR,
            id -> UIUtils.asyncExec(this::updateAccountActionBackground),
            accountActionButton
        );
        Link accountLink = UIUtils.createLink(accountActions,
            "<a>" + DDTrackingUIMessages.sync_preference_page_open_account + "</a>",
            SelectionListener.widgetSelectedAdapter(e -> UIUtils.openWebBrowser(
                URI.create(DDEndpoints.getAccountBaseUrl()).resolve("/account").toString())));
        accountLink.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        return authenticationGroup.getParent();
    }

    private void createDamNameRow(@NotNull Composite parent) {
        damNameRow = UIUtils.createComposite(parent, 2);
        GridData rowLayout = new GridData(SWT.FILL, SWT.CENTER, true, false);
        rowLayout.horizontalIndent = 4;
        damNameRow.setLayoutData(rowLayout);
        UIUtils.createLabel(damNameRow, DDTrackingUIMessages.sync_preference_page_dam_id_label);
        damIdText = new Text(damNameRow, SWT.READ_ONLY | SWT.BORDER);
        damIdText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        UIUtils.createLabel(damNameRow, DDTrackingUIMessages.sync_preference_page_dam_name_label);
        damNameText = new Text(damNameRow, SWT.BORDER);
        damNameText.setMessage(DDTrackingUIMessages.sync_preference_page_dam_name_not_set);
        damNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        damNameText.addModifyListener(e -> {
            if (!settingDamName) {
                damNameEdited = true;
            }
        });
        damNameText.addListener(SWT.FocusOut, event -> saveDamName());
        damNameText.addListener(SWT.DefaultSelection, event -> saveDamName());
    }

    @Override
    public void dispose() {
        accessService.removeAccessListener(accessListener);
        if (mcpJob != null) {
            mcpJob.cancel();
        }
        if (damNameJob != null) {
            damNameJob.cancel();
        }
        super.dispose();
    }

    private void runOperation(@NotNull String errorMessage, @NotNull AccessOperation operation) {
        try {
            UIUtils.runInProgressDialog(monitor -> {
                try {
                    operation.run(monitor);
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                }
            });
        } catch (InvocationTargetException e) {
            DDUIUtils.showError(SYNC_TITLE, errorMessage, e.getTargetException());
        } finally {
            refresh();
        }
    }

    @FunctionalInterface
    private interface AccessOperation {
        void run(@NotNull DBRProgressMonitor monitor) throws DBException, InterruptedException;
    }

    private void logIn() {
        runOperation(DDTrackingUIMessages.sync_preference_page_login_failed, accessService::signIn);
    }

    private void logOut() {
        if (!UIUtils.confirmAction(getShell(), DDTrackingUIMessages.sync_preference_page_logout_confirm_title,
            DDTrackingUIMessages.sync_preference_page_project_deletion_confirm_message)) {
            return;
        }
        UIExecutionQueue.blockQueue();
        try {
            runOperation(DDTrackingUIMessages.sync_preference_page_logout_failed,
                monitor -> DDProjectKeyUIManager.getInstance().signOut());
        } finally {
            UIExecutionQueue.unblockQueue();
        }
    }

    private void refresh() {
        boolean enabled = DDProductConfig.isIntegrationEnabled();
        DBDamAccessService.Status status = accessService.getStatus();
        boolean signedIn = status == DBDamAccessService.Status.SIGNED_IN;
        var user = signedIn ? accessService.getLoggedUserIdentity() : null;
        userText.setText(!signedIn ? DDTrackingUIMessages.sync_preference_page_account_not_signed_in
            : user.userName() == null ? DDTrackingUIMessages.sync_preference_page_account_signed_in
            : NLS.bind(DDTrackingUIMessages.sync_preference_page_account_signed_in_as, user.userName()));
        accountActionButton.setText(signedIn
            ? DDTrackingUIMessages.sync_preference_page_desktop_sign_out
            : DDTrackingUIMessages.sync_preference_page_desktop_sign_in);
        // Apply after the dialog's initial CSS pass, which otherwise resets the background.
        UIUtils.asyncExec(this::updateAccountActionBackground);
        accountActionButton.setEnabled(enabled);
        mcpVisible.set(enabled && signedIn);
        damNameRow.setVisible(enabled && signedIn);
        ((GridData) damNameRow.getLayoutData()).exclude = !enabled || !signedIn;
        if (enabled && signedIn) {
            loadDamName();
            loadMcpStatus();
        } else {
            if (damNameJob != null) {
                damNameJob.cancel();
            }
            showDam(null);
            if (mcpJob != null) {
                mcpJob.cancel();
            }
            showMcpStatus(null);
        }
        content.layout(true, true);
    }

    private void updateAccountActionBackground() {
        if (!accountActionButton.isDisposed()) {
            if (accessService.getStatus() == DBDamAccessService.Status.SIGNED_IN) {
                accountActionButton.setBackground(BaseThemeSettings.instance.colorError);
            } else {
                accountActionButton.setBackground(null);
                CSSUtils.applyStyles(accountActionButton);
            }
        }
    }

    private void loadDamName() {
        if (damNameJob != null) {
            damNameJob.cancel();
        }
        DDConnectedRemoteDam cached = null;
        try {
            cached = DBDamService.getInstance().getCachedDam();
        } catch (DBException e) {
            log.debug("Cannot read cached DataDam desktop name", e);
        }
        if (cached != null) {
            showDam(cached);
        } else {
            damIdText.setText("");
            setDamNameText(DDTrackingUIMessages.sync_preference_page_dam_name_connecting);
        }
        damNameText.setEnabled(false);
        damNameJob = new AbstractJob("Read DataDam desktop name") {
            @NotNull
            @Override
            protected IStatus run(@NotNull DBRProgressMonitor monitor) {
                if (monitor.isCanceled()) {
                    return Status.CANCEL_STATUS;
                }
                var service = DBDamService.getInstance();
                boolean canRename = true;
                try {
                    canRename = service.canRenameDam();
                } catch (DBException e) {
                    log.debug("Cannot read DataDam rename permissions", e);
                }
                boolean allowed = canRename;
                try {
                    var result = service.getDam(monitor);
                    UIUtils.asyncExec(() -> {
                        if (isDamNameJobActive(this, monitor)) {
                            canRenameDam = allowed;
                            showDam(result);
                        }
                    });
                } catch (InterruptedException e) {
                    return Status.CANCEL_STATUS;
                } catch (DBException e) {
                    UIUtils.asyncExec(() -> {
                        if (isDamNameJobActive(this, monitor)) {
                            log.debug("Cannot read DataDam desktop name", e);
                            canRenameDam = allowed;
                            showDam(null);
                        }
                    });
                }
                return Status.OK_STATUS;
            }
        };
        damNameJob.setSystem(true);
        damNameJob.schedule();
    }

    private boolean isDamNameJobActive(@NotNull AbstractJob job, @NotNull DBRProgressMonitor monitor) {
        return !damNameText.isDisposed() && damNameJob == job && !monitor.isCanceled()
            && accessService.getStatus() == DBDamAccessService.Status.SIGNED_IN;
    }

    private void showDam(@Nullable DDConnectedRemoteDam result) {
        dam = result;
        damIdText.setText(result == null ? "" : result.damId());
        setDamNameText(result == null ? DDTrackingUIMessages.sync_preference_page_dam_name_unavailable
            : result.damName() == null ? "" : result.damName());
        damNameText.setEnabled(result != null && canRenameDam);
    }

    private void setDamNameText(@NotNull String name) {
        settingDamName = true;
        try {
            damNameText.setText(name);
        } finally {
            settingDamName = false;
            damNameEdited = false;
        }
        damNameRow.getParent().layout(true, true);
    }

    private void saveDamName() {
        if (!damNameEdited || !damNameText.isEnabled()) {
            return;
        }
        damNameEdited = false;
        String name;
        try {
            name = DDRemoteDam.normalizeName(damNameText.getText());
        } catch (IllegalArgumentException e) {
            showDam(dam);
            DDUIUtils.showError(SYNC_TITLE, DDTrackingUIMessages.sync_preference_page_dam_name_update_failed, e);
            return;
        }
        if (dam != null && name.equals(dam.damName())) {
            setDamNameText(name);
            return;
        }
        updateDamName(name);
    }

    private void updateDamName(@NotNull String name) {
        if (damNameJob != null) {
            damNameJob.cancel();
        }
        damNameText.setEnabled(false);
        setDamNameText(DDTrackingUIMessages.sync_preference_page_dam_name_connecting);
        damNameJob = new AbstractJob("Update DataDam desktop name") {
            @NotNull
            @Override
            protected IStatus run(@NotNull DBRProgressMonitor monitor) {
                if (monitor.isCanceled()) {
                    return Status.CANCEL_STATUS;
                }
                try {
                    var updated = DBDamService.getInstance().renameDam(name);
                    UIUtils.asyncExec(() -> {
                        if (isDamNameJobActive(this, monitor)) {
                            showDam(updated);
                        }
                    });
                } catch (DBException e) {
                    UIUtils.asyncExec(() -> {
                        if (isDamNameJobActive(this, monitor)) {
                            showDam(dam);
                            DDUIUtils.showError(
                                SYNC_TITLE, DDTrackingUIMessages.sync_preference_page_dam_name_update_failed, e);
                        }
                    });
                }
                return Status.OK_STATUS;
            }
        };
        damNameJob.setSystem(true);
        damNameJob.schedule();
    }

    private void loadMcpStatus() {
        if (mcpJob != null) {
            mcpJob.cancel();
        }
        try {
            var cached = DBDamService.getInstance().getCachedDamSettings();
            if (cached != null) {
                showMcpStatus(cached);
                return;
            }
        } catch (DBException e) {
            log.debug("Cannot read cached DataDam MCP status", e);
        }
        showMcpLoading();
        mcpJob = DBDamService.getInstance().createJob(
            "Read DataDam MCP status",
            account -> DBDamService.getInstance().getDamSettings(),
            status -> {
                if (!mcpCheck.isDisposed() && accessService.getStatus() == DBDamAccessService.Status.SIGNED_IN) {
                    showMcpStatus(status);
                }
            },
            error -> {
                if (!mcpCheck.isDisposed() && accessService.getStatus() == DBDamAccessService.Status.SIGNED_IN) {
                    log.debug("Cannot read DataDam MCP status", error);
                    showMcpStatus(null);
                }
            });
    }

    private void updateMcpStatus(boolean enabled) {
        var previous = mcpSettings;
        if (previous == null) {
            return;
        }
        if (mcpJob != null) {
            mcpJob.cancel();
        }
        showMcpLoading();
        mcpJob = DBDamService.getInstance().createJob(
            "Update DataDam MCP status",
            account -> new DDAccountClient.DamSettingsState(previous.accountId(), account.setDamSetting(
                DBDamSessionService.getInstance().getClientDamId(), DDRemoteDamSettingKeys.MCP_ENABLED,
                Boolean.toString(enabled), previous.accountId()), previous.canManage()),
            updated -> {
                if (!mcpCheck.isDisposed() && accessService.getStatus() == DBDamAccessService.Status.SIGNED_IN) {
                    showMcpStatus(updated);
                }
            },
            error -> {
                if (!mcpCheck.isDisposed() && accessService.getStatus() == DBDamAccessService.Status.SIGNED_IN) {
                    showMcpStatus(null);
                    DDUIUtils.showError(
                        SYNC_TITLE, DDTrackingUIMessages.sync_preference_page_mcp_update_failed, error);
                }
            });
    }

    private void showMcpLoading() {
        mcpCheck.setEnabled(false);
        mcpLoading.set(true);
        mcpUnavailable.set(false);
        mcpGloballyDisabled.set(false);
        content.layout(true, true);
    }

    private void showMcpStatus(@Nullable DDAccountClient.DamSettingsState state) {
        mcpSettings = state;
        boolean accountEnabled = state != null && state.settings().isAccountEnabled(DDRemoteDamSettingKeys.MCP_ENABLED, true);
        mcpCheck.setSelection(accountEnabled && state.settings().isDamEnabled(DDRemoteDamSettingKeys.MCP_ENABLED, false));
        mcpCheck.setEnabled(DDProductConfig.isIntegrationEnabled() && accountEnabled && state.canManage());
        mcpLoading.set(false);
        mcpUnavailable.set(state == null);
        mcpGloballyDisabled.set(state != null && !accountEnabled);
        content.layout(true, true);
    }
}
