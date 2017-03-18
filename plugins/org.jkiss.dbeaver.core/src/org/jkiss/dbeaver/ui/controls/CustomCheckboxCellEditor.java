/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.utils.CommonUtils;

/**
 * Checkbox cell editor
 */
public class CustomCheckboxCellEditor extends CellEditor {

    private Button checkBox;

    public CustomCheckboxCellEditor(Composite parent) {
        this(parent, SWT.NONE);
    }

    public CustomCheckboxCellEditor(Composite parent, int style) {
        super(parent, style);
    }

    @Override
    protected Control createControl(Composite parent) {
        checkBox = new Button(parent, SWT.CHECK);
        checkBox.setFont(parent.getFont());
        checkBox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                applyEditorValue();
                // This is needed for MacOS
                fireApplyEditorValue();
            }
        });

        checkBox.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                CustomCheckboxCellEditor.this.focusLost();
            }
        });

        return checkBox;
    }

    @Override
    protected Boolean doGetValue() {
        return checkBox.getSelection();
    }

    @Override
    protected void doSetFocus() {
        checkBox.setFocus();
    }

    @Override
    protected void doSetValue(Object value) {
        Assert.isTrue(checkBox != null && (value instanceof Boolean));
        checkBox.setSelection(CommonUtils.toBoolean(value));
    }

    @Override
    public LayoutData getLayoutData() {
        LayoutData layoutData = super.getLayoutData();
        layoutData.grabHorizontal = false;
        layoutData.horizontalAlignment = SWT.CENTER;
        return layoutData;
    }

    void applyEditorValue() {
        // must set the selection before getting value
        Object newValue = doGetValue();
        markDirty();
        boolean isValid = isCorrect(newValue);
        setValueValid(isValid);

        //fireApplyEditorValue();
    }

    protected int getDoubleClickTimeout() {
        return 0;
    }
}
