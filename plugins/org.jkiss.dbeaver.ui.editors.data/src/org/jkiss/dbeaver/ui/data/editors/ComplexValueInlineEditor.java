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
package org.jkiss.dbeaver.ui.data.editors;

import org.eclipse.jface.action.IContributionManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetRow;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetValueController;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.dialogs.ComplexObjectEditor;

/**
* ComplexValueInlineEditor
*/
public class ComplexValueInlineEditor extends BaseValueEditor<Tree> {
    private final IValueController controller;
    protected ComplexObjectEditor editor;

    public ComplexValueInlineEditor(IValueController controller) {
        super(controller);
        this.controller = controller;
    }

    @Override
    public void primeEditorValue(@Nullable Object value) throws DBException
    {
        editor.setModel(controller.getExecutionContext(), value, getOriginalValue(), isChangeHighlighted());
    }

    @Override
    protected Tree createControl(Composite editPlaceholder)
    {
        final boolean isDialog = valueController.getEditType() == IValueController.EditType.EDITOR;
        editor = new ComplexObjectEditor(controller, this, isDialog ? SWT.BORDER : SWT.NONE);

        editor.setModel(controller.getExecutionContext(), controller.getValue(), getOriginalValue(), isChangeHighlighted());
        return editor.getTree();
    }

    @Override
    public Object extractEditorValue()
    {
        return editor.extractValue();
    }

    /**
     * Returns the original (committed) value of the edited cell.
     * It is used to highlight pending changes in the structure editor while the value is not committed.
     */
    @Nullable
    private Object getOriginalValue() {
        ResultSetValueController valueController = getRowValueController();
        if (valueController == null) {
            return null;
        }
        ResultSetRow row = valueController.getCurRow();
        if (row == null) {
            return null;
        }
        final DBDAttributeBinding topAttribute = valueController.getBinding().getTopParent();
        if (!row.isChanged(topAttribute)) {
            return null;
        }
        return row.getChange(topAttribute);
    }

    /**
     * Returns true if pending changes highlighting should be shown in the structure editor
     * because the edited value differs from the committed one.
     */
    private boolean isChangeHighlighted() {
        ResultSetValueController valueController = getRowValueController();
        if (valueController == null) {
            return false;
        }
        ResultSetRow row = valueController.getCurRow();
        return row != null && row.isChanged(valueController.getBinding().getTopParent());
    }

    /**
     * Returns the row value controller if the value can be compared with the committed one
     * (i.e. it is a whole cell value, not a nested value path).
     */
    @Nullable
    private ResultSetValueController getRowValueController() {
        if (controller instanceof ResultSetValueController valueController && valueController.getValuePath() == null) {
            return valueController;
        }
        return null;
    }

    @Override
    public void contributeActions(@NotNull IContributionManager manager, @NotNull IValueController controller) throws DBCException {
        editor.contributeActions(manager);
    }
}
