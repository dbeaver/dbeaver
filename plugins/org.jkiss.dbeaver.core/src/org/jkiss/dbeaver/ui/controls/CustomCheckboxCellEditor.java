/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
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
        Composite placeholder = new Composite(parent, SWT.NONE);
        placeholder.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        GridLayout gl = new GridLayout(1, false);
        gl.verticalSpacing = 0;
        gl.horizontalSpacing = 0;
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        placeholder.setLayout(gl);

        checkbox = new Button(placeholder, SWT.CHECK);
        final GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.horizontalIndent = 4;
        gd.grabExcessHorizontalSpace = true;
        checkbox.setLayoutData(gd);
        checkbox.setFont(parent.getFont());
        checkbox.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e)
            {
                applyEditorValue();
            }
        });
//        checkbox.addSelectionListener(new SelectionAdapter() {
//            public void widgetDefaultSelected(SelectionEvent event) {
//                applyEditorValue();
//            }
//            public void widgetSelected(SelectionEvent event) {
//                applyEditorValue();
//            }
//        });


        return placeholder;
    }

    protected Object doGetValue() {
        return checkbox.getSelection();
    }

    protected void doSetFocus() {
        checkbox.setFocus();
        checkbox.setSelection(!checkbox.getSelection());
//        checkbox.getDisplay().asyncExec(new Runnable() {
//            public void run()
//            {
//                applyEditorValue();
//            }
//        });
    }

    protected void doSetValue(Object value) {
        Assert.isTrue(checkbox != null && (value instanceof Boolean));
        checkbox.setSelection((Boolean)value);
    }

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

}
