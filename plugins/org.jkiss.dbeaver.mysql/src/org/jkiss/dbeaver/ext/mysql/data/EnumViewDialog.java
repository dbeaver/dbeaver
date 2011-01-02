/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.data;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.ui.dialogs.data.ValueViewDialog;

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

}
