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

import com.dbeaver.datadam.dam.remote.api.model.DDConnectedRemoteDam;
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
import org.jkiss.dbeaver.model.datadam.DDEndpoints;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopAccessService;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIMessages;
import org.jkiss.dbeaver.ui.forms.UIAlignX;
import org.jkiss.dbeaver.ui.forms.UIGrowX;
import org.jkiss.dbeaver.ui.forms.UIPanelBuilder;
import org.jkiss.dbeaver.ui.preferences.AbstractPrefPage;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;

public class DDSyncPreferencePage extends AbstractPrefPage implements IWorkbenchPreferencePage {

    private static final Log log = Log.getLog(DDSyncPreferencePage.class);

    private static final String SYNC_TITLE = DDTrackingUIMessages.sync_preference_page_title;
    private final DDDesktopAccessService accessService = DDDesktopAccessService.getInstance();
    private AbstractJob userViewJob;
    @Nullable
    private AbstractJob damNameJob;

    private Composite content;
    private Text userText;
    private Button accountActionButton;
    private Label damNameLabel;
    private Button damNameEditButton;
    private Composite damNameRow;
    private boolean canRenameDam = true;
    private String suggestedDamName = GeneralUtils.getProductName();
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
        );
        GridData formLayout = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
        formLayout.verticalIndent = 16;
        form.setLayoutData(formLayout);

        refresh();
        return content;
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
        damNameRow.setVisible(signedIn);
        ((GridData) damNameRow.getLayoutData()).exclude = !signedIn;
        if (signedIn) {
            loadDamName();
        } else {
            if (damNameJob != null) {
                damNameJob.cancel();
            }
            showDam(null);
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
        setDamNameText(DDTrackingUIMessages.sync_preference_page_dam_name_connecting);
        damNameJob = new AbstractJob("Read DataDam desktop name") {
            @NotNull
            @Override
            protected IStatus run(@NotNull DBRProgressMonitor monitor) {
                if (monitor.isCanceled()) {
                    return Status.CANCEL_STATUS;
                }
                var service = DDDesktopDamService.getInstance();
                String suggestion = service.suggestDamName();
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
                            suggestedDamName = suggestion;
                            showDam(result);
                            if (allowed && result != null && result.damName() == null) {
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
                            canRenameDam = allowed;
                            suggestedDamName = suggestion;
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

    private void showDam(@Nullable DDConnectedRemoteDam result) {
        dam = result;
        setDamNameText(result == null ? DDTrackingUIMessages.sync_preference_page_dam_name_unavailable
            : result.damName() == null ? DDTrackingUIMessages.sync_preference_page_dam_name_not_set : result.damName());
        damNameEditButton.setEnabled(canRenameDam);
    }

    private void setDamNameText(@NotNull String name) {
        damNameLabel.setText(name);
        damNameRow.getParent().layout(true, true);
    }

    private void editDamName() {
        InputDialog dialog = new InputDialog(
            getShell(), DDTrackingUIMessages.sync_preference_page_dam_name_edit,
            DDTrackingUIMessages.sync_preference_page_dam_name_prompt,
            dam == null || dam.damName() == null ? suggestedDamName : dam.damName(),
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
        setDamNameText(DDTrackingUIMessages.sync_preference_page_dam_name_connecting);
        damNameJob = new AbstractJob("Update DataDam desktop name") {
            @NotNull
            @Override
            protected IStatus run(@NotNull DBRProgressMonitor monitor) {
                if (monitor.isCanceled()) {
                    return Status.CANCEL_STATUS;
                }
                try {
                    var updated = DDDesktopDamService.getInstance().renameDam(name);
                    UIUtils.asyncExec(() -> {
                        if (isDamNameJobActive(this, monitor)) {
                            showDam(updated);
                        }
                    });
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
