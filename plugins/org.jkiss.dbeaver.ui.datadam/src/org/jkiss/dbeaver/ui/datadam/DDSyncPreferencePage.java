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
import com.dbeaver.datadam.dam.remote.api.model.DDRemoteDam;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
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
import org.jkiss.dbeaver.model.datadam.DDDamInfo;
import org.jkiss.dbeaver.model.datadam.DDEndpoints;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopAccessService;
import org.jkiss.dbeaver.model.datadam.rpc.DDRemoteDamDesktopSessionService;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIMessages;
import org.jkiss.dbeaver.ui.forms.UIAlignX;
import org.jkiss.dbeaver.ui.forms.UIGrowX;
import org.jkiss.dbeaver.ui.forms.UIObservable;
import org.jkiss.dbeaver.ui.forms.UIPanelBuilder;
import org.jkiss.dbeaver.ui.preferences.AbstractPrefPage;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;

public class DDSyncPreferencePage extends AbstractPrefPage implements IWorkbenchPreferencePage {

    private static final Log log = Log.getLog(DDSyncPreferencePage.class);

    private static final String SYNC_TITLE = DDTrackingUIMessages.sync_preference_page_title;
    private static final String MCP_SETTINGS_URL = "remote-dams";
    private final DDDesktopAccessService accessService = DDDesktopAccessService.getInstance();
    private final UIObservable<Boolean> mcpLoading = UIObservable.of(false);
    private final UIObservable<Boolean> mcpUnavailable = UIObservable.of(false);
    private final UIObservable<Boolean> mcpGloballyDisabled = UIObservable.of(false);
    private final UIObservable<Boolean> mcpVisible = UIObservable.of(false);
    private AbstractJob userViewJob;
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
    private Label damNameLabel;
    private Button damNameEditButton;
    private Composite damNameRow;
    @Nullable
    private DDDamInfo dam;

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
                DDDesktopAccessService.Status status = accessService.getStatus();
                if (status == DDDesktopAccessService.Status.SIGNED_IN) {
                    logOut();
                } else {
                    logIn();
                }
            })
        );
        Link accountLink = UIUtils.createLink(accountActions,
            "<a>" + DDTrackingUIMessages.sync_preference_page_open_account + "</a>",
            SelectionListener.widgetSelectedAdapter(e -> UIUtils.openWebBrowser(
                URI.create(DDEndpoints.getAccountBaseUrl()).resolve("/account").toString())));
        accountLink.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        return authenticationGroup.getParent();
    }

    private void createDamNameRow(@NotNull Composite parent) {
        damNameRow = UIUtils.createComposite(parent, 3);
        GridData rowLayout = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
        rowLayout.horizontalIndent = 4;
        damNameRow.setLayoutData(rowLayout);
        UIUtils.createLabel(damNameRow, DDTrackingUIMessages.sync_preference_page_dam_name_label);
        damNameLabel = UIUtils.createLabel(damNameRow, "");
        GridData nameLayout = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
        nameLayout.widthHint = 300;
        damNameLabel.setLayoutData(nameLayout);
        damNameEditButton = new Button(damNameRow, SWT.PUSH);
        damNameEditButton.setImage(DBeaverIcons.getImage(UIIcon.EDIT));
        damNameEditButton.setToolTipText(DDTrackingUIMessages.sync_preference_page_dam_name_edit);
        damNameEditButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> editDamName()));
    }

    @Override
    public void dispose() {
        if (userViewJob != null) {
            userViewJob.cancel();
        }
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
            DBWorkbench.getPlatformUI().showError(SYNC_TITLE, errorMessage, e.getTargetException());
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
        runOperation(DDTrackingUIMessages.sync_preference_page_logout_failed, monitor -> accessService.signOut());
    }

    private void refresh() {
        DDDesktopAccessService.Status status = accessService.getStatus();
        boolean signedIn = status == DDDesktopAccessService.Status.SIGNED_IN;
        userText.setText(signedIn ? DDTrackingUIMessages.sync_preference_page_account_signed_in
            : DDTrackingUIMessages.sync_preference_page_account_not_signed_in);
        accountActionButton.setText(signedIn
            ? DDTrackingUIMessages.sync_preference_page_desktop_sign_out
            : DDTrackingUIMessages.sync_preference_page_desktop_sign_in);
        mcpVisible.set(signedIn);
        damNameRow.setVisible(signedIn);
        ((GridData) damNameRow.getLayoutData()).exclude = !signedIn;
        if (signedIn) {
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
        if (signedIn) {
            loadLoggedUserView();
        } else if (userViewJob != null) {
            userViewJob.cancel();
        }
    }

    private void loadDamName() {
        if (damNameJob != null) {
            damNameJob.cancel();
        }
        damNameEditButton.setEnabled(false);
        damNameLabel.setText(DDTrackingUIMessages.sync_preference_page_dam_name_connecting);
        damNameJob = new AbstractJob("Read DataDam desktop name") {
            @NotNull
            @Override
            protected IStatus run(@NotNull DBRProgressMonitor monitor) {
                if (monitor.isCanceled()) {
                    return Status.CANCEL_STATUS;
                }
                try {
                    var result = DDDesktopDamService.getInstance().getDam(monitor);
                    UIUtils.asyncExec(() -> {
                        if (isDamNameJobActive(this, monitor)) {
                            showDam(result);
                            if (result != null && result.damName() == null) {
                                editDamName();
                            }
                        }
                    });
                } catch (InterruptedException e) {
                    return Status.CANCEL_STATUS;
                } catch (DBException e) {
                    UIUtils.asyncExec(() -> {
                        if (isDamNameJobActive(this, monitor)) {
                            log.debug("Cannot read DataDam desktop name", e);
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
        return !damNameLabel.isDisposed() && damNameJob == job && !monitor.isCanceled()
            && accessService.getStatus() == DDDesktopAccessService.Status.SIGNED_IN;
    }

    private void showDam(@Nullable DDDamInfo result) {
        dam = result;
        damNameLabel.setText(result == null ? DDTrackingUIMessages.sync_preference_page_dam_name_unavailable
            : result.damName() == null ? DDTrackingUIMessages.sync_preference_page_dam_name_not_set : result.damName());
        damNameEditButton.setEnabled(result != null);
    }

    private void editDamName() {
        String computer = System.getenv("COMPUTERNAME");
        if (computer == null || computer.isBlank()) {
            computer = System.getenv("HOSTNAME");
        }
        String productName = GeneralUtils.getProductName();
        String suggestedName = computer == null || computer.isBlank() ? productName : productName + " on " + computer;
        InputDialog dialog = new InputDialog(
            getShell(), DDTrackingUIMessages.sync_preference_page_dam_name_edit,
            DDTrackingUIMessages.sync_preference_page_dam_name_prompt,
            dam == null || dam.damName() == null ? suggestedName : dam.damName(),
            value -> {
                try {
                    DDRemoteDam.normalizeName(value);
                    return null;
                } catch (IllegalArgumentException e) {
                    return e.getMessage();
                }
            });
        if (dialog.open() == Window.OK) {
            updateDamName(DDRemoteDam.normalizeName(dialog.getValue()));
        }
    }

    private void updateDamName(@NotNull String name) {
        if (damNameJob != null) {
            damNameJob.cancel();
        }
        damNameEditButton.setEnabled(false);
        damNameLabel.setText(DDTrackingUIMessages.sync_preference_page_dam_name_connecting);
        damNameJob = new AbstractJob("Update DataDam desktop name") {
            @NotNull
            @Override
            protected IStatus run(@NotNull DBRProgressMonitor monitor) {
                if (monitor.isCanceled()) {
                    return Status.CANCEL_STATUS;
                }
                try {
                    var updated = DDDesktopDamService.getInstance().renameDam(name, monitor);
                    UIUtils.asyncExec(() -> {
                        if (isDamNameJobActive(this, monitor)) {
                            showDam(updated);
                        }
                    });
                } catch (InterruptedException e) {
                    return Status.CANCEL_STATUS;
                } catch (DBException e) {
                    UIUtils.asyncExec(() -> {
                        if (isDamNameJobActive(this, monitor)) {
                            showDam(dam);
                            DBWorkbench.getPlatformUI().showError(
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
        showMcpLoading();
        mcpJob = DDDesktopDamService.getInstance().createJob(
            "Read DataDam MCP status",
            account -> account.getDamSettings(
                DDRemoteDamDesktopSessionService.getInstance().getClientDamId()),
            status -> {
                if (!mcpCheck.isDisposed() && accessService.getStatus() == DDDesktopAccessService.Status.SIGNED_IN) {
                    showMcpStatus(status);
                }
            },
            error -> {
                if (!mcpCheck.isDisposed() && accessService.getStatus() == DDDesktopAccessService.Status.SIGNED_IN) {
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
        mcpJob = DDDesktopDamService.getInstance().createJob(
            "Update DataDam MCP status",
            account -> new DDAccountClient.DamSettingsState(previous.accountId(), account.setDamSetting(
                DDRemoteDamDesktopSessionService.getInstance().getClientDamId(), DDRemoteDamSettingKeys.MCP_ENABLED,
                Boolean.toString(enabled), previous.accountId()), previous.canManage()),
            updated -> {
                if (!mcpCheck.isDisposed() && accessService.getStatus() == DDDesktopAccessService.Status.SIGNED_IN) {
                    showMcpStatus(updated);
                }
            },
            error -> {
                if (!mcpCheck.isDisposed() && accessService.getStatus() == DDDesktopAccessService.Status.SIGNED_IN) {
                    showMcpStatus(null);
                    DBWorkbench.getPlatformUI().showError(
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
        mcpCheck.setEnabled(accountEnabled && state.canManage());
        mcpLoading.set(false);
        mcpUnavailable.set(state == null);
        mcpGloballyDisabled.set(state != null && !accountEnabled);
        content.layout(true, true);
    }

    private void loadLoggedUserView() {
        if (userViewJob != null) {
            userViewJob.cancel();
        }
        userViewJob = new AbstractJob("Read DataDam user information") {
            @NotNull
            @Override
            protected IStatus run(@NotNull DBRProgressMonitor monitor) {
                if (monitor.isCanceled()) {
                    return Status.CANCEL_STATUS;
                }
                var user = accessService.getLoggedUserView();
                UIUtils.asyncExec(() -> {
                    if (!userText.isDisposed() && userViewJob == this && !monitor.isCanceled()
                        && accessService.getStatus() == DDDesktopAccessService.Status.SIGNED_IN) {
                        userText.setText(user.userName() == null
                            ? DDTrackingUIMessages.sync_preference_page_account_signed_in
                            : NLS.bind(
                                DDTrackingUIMessages.sync_preference_page_account_signed_in_as, user.userName()));
                        userText.getParent().layout(true, true);
                    }
                });
                return Status.OK_STATUS;
            }
        };
        userViewJob.setSystem(true);
        userViewJob.schedule();
    }
}
