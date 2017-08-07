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

import org.eclipse.jface.fieldassist.SimpleContentProposalProvider;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

/**
 * Custom combo editor
 */
public class CustomComboBoxCellEditor extends ComboBoxCellEditor {

    private SimpleContentProposalProvider proposalProvider;

    public CustomComboBoxCellEditor(Composite parent, String[] items) {
        super(parent, items);
    }

    public CustomComboBoxCellEditor(Composite parent, String[] items, int style) {
        super(parent, items, style);
    }

    /**
     * Sets the list of choices for the combo box
     *
     * @param items the list of choices for the combo box
     */
    public void setItems(String[] items) {
        super.setItems(items);
        if (proposalProvider != null) {
            proposalProvider.setProposals(items);
        }
    }

    @Override
    protected Control createControl(Composite parent) {

        CCombo comboBox = (CCombo) super.createControl(parent);
        //comboBox.setEditable((getStyle() & SWT.READ_ONLY) == 0);
        comboBox.setVisibleItemCount(15);
        comboBox.setFont(parent.getFont());
        comboBox.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));

        if ((getStyle() & SWT.READ_ONLY) == 0) {
            // Install proposal provider for editable combos
            proposalProvider = new SimpleContentProposalProvider(comboBox.getItems());
            proposalProvider.setFiltering(true);
            UIUtils.installContentProposal(comboBox, new CComboContentAdapter(), proposalProvider, true, true);
        }

        return comboBox;
    }

    @Override
    protected Object doGetValue() {
        CCombo comboBox = (CCombo) getControl();
        if (comboBox == null || comboBox.isDisposed()) {
            return null;
        }
        return comboBox.getText();
    }

    @Override
    protected void doSetValue(Object value) {
        CCombo comboBox = (CCombo) getControl();
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

    protected int getDoubleClickTimeout() {
        return 0;
    }
}
