/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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

import org.eclipse.jface.fieldassist.ComboContentAdapter;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.ui.contentassist.ContentAssistUtils;
import org.jkiss.dbeaver.ui.contentassist.StringContentProposalProvider;
import org.jkiss.utils.CommonUtils;

import java.text.MessageFormat;
import java.util.Objects;

/**
 * Custom combo editor
 */
public class CustomComboBoxCellEditor extends CellEditor {
    public static final int DROP_DOWN_ON_MOUSE_ACTIVATION = 1;
    public static final int DROP_DOWN_ON_KEY_ACTIVATION = 1 << 1;
    public static final int DROP_DOWN_ON_PROGRAMMATIC_ACTIVATION = 1 << 2;
    public static final int DROP_DOWN_ON_TRAVERSE_ACTIVATION = 1 << 3;

    private String[] items;
    private Combo comboBox;
    private StringContentProposalProvider proposalProvider;
    private int activationStyle;

    public CustomComboBoxCellEditor(Composite parent, String[] items) {
        this(parent, items, SWT.NONE);
    }

    public CustomComboBoxCellEditor(Composite parent, String[] items, int style) {
        super(parent, style);
        setItems(items);
    }

    public CustomComboBoxCellEditor(ColumnViewer columnViewer, Composite parent, String[] items, int style) {
        this(parent, items, style);
        init(columnViewer);
    }

    private void init(ColumnViewer columnViewer) {
        comboBox.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> columnViewer.applyEditorValue()));
    }

    /**
     * Sets the list of choices for the combo box
     *
     * @param items the list of choices for the combo box
     */
    public void setItems(String[] items) {
        this.items = Objects.requireNonNull(items);
        if (comboBox != null && !comboBox.isDisposed()) {
            comboBox.setItems(items);
            setValueValid(true);
        }
        if (proposalProvider != null) {
            proposalProvider.setProposals(items);
        }
    }

    public String[] getItems() {
        return items;
    }

    @Override
    protected Control createControl(Composite parent) {
        comboBox = new Combo(parent, getStyle());
        comboBox.setVisibleItemCount(15);
        comboBox.setFont(parent.getFont());
        comboBox.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        comboBox.addKeyListener(KeyListener.keyPressedAdapter(this::keyReleaseOccured));
        comboBox.addSelectionListener(SelectionListener.widgetDefaultSelectedAdapter(e -> applyEditorValueAndDeactivate()));
        comboBox.addTraverseListener(e -> {
            if (e.detail == SWT.TRAVERSE_ESCAPE || e.detail == SWT.TRAVERSE_RETURN) {
                e.doit = false;
            }
        });
        comboBox.addFocusListener(FocusListener.focusLostAdapter(e -> focusLost()));

        if ((getStyle() & SWT.READ_ONLY) == 0) {
            // Install proposal provider for editable combos
            // In fact it was a bad idea to use proposals in inline combo editors (#2409)
            proposalProvider = new StringContentProposalProvider(true, new String[0]);
            ContentAssistUtils.installContentProposal(comboBox, new ComboContentAdapter(), proposalProvider, (getStyle() & SWT.DROP_DOWN) == 0);
        }

        return comboBox;
    }

    @Override
    protected Object doGetValue() {
        if (comboBox == null || comboBox.isDisposed()) {
            return null;
        }
        return comboBox.getText();
    }

    @Override
    protected void doSetValue(Object value) {
        if (comboBox == null || comboBox.isDisposed()) {
            return;
        }
        if (value == null) {
            comboBox.setText("");
        } else {
            if (value instanceof DBPNamedObject) {
                comboBox.setText(((DBPNamedObject) value).getName());
            } else if (value instanceof Enum) {
                comboBox.setText(((Enum) value).name());
            } else {
                comboBox.setText(CommonUtils.toString(value));
            }
        }
    }

    @Override
    protected void doSetFocus() {
        comboBox.setFocus();
    }

    @Override
    public LayoutData getLayoutData() {
        LayoutData layoutData = super.getLayoutData();
        if (comboBox == null || comboBox.isDisposed()) {
            layoutData.minimumWidth = 60;
        } else {
            GC gc = new GC(comboBox);
            try {
                layoutData.minimumWidth = (int) (gc.getFontMetrics().getAverageCharacterWidth() * 10 + 10);
            } finally {
                gc.dispose();
            }
        }
        return layoutData;
    }

    private void applyEditorValueAndDeactivate() {
        Object value = doGetValue();
        markDirty();
        boolean valid = isCorrect(value);
        setValueValid(valid);
        if (!valid) {
            setErrorMessage(MessageFormat.format(getErrorMessage(), value));
        }
        fireApplyEditorValue();
        deactivate();
    }

    @Override
    protected void keyReleaseOccured(KeyEvent event) {
        if (event.character == SWT.ESC) {
            fireCancelEditor();
        } else if (event.character == SWT.TAB) {
            applyEditorValueAndDeactivate();
        }
    }

    public void setActivationStyle(int activationStyle) {
        this.activationStyle = activationStyle;
    }

    @Override
    public void activate(ColumnViewerEditorActivationEvent event) {
        super.activate(event);
        boolean showList = switch (event.eventType) {
            case ColumnViewerEditorActivationEvent.MOUSE_CLICK_SELECTION,
                 ColumnViewerEditorActivationEvent.MOUSE_DOUBLE_CLICK_SELECTION ->
                (activationStyle & DROP_DOWN_ON_MOUSE_ACTIVATION) != 0;
            case ColumnViewerEditorActivationEvent.KEY_PRESSED ->
                (activationStyle & DROP_DOWN_ON_KEY_ACTIVATION) != 0;
            case ColumnViewerEditorActivationEvent.PROGRAMMATIC ->
                (activationStyle & DROP_DOWN_ON_PROGRAMMATIC_ACTIVATION) != 0;
            case ColumnViewerEditorActivationEvent.TRAVERSAL ->
                (activationStyle & DROP_DOWN_ON_TRAVERSE_ACTIVATION) != 0;
            default -> false;
        };
        if (showList) {
            comboBox.getDisplay().asyncExec(() -> {
                if (!comboBox.isDisposed()) {
                    comboBox.setListVisible(true);
                }
            });
        }
    }

    protected int getDoubleClickTimeout() {
        return 0;
    }

    @Override
    protected boolean dependsOnExternalFocusListener() {
        return false;
    }

    @Override
    protected void focusLost() {
        Control newFocus = getControl().getDisplay().getFocusControl();
        if (newFocus == null) {
            return;
        }
        if (newFocus.getShell() != getControl().getShell()) {
            // It is probably content assist popup - do no close editor
            return;
        }
        if (isActivated()) {
            applyEditorValueAndDeactivate();
        }
    }

}
