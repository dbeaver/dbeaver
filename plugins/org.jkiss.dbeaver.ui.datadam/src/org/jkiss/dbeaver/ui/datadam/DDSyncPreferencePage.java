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

import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.datadam.DDAccountClient;
import org.jkiss.dbeaver.model.datadam.DDEndpoints;
import org.jkiss.dbeaver.model.datadam.auth.DDCryptoState;
import org.jkiss.dbeaver.model.datadam.auth.DDKeyBundle;
import org.jkiss.dbeaver.model.datadam.auth.DDKeyStore;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopTokenManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIMessages;
import org.jkiss.dbeaver.ui.datadam.project.DDProjectSyncUIManager;
import org.jkiss.dbeaver.ui.preferences.AbstractPrefPage;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

public class DDSyncPreferencePage extends AbstractPrefPage implements IWorkbenchPreferencePage {

    private static final String SYNC_TITLE = DDTrackingUIMessages.sync_preference_page_title;

    private Text accountText;
    private Text statusText;
    private Button loginButton;
    private Button logoutButton;
    private Button importKeysButton;
    private Button forgetKeysButton;

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
        statusText = UIUtils.createLabelText(
            group, DDTrackingUIMessages.sync_preference_page_status_label, "", SWT.READ_ONLY, idFieldLayout());

        Composite buttons = UIUtils.createComposite(group, 2);
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.horizontalSpan = 2;
        buttons.setLayoutData(gd);
        loginButton = UIUtils.createPushButton(
            buttons, DDTrackingUIMessages.sync_preference_page_desktop_sign_in, null,
            SelectionListener.widgetSelectedAdapter(e -> logIn()));
        logoutButton = UIUtils.createPushButton(
            buttons, DDTrackingUIMessages.sync_preference_page_desktop_sign_out, null,
            SelectionListener.widgetSelectedAdapter(e -> logOut()));

        Composite keysGroup = UIUtils.createTitledComposite(
            composite, DDTrackingUIMessages.sync_preference_page_encryption_keys_group,
            2, GridData.FILL_HORIZONTAL, SWT.DEFAULT);
        accountText = UIUtils.createLabelText(
            keysGroup, DDTrackingUIMessages.sync_preference_page_account_label, "", SWT.READ_ONLY, idFieldLayout());
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

    @NotNull
    private <T> T runInProgress(@NotNull DBSupplier<T> supplier) throws DBException {
        AtomicReference<T> holder = new AtomicReference<>();
        try {
            UIUtils.runInProgressDialog(monitor -> {
                try {
                    holder.set(supplier.get(monitor));
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                }
            });
        } catch (InvocationTargetException e) {
            throw unwrap(e);
        }
        return holder.get();
    }

    @NotNull
    private static DBException unwrap(@NotNull InvocationTargetException e) {
        Throwable target = e.getTargetException();
        return target instanceof DBException dbException
            ? dbException
            : new DBException(String.valueOf(target.getMessage()), target);
    }

    @FunctionalInterface
    private interface DBSupplier<T> {
        T get(@NotNull DBRProgressMonitor monitor) throws DBException;
    }

    @NotNull
    private static GridData idFieldLayout() {
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.widthHint = UIUtils.getFontHeight(Display.getCurrent().getSystemFont()) * 22;
        return gd;
    }

    private void importKeys() {
        String siteUrl = DDEndpoints.getAccountBaseUrl();
        DDCryptoState state;
        try {
            state = runInProgress(monitor -> new DDAccountClient(URI.create(siteUrl)).getCryptoState());
        } catch (DBException | IllegalArgumentException e) {
            DBWorkbench.getPlatformUI().showError(
                SYNC_TITLE, DDTrackingUIMessages.sync_preference_page_import_keys_failed, e);
            return;
        }
        if (state == null) {
            return;
        }
        if (!state.cryptoConfigured()) {
            DBWorkbench.getPlatformUI().showMessageBox(
                SYNC_TITLE,
                DDTrackingUIMessages.sync_preference_page_encryption_not_configured,
                true);
            return;
        }
        DDImportKeyDialog dialog = new DDImportKeyDialog(getShell());
        if (dialog.open() != Window.OK) {
            return;
        }
        DDKeyBundle[] keyBundle = new DDKeyBundle[1];
        try {
            UIUtils.runInProgressDialog(monitor -> {
                try {
                    keyBundle[0] = DDKeyStore.unpack(state, dialog.getPhrase());
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                }
            });
            DDKeyStore.save(keyBundle[0]);
            DDProjectSyncUIManager.getInstance().refresh();
            DDTrackingInitializer.start();
            refresh();
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError(SYNC_TITLE, DDTrackingUIMessages.sync_preference_page_import_keys_failed, e);
        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError(
                SYNC_TITLE, DDTrackingUIMessages.sync_preference_page_import_keys_failed, e.getTargetException());
        }
    }

    private void forgetKeys() {
        try {
            DDTrackingInitializer.stop();
            DDKeyStore.clear();
            DDProjectSyncUIManager.getInstance().refresh();
            refresh();
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError(SYNC_TITLE, DDTrackingUIMessages.sync_preference_page_cannot_forget_keys, e);
        }
    }

    private void logIn() {
        String account = DDEndpoints.getAccountBaseUrl();
        String storage = DDEndpoints.getStorageBaseUrl();
        try {
            Boolean signedIn = runInProgress(monitor -> {
                try {
                    DDDesktopTokenManager.getInstance().signIn(URI.create(account), URI.create(storage), monitor);
                    return true;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new DBException("DataDam sign-in was interrupted", e);
                } catch (IllegalArgumentException e) {
                    throw new DBException("Invalid DataDam server URL", e);
                }
            });
            if (!Boolean.TRUE.equals(signedIn)) {
                return;
            }
            DDTrackingInitializer.start();
            refresh();
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError(SYNC_TITLE, DDTrackingUIMessages.sync_preference_page_login_failed, e);
        }
    }

    private void logOut() {
        DDTrackingInitializer.stop();
        try {
            DDDesktopTokenManager.getInstance().signOut();
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError(SYNC_TITLE, DDTrackingUIMessages.sync_preference_page_logout_failed, e);
        }
        DDProjectSyncUIManager.getInstance().refresh();
        refresh();
    }

    private void refresh() {
        DDKeyBundle bundle = DDKeyStore.load();
        boolean present = bundle != null;
        DDDesktopTokenManager.Status status = DDDesktopTokenManager.getInstance().getStatus();
        boolean signedIn = status == DDDesktopTokenManager.Status.SIGNED_IN;
        statusText.setText(signedIn ? DDTrackingUIMessages.sync_preference_page_signed_in
            : status == DDDesktopTokenManager.Status.EXPIRED ? DDTrackingUIMessages.sync_preference_page_session_expired
                : DDTrackingUIMessages.sync_preference_page_signed_out);
        accountText.setText(present ? bundle.accountId().toString() : "");
        loginButton.setEnabled(!signedIn);
        logoutButton.setEnabled(status != DDDesktopTokenManager.Status.SIGNED_OUT);
        importKeysButton.setEnabled(signedIn);
        forgetKeysButton.setEnabled(present);
    }
}
