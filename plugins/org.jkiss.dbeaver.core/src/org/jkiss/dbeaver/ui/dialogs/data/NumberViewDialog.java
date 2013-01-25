/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jkiss.dbeaver.ui.dialogs.data;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.data.DBDValueEditor;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * TextViewDialog
 */
public class NumberViewDialog extends ValueViewDialog {

    private DBDValueEditor panelEditor;

    public NumberViewDialog(DBDValueController valueController) {
        super(valueController);
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite dialogGroup = (Composite)super.createDialogArea(parent);

        Label label = new Label(dialogGroup, SWT.NONE);
        label.setText(CoreMessages.dialog_data_label_value);

        Composite editorPlaceholder = UIUtils.createPlaceholder(dialogGroup, 1);
        editorPlaceholder.setLayoutData(new GridData(GridData.FILL_BOTH));
        editorPlaceholder.setLayout(new FillLayout());

        try {
            panelEditor = createPanelEditor(editorPlaceholder);
            panelEditor.refreshValue();
        } catch (DBException e) {
            log.error(e);
            return dialogGroup;
        }
        if (super.isForeignKey() && panelEditor.getControl() instanceof Text) {
            super.createEditorSelector(dialogGroup, (Text)panelEditor.getControl());
        }

        return dialogGroup;
    }

    @Override
    protected Object getEditorValue()
    {
        try {
            return panelEditor.extractValue(VoidProgressMonitor.INSTANCE);
        } catch (DBException e) {
            // NEver be here
            log.error(e);
            return null;
        }
    }

    @Override
    public void refreshValue()
    {
        panelEditor.refreshValue();
    }
}