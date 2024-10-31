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
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
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
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public class CustomTimestampCellEditor extends DialogCellEditor {
    private Text textEditor;
    private FocusListener textFocusListener;

    public CustomTimestampCellEditor(@NotNull Composite parent) {
        super(parent);
    }

    @Nullable
    @Override
    protected Button createButton(@NotNull Composite parent) {
        Button result = new Button(parent, SWT.DOWN | SWT.NO_FOCUS);
        result.setImage(DBeaverIcons.getImage(UIIcon.DOTS_BUTTON)); //$NON-NLS-1$
        return result;
    }

    @Nullable
    @Override
    protected Control createContents(@NotNull Composite cell) {
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


        textFocusListener = FocusListener.focusLostAdapter(e -> {
            applyEditorValueFromText(textEditor.getText(), new Shell(cell.getShell()));
            UIUtils.asyncExec(() -> {
                if (!UIUtils.hasFocus(cell)) {
                    CustomTimestampCellEditor.this.fireApplyEditorValue();
                }
            });
        });

        textEditor.addFocusListener(textFocusListener);
        textEditor.addMouseListener(MouseListener.mouseDoubleClickAdapter(e -> openDialogBox(cell)));

        return textEditor;
    }

    @Nullable
    @Override
    protected Object doGetValue() {
        return truncateToSeconds((Timestamp) super.doGetValue());
    }

    private void applyEditorValueFromText(@Nullable String text, @NotNull Shell shell) {
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

    @Nullable
    @Override
    protected Object openDialogBox(Control cellEditorWindow) {
        textEditor.removeFocusListener(textFocusListener);

        Object currentValue = doGetValue();
        var initialValue = currentValue != null
            ? (Timestamp) currentValue
            // Default to 30 days from now
            : Timestamp.valueOf(LocalDateTime.now().plusDays(30));

        CustomTimeEditorDialog customTimeEditorDialog = new CustomTimeEditorDialog(
            cellEditorWindow.getShell(),
            initialValue
        );

        int returnCode = customTimeEditorDialog.open();
        Object result = switch (returnCode) {
            case Window.OK -> customTimeEditorDialog.result();
            case Window.CANCEL -> currentValue;
            default -> null;
        };

        textEditor.addFocusListener(textFocusListener);
        return result;
    }

    @Override
    protected void updateContents(@Nullable Object value) {
        if (value == null) {
            textEditor.setText("");
            return;
        }

        textEditor.setText(value.toString());
        textEditor.selectAll();
    }

    @Override
    protected void doSetFocus() {
        textEditor.setFocus();
    }

    @Override
    protected void doSetValue(@Nullable Object value) {
        super.doSetValue(truncateToSeconds((Timestamp) value));
    }

    private Timestamp truncateToSeconds(@Nullable Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }

        return Timestamp.valueOf(timestamp.toLocalDateTime().truncatedTo(ChronoUnit.SECONDS));
    }

    private static class CustomTimeEditorDialog extends BaseDialog {
        private static final Log log = Log.getLog(CustomTimeEditorDialog.class);

        @Nullable
        private Timestamp value;

        public CustomTimeEditorDialog(
            @NotNull Shell parent,
            @Nullable Timestamp value
        ) {
            super(parent, "Select Date and Time", null);
            this.value = value;
        }

        @Override
        protected Composite createDialogArea(@NotNull Composite parent) {
            CustomTimeEditor customTimeEditor = new CustomTimeEditor(parent, SWT.NONE, false, false);
            customTimeEditor.createDateFormat(JDBCType.TIMESTAMP);
            GridData layoutData = new GridData(SWT.FILL, SWT.CENTER, true, false);
            customTimeEditor.getControl().setLayoutData(layoutData);
            customTimeEditor.setEditable(true);

            try {
                customTimeEditor.setValue(value);
            } catch (DBCException e) {
                log.error("Error setting initial value", e);
            }

            customTimeEditor.addSelectionAdapter(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    value = Optional.ofNullable(customTimeEditor.getValueAsDate())
                        .map(v -> Timestamp.from(v.toInstant()))
                        .orElse(null);
                }
            });

            return customTimeEditor.getControl();
        }

        @Nullable
        public Timestamp result() {
            return value;
        }
    }
}
