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

import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.ui.contentassist.ContentAssistUtils;
import org.jkiss.dbeaver.ui.contentassist.StringContentProposalProvider;
import org.jkiss.dbeaver.ui.contentassist.StyledTextContentAdapter;
import org.jkiss.utils.CommonUtils;

/**
 * A smart combo editor with string values.
 */
public class SmartComboBoxCellEditor extends CellEditor {
    private CSmartCombo<String> comboBox;
    private StringContentProposalProvider proposalProvider;
    private ContentProposalAdapter proposalAdapter;

    public SmartComboBoxCellEditor(@NotNull Composite parent, @NotNull String[] items) {
        this(parent, items, SWT.READ_ONLY);
    }

    public SmartComboBoxCellEditor(@NotNull Composite parent, @NotNull String[] items, int style) {
        super(parent, style);
        setItems(items);
    }

    public SmartComboBoxCellEditor(
        @NotNull ColumnViewer columnViewer,
        @NotNull Composite parent,
        @NotNull String[] items,
        int style
    ) {
        this(parent, items, style);
        comboBox.addListener(SWT.Selection, event -> columnViewer.applyEditorValue());
    }

    public void setItems(@NotNull String[] items) {
        comboBox.removeAll();
        for (String item : items) {
            comboBox.addItem(item);
        }
        comboBox.select(-1);
        if (proposalProvider != null) {
            proposalProvider.setProposals(items);
        }
    }

    @NotNull
    @Override
    protected Control createControl(@NotNull Composite parent) {
        comboBox = new CSmartCombo<>(parent, getStyle(), new LabelProvider() {
            @NotNull
            @Override
            public String getText(@Nullable Object element) {
                return CommonUtils.toString(element);
            }
        }) {
            @Override
            protected void textEvent(@NotNull Event event) {
                // Content assist must handle navigation and acceptance before the combo editor.
                if (proposalAdapter != null && proposalAdapter.isProposalPopupOpen() &&
                    (event.type == SWT.KeyDown || event.type == SWT.Traverse)) {
                    return;
                }
                super.textEvent(event);
            }
        };
        comboBox.setFont(parent.getFont());
        comboBox.setVisibleItemCount(15);
        if ((getStyle() & SWT.READ_ONLY) == 0) {
            comboBox.setEditable(true);
            proposalProvider = new StringContentProposalProvider(true, new String[0]);
            proposalAdapter = ContentAssistUtils.installContentProposal(
                comboBox.getTextControl(),
                new StyledTextContentAdapter(),
                proposalProvider,
                null,
                true,
                (getStyle() & SWT.DROP_DOWN) == 0);
        }
        comboBox.addListener(SWT.Selection, event -> valueChanged(isValueValid(), isCorrect(doGetValue())));
        comboBox.addListener(SWT.DefaultSelection, event -> applyEditorValueAndDeactivate());
        comboBox.addListener(SWT.FocusOut, event -> focusLost());
        comboBox.addTraverseListener(event -> {
            if (event.detail == SWT.TRAVERSE_ESCAPE) {
                event.doit = false;
                fireCancelEditor();
            } else if (event.detail == SWT.TRAVERSE_RETURN) {
                event.doit = false;
            }
        });
        return comboBox;
    }

    @Nullable
    @Override
    protected Object doGetValue() {
        return comboBox == null || comboBox.isDisposed() ? null : comboBox.getText();
    }

    @Override
    protected void doSetValue(@Nullable Object value) {
        String text;
        if (value instanceof DBPNamedObject namedObject) {
            text = namedObject.getName();
        } else if (value instanceof Enum<?> enumValue) {
            text = enumValue.name();
        } else {
            text = CommonUtils.toString(value);
        }
        comboBox.select(-1);
        comboBox.setText(text);
    }

    @Override
    protected void doSetFocus() {
        comboBox.setFocus();
        if (comboBox.getTextControl().getEditable()) {
            comboBox.getTextControl().selectAll();
        }
    }

    @Override
    protected void focusLost() {
        Control focus = comboBox.getDisplay().getFocusControl();
        if (isActivated() && focus != null && focus.getShell() == comboBox.getShell()) {
            applyEditorValueAndDeactivate();
        }
    }

    @Override
    public void deactivate() {
        if (comboBox != null && !comboBox.isDisposed()) {
            comboBox.dropDown(false);
        }
        super.deactivate();
    }

    private void applyEditorValueAndDeactivate() {
        markDirty();
        setValueValid(isCorrect(doGetValue()));
        fireApplyEditorValue();
        deactivate();
    }

    @Override
    protected boolean dependsOnExternalFocusListener() {
        return false;
    }

    @Override
    protected int getDoubleClickTimeout() {
        return 0;
    }
}
