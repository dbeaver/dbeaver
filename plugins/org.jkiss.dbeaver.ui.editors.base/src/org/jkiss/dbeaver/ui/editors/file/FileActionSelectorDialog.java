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
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.editors.internal.EditorsMessages;

import java.util.List;

/**
 * FileActionSelectorDialog
 */
public class FileActionSelectorDialog extends BaseDialog {

    private final List<FileTypeAction> actions;
    private FileTypeAction selectedAction;

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
            radio.setText(action.getLabel());
            radio.setSelection(action == selectedAction);
            radio.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> {
                if (radio.getSelection()) {
                    selectedAction = action;
                }
            }));
        }

        return composite;
    }

    @Nullable
    public FileTypeAction getSelectedAction() {
        return selectedAction;
    }
}
