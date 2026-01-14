/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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

package org.jkiss.dbeaver.ui.dialogs;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

/**
 * StandardErrorDialog
 */
public class StandardErrorDialog extends BaseErrorDialog implements BlockingPopupDialog {

    private static final String DIALOG_ID = "DBeaver.StandardErrorDialog";//$NON-NLS-1$

    public StandardErrorDialog(
        @NotNull Shell parentShell,
        @NotNull String dialogTitle,
        @Nullable String message,
        @NotNull IStatus status,
        int displayMask)
    {
        super(parentShell, dialogTitle, message, status, displayMask);
        setStatus(status);
        if (message == null) {
            IStatus rootStatus = GeneralUtils.getRootStatus(status);
            if (rootStatus.getException() != null) {
                String lastMessage = GeneralUtils.makeStandardErrorMessage(rootStatus.getException());
                if (CommonUtils.isEmpty(lastMessage)) {
                    for (Throwable e = rootStatus.getException(); e != null; e = e.getCause()) {
                        if (e.getMessage() != null) {
                            lastMessage = e.getMessage();
                        }
                    }
                    if (CommonUtils.isEmpty(lastMessage)) {
                        lastMessage = rootStatus.getMessage();
                        if (CommonUtils.isEmpty(lastMessage)) {
                            lastMessage = status.getMessage();
                            if (CommonUtils.isEmpty(lastMessage)) {
                                // No message at all. This may happen in case of NPE and other messageless errors.
                                // Let's use exception name then
                                if (rootStatus.getException() != null) {
                                    lastMessage = rootStatus.getException().getClass().getName();
                                }
                            }
                        }
                    }
                }
                this.message = CommonUtils.cutExtraLines(lastMessage, 20);
            } else {
                this.message = CommonUtils.cutExtraLines(rootStatus.getMessage(), 20);
            }
        } else {
            this.message = CommonUtils.cutExtraLines(message, 20); //$NON-NLS-1$
        }
        // Truncate message to 64kb
        this.message = CommonUtils.truncateString(this.message, 64000);
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings() {
        return UIUtils.getDialogSettings(DIALOG_ID);
    }

    @Override
    protected int getDialogBoundsStrategy() {
        return DIALOG_PERSISTLOCATION;
    }

    @Override
    protected Composite createContents(@NotNull Composite parent) {
        Composite contents = super.createContents(parent);

        if (getDialogBoundsSettings().getBoolean("showDetails")) {
            this.showDetailsArea();
        }

        return contents;
    }

    @Override
    public void create() {
        super.create();
//        Point prefSize = getContents().computeSize(SWT.DEFAULT, SWT.DEFAULT);
//        Point actualSize = getShell().getSize();
//        if ((prefSize.x < MAX_AUTO_SIZE_X && prefSize.x > actualSize.x) ||
//            (prefSize.y < MAX_AUTO_SIZE_Y && prefSize.y > actualSize.y)) {
//            if (prefSize.x > actualSize.x) {
//                actualSize.x = prefSize.x;
//            }
//            if (prefSize.y > actualSize.y) {
//                actualSize.y = prefSize.y;
//            }
//            getShell().setSize(actualSize);
//        }
        UIUtils.asyncExec(() -> {
            Button okButton = getButton(IDialogConstants.OK_ID);
            if (okButton != null) {
                okButton.setFocus();
            }
        });
    }

    @Override
    public boolean close() {
        getDialogBoundsSettings().put("showDetails", isDetailsVisible());
        return super.close();
    }

}