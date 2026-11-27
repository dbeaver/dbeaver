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
package org.jkiss.dbeaver.ui.editors.file;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.file.FileTypeAction;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.editors.internal.EditorsMessages;

import java.util.List;

/**
 * FileActionSelectorDialog
 */
public class FileActionSelectorDialog extends BaseDialog {

    private final List<FileTypeAction> actions;
    private FileTypeAction selectedAction;
    private boolean rememberAction;

    public FileActionSelectorDialog(@NotNull Shell shell, @NotNull List<FileTypeAction> actions) {
        super(shell, EditorsMessages.dialog_file_type_selector_title, null);
        this.actions = actions;
        this.selectedAction = actions.isEmpty() ? null : actions.getFirst();
    }

    @NotNull
    @Override
    protected Composite createDialogArea(@NotNull Composite parent) {
        Composite composite = super.createDialogArea(parent);

        for (FileTypeAction action : actions) {
            Button radio = new Button(composite, SWT.RADIO);
            radio.setText(getActionLabel(action));
            radio.setSelection(action == selectedAction);
            radio.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> {
                if (radio.getSelection()) {
                    selectedAction = action;
                }
            }));
        }

        UIUtils.createCheckbox(composite, EditorsMessages.dialog_file_type_selector_dont_ask_again, false)
            .addSelectionListener(SelectionListener.widgetSelectedAdapter(event ->
                rememberAction = ((Button) event.widget).getSelection()));

        return composite;
    }

    public boolean isRememberAction() {
        return rememberAction;
    }

    @NotNull
    public static String getActionLabel(@NotNull FileTypeAction action) {
        return switch (action) {
            case DATABASE -> EditorsMessages.file_open_as_database;
            case INTERNAL_EDITOR -> EditorsMessages.file_open_as_text;
            case EXTERNAL_EDITOR -> EditorsMessages.file_open_as_external;
        };
    }

    @Nullable
    public FileTypeAction getSelectedAction() {
        return selectedAction;
    }
}
