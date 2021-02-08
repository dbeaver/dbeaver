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
package org.jkiss.dbeaver.ui.data.editors;

import org.eclipse.jface.action.IContributionManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.DBCException;
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
        editor.setModel(controller.getExecutionContext(), value);
    }

    @Override
    protected Tree createControl(Composite editPlaceholder)
    {
        final boolean inline = valueController.getEditType() == IValueController.EditType.INLINE;
        editor = new ComplexObjectEditor(controller, this, inline ? SWT.NONE : SWT.BORDER);
        editor.setModel(controller.getExecutionContext(), controller.getValue());
        return editor.getTree();
    }

    @Override
    public Object extractEditorValue()
    {
        return editor.extractValue();
    }

    @Override
    public void contributeActions(@NotNull IContributionManager manager, @NotNull IValueController controller) throws DBCException {
        editor.contributeActions(manager);
    }
}
