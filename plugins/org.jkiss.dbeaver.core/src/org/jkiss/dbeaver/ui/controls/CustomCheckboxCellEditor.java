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
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ui.ImageUtils;
import org.jkiss.utils.CommonUtils;

/**
 * Checkbox cell editor
 */
public class CustomCheckboxCellEditor extends CellEditor {

    private Label checkBox;
    private boolean checked;

    public CustomCheckboxCellEditor(Composite parent) {
        this(parent, SWT.NONE);
    }

    public CustomCheckboxCellEditor(Composite parent, int style) {
        super(parent, style);
    }

    @Override
    protected Control createControl(Composite parent) {
        checkBox = new Label(parent, SWT.NONE);
        setCheckIcon();
        checkBox.setFont(parent.getFont());

        checkBox.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                CustomCheckboxCellEditor.this.focusLost();
            }
        });

        return checkBox;
    }

    private void setCheckIcon() {
        Image image = checked ? ImageUtils.getImageCheckboxEnabledOn() : ImageUtils.getImageCheckboxEnabledOff();
        checkBox.setImage(image);
    }

    @Override
    protected Boolean doGetValue() {
        return checked;
    }

    @Override
    protected void doSetFocus() {
        checkBox.setFocus();
    }

    @Override
    protected void doSetValue(Object value) {
        Assert.isTrue(checkBox != null && (value instanceof Boolean));
        checked = CommonUtils.toBoolean(value);
        setCheckIcon();
    }

    @Override
    public LayoutData getLayoutData() {
        LayoutData layoutData = super.getLayoutData();
        if ((getStyle() & SWT.LEFT) == SWT.LEFT) {
            layoutData.grabHorizontal = false;
            layoutData.horizontalAlignment = SWT.LEFT;
        } else {
            layoutData.grabHorizontal = false;
            layoutData.horizontalAlignment = SWT.CENTER;
        }
        return layoutData;
    }

    void applyEditorValue() {
        // must set the selection before getting value
        Object newValue = doGetValue();
        markDirty();
        boolean isValid = isCorrect(newValue);
        setValueValid(isValid);
        setCheckIcon();

        //fireApplyEditorValue();
    }

    protected int getDoubleClickTimeout() {
        return 0;
    }

    @Override
    public void activate() {
        checkBox.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent e) {
                checked = !checked;
                setCheckIcon();
                applyEditorValue();
                fireApplyEditorValue();
            }
        });

        checked = !checked;
        setCheckIcon();
        applyEditorValue();
        // Run in async to avoid NPE. fireApplyEditorValue disposes and nullifies editor
        DBeaverUI.asyncExec(this::fireApplyEditorValue);
    }

    @Override
    public void activate(ColumnViewerEditorActivationEvent activationEvent) {
        if (activationEvent.eventType != ColumnViewerEditorActivationEvent.TRAVERSAL) {
            super.activate(activationEvent);
        }
    }

}
