/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.EditTextDialog;
import org.jkiss.dbeaver.ui.internal.UIMessages;
import org.jkiss.utils.CommonUtils;

/**
 * Text editor with dialog
 */
public class AdvancedTextCellEditor extends DialogCellEditor {

    private boolean wasNull;
    private Text textEditor;
    private FocusAdapter textFocusListener;

    public AdvancedTextCellEditor(Composite parent)
    {
        super(parent);
    }

    @Override
    protected void doSetFocus() {
        if (textEditor != null) {
            textEditor.setFocus();
        } else {
            super.doSetFocus();
        }
    }

    @Override
    protected Object doGetValue()
    {
        final Object value;
        if (textEditor == null || textEditor.isDisposed()) {
            value = super.doGetValue();
        } else {
            value = textEditor.getText();
        }
        if (wasNull && "".equals(value)) { //$NON-NLS-1$
            return null;
        } else {
            return value;
        }
    }

    @Override
    protected void doSetValue(Object value)
    {
        if (value == null) {
            wasNull = true;
        }
        super.doSetValue(CommonUtils.toString(value));
    }

    protected Button createButton(Composite parent) {
        Button result = new Button(parent, SWT.DOWN | SWT.NO_FOCUS);
        //result.setText("..."); //$NON-NLS-1$
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
                UIUtils.asyncExec(() -> {
                    if (!UIUtils.hasFocus(cell)) {
                        AdvancedTextCellEditor.this.focusLost();
                    }
                });
            }
        };
        textEditor.addFocusListener(textFocusListener);

        return textEditor;
    }

    @Override
    protected void updateContents(Object value) {
        if (textEditor == null) {
            return;
        }
        if (value != null) {
            textEditor.setText((String)value);
            textEditor.selectAll();
        }
    }

    @Override
    protected Object openDialogBox(Control cellEditorWindow) {
        textEditor.removeFocusListener(textFocusListener);
        String value = EditTextDialog.editText(cellEditorWindow.getShell(), UIMessages.edit_text_dialog_title_edit_value, (String) getValue());
        textEditor.addFocusListener(textFocusListener);

        return value;
    }

    protected int getDoubleClickTimeout() {
        return 0;
    }

    @Override
    public void activate() {
        super.activate();
    }

    @Override
    public void deactivate() {
//        if (!UIUtils.hasFocus(textEditor)) {
//            focusLost();
//        }
        super.deactivate();
    }
}
