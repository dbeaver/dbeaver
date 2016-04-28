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

package org.jkiss.dbeaver.ui.dialogs.data;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.IValueEditor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.data.editors.ReferenceValueEditor;

/**
 * Default value view dialog.
 * Uses panel editor inside of value viewer.
 */
public class DefaultValueViewDialog extends ValueViewDialog {

    private static final Log log = Log.getLog(DefaultValueViewDialog.class);

    private IValueEditor panelEditor;

    public DefaultValueViewDialog(IValueController valueController) {
        super(valueController);
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite dialogGroup = (Composite)super.createDialogArea(parent);

        Label label = new Label(dialogGroup, SWT.NONE);
        label.setText(CoreMessages.dialog_data_label_value);

        Composite editorPlaceholder = UIUtils.createPlaceholder(dialogGroup, 1);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.minimumHeight = label.computeSize(SWT.DEFAULT, SWT.DEFAULT).y + 4;
        editorPlaceholder.setLayoutData(gd);
        editorPlaceholder.setLayout(new FillLayout());

        try {
            panelEditor = createPanelEditor(editorPlaceholder);
            if (panelEditor == null) {
                return dialogGroup;
            }
            panelEditor.primeEditorValue(getValueController().getValue());
        } catch (DBException e) {
            log.error(e);
            return dialogGroup;
        }
        ReferenceValueEditor referenceValueEditor = new ReferenceValueEditor(getValueController(), this);
        if (referenceValueEditor.isReferenceValue()) {
            referenceValueEditor.createEditorSelector(dialogGroup);
        }

        return dialogGroup;
    }

    @Override
    public Object extractEditorValue()
        throws DBException
    {
        return panelEditor.extractEditorValue();
    }

    @Override
    public void primeEditorValue(@Nullable Object value) throws DBException
    {
        panelEditor.primeEditorValue(value);
    }

    @Override
    public Control getControl()
    {
        return panelEditor.getControl();
    }

}