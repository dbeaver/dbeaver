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
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopAccessService;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIMessages;
import org.jkiss.dbeaver.ui.preferences.AbstractPrefPage;

import java.lang.reflect.InvocationTargetException;

public class DDSyncPreferencePage extends AbstractPrefPage implements IWorkbenchPreferencePage {

    private static final String SYNC_TITLE = DDTrackingUIMessages.sync_preference_page_title;
    private final DDDesktopAccessService accessService = DDDesktopAccessService.getInstance();
    private AbstractJob userViewJob;

    private Composite content;
    private Text userText;
    private Button accountActionButton;
    private Button refreshTokenButton;

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
        DDUIUtils.create(content, DDTrackingUIMessages.sync_preference_page_description,
            DDTrackingUIMessages.sync_preference_page_about_link, DDUIConstants.DATADAM_DOCS_URL
        );
        Composite authenticationGroup = UIUtils.createTitledComposite(
            content, DDTrackingUIMessages.sync_preference_page_authentication_group,
            2, GridData.FILL_HORIZONTAL, SWT.DEFAULT
        );
        ((GridData) authenticationGroup.getParent().getLayoutData()).verticalIndent = 16;
        userText = new Text(authenticationGroup, SWT.READ_ONLY);
        UIUtils.fixReadonlyTextBackground(userText);
        GridData statusLayout = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
        statusLayout.horizontalSpan = 2;
        userText.setLayoutData(statusLayout);
        accountActionButton = UIUtils.createPushButton(
            authenticationGroup, DDTrackingUIMessages.sync_preference_page_desktop_sign_in, null,
            SelectionListener.widgetSelectedAdapter(e -> {
                DDDesktopAccessService.Status status = accessService.getStatus();
                if (status == DDDesktopAccessService.Status.SIGNED_IN || status == DDDesktopAccessService.Status.EXPIRED) {
                    logOut();
                } else {
                    logIn();
                }
            })
        );
        refreshTokenButton = UIUtils.createPushButton(
            authenticationGroup, DDTrackingUIMessages.sync_preference_page_refresh_token, null,
            SelectionListener.widgetSelectedAdapter(e -> logIn()));
        refreshTokenButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

        refresh();
        return content;
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
        boolean canRefresh = signedIn || status == DDDesktopAccessService.Status.EXPIRED;
        accountActionButton.setText(canRefresh
            ? DDTrackingUIMessages.sync_preference_page_desktop_sign_out
            : DDTrackingUIMessages.sync_preference_page_desktop_sign_in);
        refreshTokenButton.setVisible(canRefresh);
        ((GridData) refreshTokenButton.getLayoutData()).exclude = !canRefresh;
        content.layout(true, true);
        if (signedIn) {
            loadLoggedUserView();
        } else if (userViewJob != null) {
            userViewJob.cancel();
        }
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
