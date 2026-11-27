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
package org.jkiss.dbeaver.ui.datadam.project.keys;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIMessages;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;

/** One-time display of the encryption key. Both confirmed and warned dismissal lead to local import. */
public final class DDGeneratedEncryptionKeyDialog extends BaseDialog {
    private final String phrase;
    private Button savedCheckbox;

    public DDGeneratedEncryptionKeyDialog(@Nullable Shell parentShell, @NotNull String phrase) {
        super(parentShell, DDTrackingUIMessages.generate_key_dialog_title, null);
        this.phrase = phrase;
    }

    @NotNull
    @Override
    protected Composite createDialogArea(@NotNull Composite parent) {
        Composite content = super.createDialogArea(parent);
        createWrappedLabel(content, DDTrackingUIMessages.generate_key_dialog_description);
        Text phraseText = new Text(content, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.READ_ONLY | SWT.V_SCROLL);
        phraseText.setText(phrase);
        GridData phraseLayout = new GridData(GridData.FILL_HORIZONTAL);
        phraseLayout.widthHint = 500;
        phraseLayout.heightHint = UIUtils.getFontHeight(phraseText) * 6;
        phraseText.setLayoutData(phraseLayout);
        UIUtils.createPushButton(content, DDTrackingUIMessages.generate_key_dialog_copy, null,
            SelectionListener.widgetSelectedAdapter(e ->
                UIUtils.setClipboardContents(getShell().getDisplay(), TextTransfer.getInstance(), phrase)));
        savedCheckbox = new Button(content, SWT.CHECK);
        savedCheckbox.setText(DDTrackingUIMessages.generate_key_dialog_saved);
        savedCheckbox.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        savedCheckbox.addSelectionListener(SelectionListener.widgetSelectedAdapter(e ->
            getButton(IDialogConstants.OK_ID).setEnabled(savedCheckbox.getSelection())));
        return content;
    }

    @Override
    protected void createButtonsForButtonBar(@NotNull Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true).setEnabled(false);
    }

    @Override
    protected void okPressed() {
        if (savedCheckbox.getSelection()) {
            super.okPressed();
        }
    }

    @Override
    protected void cancelPressed() {
        if (confirmClose()) {
            super.cancelPressed();
        }
    }

    @Override
    protected void handleShellCloseEvent() {
        if (confirmClose()) {
            super.handleShellCloseEvent();
        }
    }

    private boolean confirmClose() {
        if (savedCheckbox.getSelection()) {
            return true;
        }
        return new MessageDialog(getShell(), DDTrackingUIMessages.generate_key_dialog_discard_title, null,
            DDTrackingUIMessages.generate_key_dialog_discard_message, MessageDialog.WARNING,
            new String[] { DDTrackingUIMessages.generate_key_dialog_go_back,
                DDTrackingUIMessages.generate_key_dialog_close_anyway }, 0).open() == 1;
    }

    private static void createWrappedLabel(@NotNull Composite parent, @NotNull String text) {
        Label label = new Label(parent, SWT.WRAP);
        label.setText(text);
        GridData layout = new GridData(GridData.FILL_HORIZONTAL);
        layout.widthHint = 500;
        label.setLayoutData(layout);
    }
}
