/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.utils.CommonUtils;

import java.sql.JDBCType;
import java.sql.Timestamp;
import java.util.Optional;

public class CustomInstantCellEditor extends DialogCellEditor {
    private Text textEditor;
    private FocusAdapter textFocusListener;

    public CustomInstantCellEditor(Composite parent) {
        super(parent);
    }

    @Override
    protected Button createButton(Composite parent) {
        Button result = new Button(parent, SWT.DOWN | SWT.NO_FOCUS);
        result.setImage(DBeaverIcons.getImage(UIIcon.DOTS_BUTTON)); //$NON-NLS-1$
        return result;
    }

    @Override
    protected Control createContents(Composite cell) {
        textEditor = new Text(cell, SWT.LEFT);
        textEditor.setFont(cell.getFont());
        textEditor.setBackground(cell.getBackground());
        textEditor.addTraverseListener(e -> {
            if (e.detail == SWT.TRAVERSE_RETURN) {
                e.doit = false;
                e.detail = SWT.TRAVERSE_NONE;
                focusLost();
            }
        });
        textFocusListener = new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                applyEditorValueFromText(textEditor.getText(), new Shell(cell.getShell()));
                if (!UIUtils.hasFocus(cell)) {
                    CustomInstantCellEditor.this.fireApplyEditorValue();
                }
            }
        };
        textEditor.addFocusListener(textFocusListener);

        return textEditor;
    }

    private void applyEditorValueFromText(String text, Shell shell) {
        if (CommonUtils.isEmpty(text)) {
            setValue(null);
            return;
        }

        try {
            Timestamp timestamp = Timestamp.valueOf(text);
            setValue(timestamp);
        } catch (Exception ex) {
            ErrorDialog.openError(
                shell,
                "Failed to parse timestamp",
                null,
                Status.warning("Invalid timestamp format", ex)
            );
        }
    }

    @Override
    protected Object openDialogBox(Control cellEditorWindow) {
        textEditor.removeFocusListener(textFocusListener);

        CustomTimeEditorDialog customTimeEditorDialog = new CustomTimeEditorDialog(cellEditorWindow.getShell());
        int returnCode = customTimeEditorDialog.open();

        Object result = null;
        if (returnCode == Window.CANCEL) {
            result = doGetValue();
        } else {
            result = customTimeEditorDialog.result();
        }

        textEditor.addFocusListener(textFocusListener);
        return result;
    }

    @Override
    protected void updateContents(Object value) {
        if (value == null) {
            textEditor.setText("");
            return;
        }

        textEditor.setText(value.toString());
        textEditor.selectAll();
    }

    @Override
    protected void doSetValue(Object value) {
        super.doSetValue(value);
    }

    @Override
    protected Object doGetValue() {
        return super.doGetValue();
    }

    @Override
    protected void doSetFocus() {
        textEditor.setFocus();
    }

    private class CustomTimeEditorDialog extends BaseDialog {
        private static final Log log = Log.getLog(CustomTimeEditorDialog.class);

        private Timestamp result;

        public CustomTimeEditorDialog(
            Shell parent
        ) {
            super(parent, "Select Date and Time", null);
        }

        @Override
        protected Composite createDialogArea(Composite parent) {
            // Create the main composite
            Composite mainComposite = new Composite(parent, SWT.NONE);
            mainComposite.setLayout(new GridLayout(1, false));
            mainComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

            // Initialize CustomTimeEditor
            CustomTimeEditor customTimeEditor = new CustomTimeEditor(mainComposite, SWT.NONE, false, false);
            customTimeEditor.createDateFormat(JDBCType.TIMESTAMP);
            GridData layoutData = new GridData(SWT.FILL, SWT.CENTER, true, false);
            customTimeEditor.getControl().setLayoutData(layoutData);
            customTimeEditor.setEditable(true);

            try {
                customTimeEditor.setValue(doGetValue());
            } catch (DBCException e) {
                log.error("Error setting initial value", e);
            }

            customTimeEditor.addSelectionAdapter(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    result = Optional.ofNullable(customTimeEditor.getValueAsDate())
                        .map(v -> Timestamp.from(v.toInstant()))
                        .orElse(null);
                }
            });

            return mainComposite;
        }

        @Nullable
        public Timestamp result() {
            return result;
        }
    }

}
