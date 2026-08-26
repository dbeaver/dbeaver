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
package org.jkiss.dbeaver.ui.ai.engine.openai;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.internal.AIUIMessages;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

class OpenAIAccountAuthDialog extends Dialog {
    private static final int COPY_LINK_ID = IDialogConstants.CLIENT_ID + 1;

    private final URI authorizationUri;
    private final CompletableFuture<Void> completion;

    OpenAIAccountAuthDialog(
        @NotNull Shell parentShell,
        @NotNull URI authorizationUri,
        @NotNull CompletableFuture<Void> completion
    ) {
        super(parentShell);
        this.authorizationUri = authorizationUri;
        this.completion = completion;
        setShellStyle(SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        getShell().setText(AIUIMessages.openai_configurator_sign_in_dialog_title);

        Label label = new Label(composite, SWT.WRAP);
        label.setText(AIUIMessages.openai_configurator_browser_dialog_message);
        label.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

        Link authorizationLink = new Link(composite, SWT.NONE);
        authorizationLink.setText("<a>" + AIUIMessages.openai_configurator_authorization_link + "</a>");
        authorizationLink.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).hint(420, SWT.DEFAULT).create());
        authorizationLink.addListener(SWT.Selection, event -> UIUtils.openWebBrowser(authorizationUri.toString()));
        return composite;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, COPY_LINK_ID, AIUIMessages.openai_configurator_copy_link, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    protected void buttonPressed(int buttonId) {
        if (buttonId == COPY_LINK_ID) {
            UIUtils.setClipboardContents(getShell().getDisplay(), TextTransfer.getInstance(), authorizationUri.toString());
        } else {
            super.buttonPressed(buttonId);
        }
    }

    @Override
    protected void cancelPressed() {
        completion.cancel(false);
        super.cancelPressed();
    }

    @Override
    protected void handleShellCloseEvent() {
        completion.cancel(false);
        super.handleShellCloseEvent();
    }
}
