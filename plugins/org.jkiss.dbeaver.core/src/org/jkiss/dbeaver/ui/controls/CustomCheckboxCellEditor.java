/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.utils.CommonUtils;

/**
 * Checkbox cell editor
 */
public class CustomCheckboxCellEditor extends CellEditor {

    private CCombo combo;

    public CustomCheckboxCellEditor(Composite parent) {
        this(parent, SWT.NONE);
    }

    public CustomCheckboxCellEditor(Composite parent, int style) {
        super(parent, style);
    }

    @Override
    protected Control createControl(Composite parent) {
        Composite placeholder = new Composite(parent, SWT.NONE);
        placeholder.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        GridLayout gl = new GridLayout(1, false);
        gl.verticalSpacing = 0;
        gl.horizontalSpacing = 0;
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        placeholder.setLayout(gl);

        combo = new CCombo(placeholder, SWT.DROP_DOWN | SWT.READ_ONLY);
        final GridData gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER);
//        gd.verticalIndent = 2;
//        gd.horizontalIndent = 4;
        gd.grabExcessHorizontalSpace = true;
        combo.add(DBConstants.BOOLEAN_PROP_NO);
        combo.add(DBConstants.BOOLEAN_PROP_YES);
        combo.setLayoutData(gd);
        combo.setFont(parent.getFont());
        combo.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                applyEditorValue();
            }
        });
//        combo.addSelectionListener(new SelectionAdapter() {
//            public void widgetDefaultSelected(SelectionEvent event) {
//                applyEditorValue();
//            }
//            public void widgetSelected(SelectionEvent event) {
//                applyEditorValue();
//            }
//        });


        return placeholder;
    }

    @Override
    protected Boolean doGetValue() {
        return combo.getSelectionIndex() > 0;
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
        combo.select(CommonUtils.toBoolean(value) ? 1 : 0);
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
