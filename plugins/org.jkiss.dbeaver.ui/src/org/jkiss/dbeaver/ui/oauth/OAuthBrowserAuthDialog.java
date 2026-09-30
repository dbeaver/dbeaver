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
package org.jkiss.dbeaver.ui.oauth;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.widgets.LabelFactory;
import org.eclipse.jface.widgets.LinkFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ui.ShellUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.internal.UIMessages;

import java.net.URI;
import java.util.concurrent.Future;

/**
 * Modal dialog shown while an OAuth authorization is completed in the browser.
 */
public class OAuthBrowserAuthDialog extends Dialog {
    private static final int COPY_LINK_ID = IDialogConstants.CLIENT_ID + 1;

    @NotNull
    private final URI browserUrl;
    @NotNull
    private final Future<Void> future;

    public OAuthBrowserAuthDialog(@NotNull Shell shell, @NotNull URI browserUrl, @NotNull Future<Void> future) {
        super(shell);
        this.browserUrl = browserUrl;
        this.future = future;
        setShellStyle(SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        getShell().setText(UIMessages.dialog_auth_code_title);

        LabelFactory.newLabel(SWT.WRAP)
            .text(UIMessages.dialog_oauth_browser_message)
            .layoutData(GridDataFactory.fillDefaults().grab(true, false).create())
            .create(composite);
        LinkFactory.newLink(SWT.NONE)
            .text("<a>" + UIMessages.dialog_oauth_browser_open_label + "</a>")
            .layoutData(GridDataFactory.fillDefaults().grab(true, false).hint(420, SWT.DEFAULT).create())
            .onSelect(event -> ShellUtils.launchProgram(browserUrl.toString()))
            .create(composite);
        return composite;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, COPY_LINK_ID, UIMessages.dialog_auth_code_copy_link_label, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    protected void buttonPressed(int buttonId) {
        if (buttonId == COPY_LINK_ID) {
            UIUtils.setClipboardContents(getShell().getDisplay(), TextTransfer.getInstance(), browserUrl.toString());
        } else {
            super.buttonPressed(buttonId);
        }
    }

    @Override
    protected void cancelPressed() {
        future.cancel(false);
        super.cancelPressed();
    }

    @Override
    protected void handleShellCloseEvent() {
        future.cancel(false);
        super.handleShellCloseEvent();
    }
}
