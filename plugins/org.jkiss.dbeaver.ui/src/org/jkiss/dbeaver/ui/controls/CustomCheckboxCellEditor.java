/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.controls.bool.BooleanMode;
import org.jkiss.dbeaver.ui.controls.bool.BooleanStyle;
import org.jkiss.dbeaver.ui.controls.bool.BooleanStyleSet;
import org.jkiss.dbeaver.ui.controls.bool.BooleanStyleDecorator;
import org.jkiss.utils.CommonUtils;

/**
 * Checkbox cell editor
 */
public class CustomCheckboxCellEditor extends CellEditor implements BooleanStyleDecorator {

    private static final boolean CHANGE_ON_ACTIVATE = false;

    private Label checkBox;
    private boolean initialValue;
    private boolean checked;
    private BooleanStyleSet booleanStyles;
    private UIElementAlignment alignment;

    public CustomCheckboxCellEditor(Composite parent) {
        super(parent);

        final IPropertyChangeListener styleChangeListener = event -> {
            booleanStyles = BooleanStyleSet.getDefaultStyles(DBWorkbench.getPlatform().getPreferenceStore());
        };

        BooleanStyleSet.installStyleChangeListener(parent, styleChangeListener);
        styleChangeListener.propertyChange(null);
    }

    @Override
    protected Control createControl(Composite parent) {
        Composite ph = new Composite(parent, SWT.NONE);
        GridLayout gl = new GridLayout(1, false);
        gl.marginWidth = 0;
        gl.marginHeight = 0;
        ph.setLayout(gl);

        ph.setBackground(parent.getBackground());
        checkBox = new Label(ph, SWT.NONE);
        checkBox.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, true));
        checkBox.setBackground(ph.getBackground());

        ph.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                CustomCheckboxCellEditor.this.focusLost();
            }
        });
        ph.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.character) {
                    case SWT.SPACE:
                        checked = !checked;
                        updateCheckVisuals();
                        applyEditorValue();
                        break;
                    case SWT.ESC:
                        checked = initialValue;
                        // fallthrough
                    case SWT.CR:
                        applyEditorValue();
                        fireApplyEditorValue();
                        break;
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {

            }
        });
        checkBox.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent e) {
                checked = !checked;
                updateCheckVisuals();
                applyEditorValue();
                //fireApplyEditorValue();
            }
        });

        return ph;
    }

    private void updateCheckVisuals() {
        final BooleanStyle style = booleanStyles.getStyle(checked);

        if (style.getMode() == BooleanMode.TEXT) {
            checkBox.setText(style.getText());
            checkBox.setForeground(UIUtils.getSharedColor(style.getColor()));
        } else {
            checkBox.setImage(DBeaverIcons.getImage(style.getIcon()));
        }

        final UIElementAlignment alignment = this.alignment == null ? style.getAlignment() : this.alignment;
        ((GridData) checkBox.getLayoutData()).horizontalAlignment = alignment.getStyle();
    }

    @Override
    protected Boolean doGetValue() {
        return checked;
    }

    @Override
    protected void doSetFocus() {
        checkBox.getParent().setFocus();
    }

    @Override
    protected void doSetValue(Object value) {
        Assert.isTrue(checkBox != null && (value instanceof Boolean));
        boolean val = CommonUtils.toBoolean(value);
        checked = val;
        initialValue = val;
        //setCheckIcon();
    }

    @Override
    public LayoutData getLayoutData() {
        LayoutData layoutData = super.getLayoutData();
        layoutData.grabHorizontal = true;
        layoutData.horizontalAlignment = SWT.CENTER;
        return layoutData;
    }

    private void applyEditorValue() {
        // must set the selection before getting value
        Object newValue = doGetValue();
        markDirty();
        boolean isValid = isCorrect(newValue);
        setValueValid(isValid);
        updateCheckVisuals();

        //fireApplyEditorValue();
    }

    protected int getDoubleClickTimeout() {
        return 0;
    }

    @Override
    public void activate() {
        if (CHANGE_ON_ACTIVATE) {
            checked = !checked;
        }
        updateCheckVisuals();
        if (CHANGE_ON_ACTIVATE) {
            applyEditorValue();
            // Run in async to avoid NPE. fireApplyEditorValue disposes and nullifies editor
            UIUtils.asyncExec(() -> {
                fireApplyEditorValue();
                dispose();
            });
        }
    }

    @Override
    public void activate(ColumnViewerEditorActivationEvent activationEvent) {
        /*if (activationEvent.eventType != ColumnViewerEditorActivationEvent.TRAVERSAL) */{
            super.activate(activationEvent);
        }
    }

    @Override
    public void setBooleanAlignment(@NotNull UIElementAlignment alignment) {
        this.alignment = alignment;
    }
}
