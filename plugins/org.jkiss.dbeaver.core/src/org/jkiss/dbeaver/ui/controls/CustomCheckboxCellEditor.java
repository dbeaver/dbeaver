/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * Checkbox cell editor
 */
public class CustomCheckboxCellEditor extends CellEditor {

    private Button checkbox;

    public CustomCheckboxCellEditor() {
    }

    public CustomCheckboxCellEditor(Composite parent) {
        this(parent, SWT.NONE);
    }

    public CustomCheckboxCellEditor(Composite parent, int style) {
        super(parent, style);
    }

    protected Control createControl(Composite parent) {
        Composite placeholder = UIUtils.createPlaceholder(parent, 1);
        checkbox = new Button(placeholder, SWT.CHECK);
        final GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_CENTER | GridData.FILL_HORIZONTAL);
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalIndent = 20;
        checkbox.setLayoutData(gd);
        checkbox.setFont(parent.getFont());
        checkbox.addSelectionListener(new SelectionAdapter() {
            public void widgetDefaultSelected(SelectionEvent event) {
                applyEditorValue();
            }
            public void widgetSelected(SelectionEvent event) {
                applyEditorValue();
            }
        });


        return placeholder;
    }

    protected Object doGetValue() {
        return checkbox.getSelection();
    }

    protected void doSetFocus() {
        checkbox.setFocus();
    }

    protected void doSetValue(Object value) {
        Assert.isTrue(checkbox != null && (value instanceof Boolean));
        checkbox.setSelection((Boolean)value);
    }

    public LayoutData getLayoutData() {
        LayoutData layoutData = super.getLayoutData();
        layoutData.grabHorizontal = true;
        layoutData.horizontalAlignment = SWT.CENTER;
        layoutData.minimumWidth = 100;
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

}
