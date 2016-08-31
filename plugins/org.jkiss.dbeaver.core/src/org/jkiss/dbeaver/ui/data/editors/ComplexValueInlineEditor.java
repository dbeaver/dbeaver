/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.data.editors;

import org.eclipse.jface.action.IContributionManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDComplexValue;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.dialogs.data.ComplexObjectEditor;

/**
* ComplexValueInlineEditor
*/
public class ComplexValueInlineEditor extends BaseValueEditor<Tree> {
    private final IValueController controller;
    ComplexObjectEditor editor;

    public ComplexValueInlineEditor(IValueController controller) {
        super(controller);
        this.controller = controller;
    }

    @Override
    public void primeEditorValue(@Nullable Object value) throws DBException
    {
        editor.setModel(controller.getExecutionContext(), (DBDComplexValue) value);
    }

    @Override
    protected Tree createControl(Composite editPlaceholder)
    {
        final boolean inline = valueController.getEditType() == IValueController.EditType.INLINE;
        editor = new ComplexObjectEditor(controller, this, inline ? SWT.NONE : SWT.BORDER);
        editor.setModel(controller.getExecutionContext(), (DBDComplexValue) controller.getValue());
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
