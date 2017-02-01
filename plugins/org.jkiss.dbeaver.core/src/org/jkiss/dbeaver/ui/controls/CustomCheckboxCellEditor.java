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
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

/**
 * Checkbox cell editor
 */
public class CustomCheckboxCellEditor extends CellEditor {

    private Button combo;

    public CustomCheckboxCellEditor(Composite parent) {
        this(parent, SWT.NONE);
    }

    public CustomCheckboxCellEditor(Composite parent, int style) {
        super(parent, style);
    }

    @Override
    protected Control createControl(Composite parent) {
        if (!RuntimeUtils.isPlatformWindows()) {
            // On non-Windows WM extra composite breaks inline editor
            combo = new Button(parent, SWT.CHECK);
            combo.setFont(parent.getFont());
            combo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    applyEditorValue();
                }
            });
            return combo;
        } else {
            Composite placeholder = new Composite(parent, SWT.NONE);
            GridLayout gl = new GridLayout(1, false);
            gl.verticalSpacing = 0;
            gl.horizontalSpacing = 0;
            gl.marginHeight = 0;
            gl.marginWidth = 0;
            placeholder.setLayout(gl);

            combo = new Button(placeholder, SWT.CHECK);
            final GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_CENTER);
            gd.verticalIndent = 1;
            gd.horizontalIndent = 4;
            combo.setLayoutData(gd);
            combo.setFont(parent.getFont());
            combo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    applyEditorValue();
                }
            });
            return placeholder;
        }

    }

    @Override
    protected Boolean doGetValue() {
        return combo.getSelection();
    }

    @Override
    protected void doSetFocus() {
        combo.setFocus();
        //combo.setSelection(!combo.getSelection());
//        combo.getDisplay().asyncExec(new Runnable() {
//            public void run()
//            {
//                applyEditorValue();
//            }
//        });
    }

    @Override
    protected void doSetValue(Object value) {
        Assert.isTrue(combo != null && (value instanceof Boolean));
        combo.setSelection(CommonUtils.toBoolean(value));
    }

    @Override
    public LayoutData getLayoutData() {
        LayoutData layoutData = super.getLayoutData();
        layoutData.grabHorizontal = true;
        layoutData.horizontalAlignment = SWT.CENTER;
        //layoutData.minimumWidth = 100;
        return layoutData;
    }

    void applyEditorValue() {
        // must set the selection before getting value
        Object newValue = doGetValue();
        markDirty();
        boolean isValid = isCorrect(newValue);
        setValueValid(isValid);

        fireApplyEditorValue();
    }

    protected int getDoubleClickTimeout() {
        return 0;
    }
}
