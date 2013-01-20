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

package org.jkiss.dbeaver.ext.mysql.data;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.ui.dialogs.data.ValueViewDialog;
import org.jkiss.utils.CommonUtils;

/**
 * Enum editor dialog
 */
public class EnumViewDialog extends ValueViewDialog {

    private org.eclipse.swt.widgets.List enumEdit;

    protected EnumViewDialog(DBDValueController valueController)
    {
        super(valueController);
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite dialogGroup = (Composite)super.createDialogArea(parent);

        MySQLTypeEnum value = (MySQLTypeEnum) getValueController().getValue();
        boolean isForeignKey = super.isForeignKey();

        Label label = new Label(dialogGroup, SWT.NONE);
        label.setText("Value: ");

        int style = SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL;
        if (value.getColumn().getTypeName().equalsIgnoreCase(MySQLConstants.TYPE_NAME_SET)) {
            style = style | SWT.MULTI;
        }
        enumEdit = new List(dialogGroup, style);

        MySQLSetValueHandler.fillSetList(enumEdit, value);

        enumEdit.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_WHITE));
        GridData gd = new GridData(isForeignKey ? GridData.FILL_HORIZONTAL : GridData.FILL_BOTH);
        gd.widthHint = 300;
        if (!isForeignKey) {
            gd.heightHint = 200;
            gd.grabExcessVerticalSpace = true;
        }
        enumEdit.setLayoutData(gd);
        enumEdit.setFocus();
        enumEdit.setEnabled(!getValueController().isReadOnly());

        return dialogGroup;
    }

    @Override
    protected Object getEditorValue()
    {
        MySQLTypeEnum value = (MySQLTypeEnum) getValueController().getValue();
        String[] selection = enumEdit.getSelection();
        StringBuilder resultString = new StringBuilder();
        for (String selString : selection) {
            if (CommonUtils.isEmpty(selString)) {
                continue;
            }
            if (resultString.length() > 0) resultString.append(',');
            resultString.append(selString);
        }
        return new MySQLTypeEnum(value.getColumn(), resultString.toString());
    }

    @Override
    public void refreshValue()
    {
        MySQLSetValueHandler.fillSetList(enumEdit, (MySQLTypeEnum) getValueController().getValue());
    }
}
