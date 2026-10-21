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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.datadam.auth.DDKeyBundle;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopAccessService;
import org.jkiss.dbeaver.model.datadam.sync.tracking.DDDesktopTrackingService;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIMessages;
import org.jkiss.dbeaver.ui.preferences.AbstractPrefPage;

import java.lang.reflect.InvocationTargetException;

public class DDSyncPreferencePage extends AbstractPrefPage implements IWorkbenchPreferencePage {

    private static final Log log = Log.getLog(DDSyncPreferencePage.class);

    private static final String SYNC_TITLE = DDTrackingUIMessages.sync_preference_page_title;
    private final DDDesktopAccessService accessService = DDDesktopAccessService.getInstance();
    private final DDDesktopTrackingService tracker = DDDesktopTrackingService.getInstance();
    private AbstractJob userViewJob;

    private Text accountText;
    private Composite accountRow;
    private Text userText;
    private Composite userRow;
    private Button loginButton;
    private Button reloginButton;
    private Button logoutButton;
    private Button importKeysButton;
    private Button forgetKeysButton;
    private Composite keysGroup;

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
        Composite composite = UIUtils.createPlaceholder(parent, 1);
        Composite group = UIUtils.createTitledComposite(
            composite,
            DDTrackingUIMessages.sync_preference_page_account_group,
            2,
            GridData.FILL_HORIZONTAL,
            SWT.DEFAULT);
        userRow = UIUtils.createComposite(group, 2);
        GridData userRowData = new GridData(GridData.FILL_HORIZONTAL);
        userRowData.horizontalSpan = 2;
        userRow.setLayoutData(userRowData);
        userText = UIUtils.createLabelText(
            userRow, DDTrackingUIMessages.sync_preference_page_user_label, "", SWT.READ_ONLY, idFieldLayout());

        Composite buttons = UIUtils.createComposite(group, 3);
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.horizontalSpan = 2;
        buttons.setLayoutData(gd);
        loginButton = UIUtils.createPushButton(
            buttons, DDTrackingUIMessages.sync_preference_page_desktop_sign_in, null,
            SelectionListener.widgetSelectedAdapter(e -> logIn()));
        reloginButton = UIUtils.createPushButton(
            buttons, DDTrackingUIMessages.sync_preference_page_desktop_sign_in_again, null,
            SelectionListener.widgetSelectedAdapter(e -> logIn()));
        logoutButton = UIUtils.createPushButton(
            buttons, DDTrackingUIMessages.sync_preference_page_desktop_sign_out, null,
            SelectionListener.widgetSelectedAdapter(e -> logOut()));

        keysGroup = UIUtils.createTitledComposite(
            composite, DDTrackingUIMessages.sync_preference_page_encryption_keys_group,
            2, GridData.FILL_HORIZONTAL, SWT.DEFAULT);
        accountRow = UIUtils.createComposite(keysGroup, 2);
        GridData accountRowData = new GridData(GridData.FILL_HORIZONTAL);
        accountRowData.horizontalSpan = 2;
        accountRow.setLayoutData(accountRowData);
        accountText = UIUtils.createLabelText(
            accountRow, DDTrackingUIMessages.sync_preference_page_account_label, "", SWT.READ_ONLY, idFieldLayout());
        Composite keyButtons = UIUtils.createComposite(keysGroup, 2);
        GridData keyButtonsGd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        keyButtonsGd.horizontalSpan = 2;
        keyButtons.setLayoutData(keyButtonsGd);
        importKeysButton = UIUtils.createPushButton(
            keyButtons, DDTrackingUIMessages.sync_preference_page_import_keys_button, null,
            SelectionListener.widgetSelectedAdapter(e -> importKeys()));
        forgetKeysButton = UIUtils.createPushButton(
            keyButtons, DDTrackingUIMessages.sync_preference_page_forget_keys_button, null,
            SelectionListener.widgetSelectedAdapter(e -> {
                if (UIUtils.confirmAction(
                    getShell(),
                    DDTrackingUIMessages.sync_preference_page_forget_keys_confirm_title,
                    DDTrackingUIMessages.sync_preference_page_forget_keys_confirm_message)
                ) {
                    forgetKeys();
                }
            }));

        refresh();
        return composite;
    }

    @Override
    public void dispose() {
        if (userViewJob != null) {
            userViewJob.cancel();
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

    @NotNull
    private static GridData idFieldLayout() {
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.widthHint = UIUtils.getFontHeight(Display.getCurrent().getSystemFont()) * 22;
        return gd;
    }

    private void importKeys() {
        DDImportKeyDialog dialog = new DDImportKeyDialog(getShell());
        if (dialog.open() != Window.OK) {
            return;
        }
        String phrase = dialog.getPhrase();
        runOperation(DDTrackingUIMessages.sync_preference_page_import_keys_failed,
            monitor -> accessService.importKeys(phrase));
    }

    private void forgetKeys() {
        runOperation(DDTrackingUIMessages.sync_preference_page_cannot_forget_keys,
            monitor -> accessService.forgetKeys());
    }

    private void logIn() {
        runOperation(DDTrackingUIMessages.sync_preference_page_login_failed, monitor -> {
            stopTracking();
            try {
                accessService.signIn(monitor);
            } finally {
                if (accessService.getStatus() == DDDesktopAccessService.Status.SIGNED_IN) {
                    try {
                        tracker.start();
                    } catch (DBException e) {
                        log.debug("DataDam tracking start failed", e);
                    }
                }
            }
        });
    }

    private void logOut() {
        runOperation(DDTrackingUIMessages.sync_preference_page_logout_failed, monitor -> {
            stopTracking();
            accessService.signOut();
        });
    }

    private void stopTracking() {
        try {
            tracker.stop();
        } catch (DBException e) {
            log.debug("DataDam tracking stop failed", e);
            tracker.reset();
        }
    }

    private void refresh() {
        DDKeyBundle bundle = accessService.getKeyBundle();
        boolean present = bundle != null;
        accountRow.setVisible(present);
        ((GridData) accountRow.getLayoutData()).exclude = !present;
        DDDesktopAccessService.Status status = accessService.getStatus();
        boolean signedIn = status == DDDesktopAccessService.Status.SIGNED_IN;
        boolean showUser = signedIn || status == DDDesktopAccessService.Status.EXPIRED;
        userRow.setVisible(showUser);
        ((GridData) userRow.getLayoutData()).exclude = !showUser;
        accountText.setText(present ? bundle.accountId().toString() : DDTrackingUIMessages.sync_preference_page_unknown);
        userText.setText(DDTrackingUIMessages.sync_preference_page_unknown);
        loginButton.setEnabled(!signedIn);
        reloginButton.setEnabled(signedIn || status == DDDesktopAccessService.Status.EXPIRED);
        logoutButton.setEnabled(status != DDDesktopAccessService.Status.SIGNED_OUT);
        importKeysButton.setEnabled(signedIn);
        forgetKeysButton.setEnabled(present);
        Composite keysSection = keysGroup.getParent();
        keysSection.setVisible(signedIn);
        ((GridData) keysSection.getLayoutData()).exclude = !signedIn;
        keysSection.getParent().layout(true, true);
        loadLoggedUserView();
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
                    if (!accountText.isDisposed() && userViewJob == this && !monitor.isCanceled()) {
                        userText.setText(user.userName() == null
                            ? DDTrackingUIMessages.sync_preference_page_unknown : user.userName());
                        accountText.setText(user.accountName() != null ? user.accountName()
                            : user.accountId() == null ? DDTrackingUIMessages.sync_preference_page_unknown
                                : user.accountId().toString());
                    }
                });
                return Status.OK_STATUS;
            }
        };
        userViewJob.setSystem(true);
        userViewJob.schedule();
    }
}
