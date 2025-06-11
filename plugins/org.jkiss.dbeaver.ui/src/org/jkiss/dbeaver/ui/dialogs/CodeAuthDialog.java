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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.widgets.LabelFactory;
import org.eclipse.jface.widgets.LinkFactory;
import org.eclipse.jface.widgets.TextFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ui.ShellUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.internal.UIMessages;

import java.net.URI;
import java.util.concurrent.Future;

/**
 * A simple dialog showing the browser url of the authorization page and the code the user is supposed to enter there.
 */
public class CodeAuthDialog extends Dialog implements BlockingPopupDialog {
    private final URI browserUrl;
    private final String userCode;
    private final Future<Void> future;

    public CodeAuthDialog(
        @NotNull Shell shell,
        @NotNull URI browserUrl,
        @NotNull String userCode,
        @NotNull Future<Void> future
    ) {
        super(shell);
        this.browserUrl = browserUrl;
        this.userCode = userCode;
        this.future = future;

        setShellStyle(SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);

        getShell().setText(UIMessages.dialog_auth_code_title);

        LabelFactory.newLabel(SWT.WRAP)
            .text(UIMessages.dialog_auth_code_browser_label)
            .layoutData(GridDataFactory.fillDefaults().grab(true, false).create())
            .create(composite);

        LinkFactory.newLink(SWT.NONE)
            .text("<a>" + browserUrl + "</a>")
            .onSelect(e -> ShellUtils.launchProgram(browserUrl.toString()))
            .create(composite);

        LabelFactory.newLabel(SWT.WRAP)
            .text(UIMessages.dialog_auth_code_code_label)
            .layoutData(GridDataFactory.fillDefaults().grab(true, false).create())
            .create(composite);

        Text userCodeText = TextFactory.newText(SWT.READ_ONLY | SWT.CENTER)
            .text(userCode)
            .font(JFaceResources.getFont(JFaceResources.HEADER_FONT))
            .layoutData(GridDataFactory.fillDefaults().grab(true, false).create())
            .create(composite);

        userCodeText.addFocusListener(FocusListener.focusGainedAdapter(e -> userCodeText.selectAll()));
        userCodeText.setFocus();

        return composite;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OPEN_ID, UIMessages.dialog_auth_code_copy_and_open_label, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    protected void buttonPressed(int buttonId) {
        if (buttonId == IDialogConstants.OPEN_ID) {
            UIUtils.setClipboardContents(getShell().getDisplay(), TextTransfer.getInstance(), userCode);
            ShellUtils.launchProgram(browserUrl.toString());
        } else {
            super.buttonPressed(buttonId);
        }
    }

    @Override
    protected void cancelPressed() {
        future.cancel(false);
        super.cancelPressed();
    }
}
