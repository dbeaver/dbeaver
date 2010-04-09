package org.jkiss.dbeaver.ui.dialogs.data;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ui.controls.grid.IGridRowData;
import org.jkiss.dbeaver.model.dbc.DBCColumnMetaData;

/**
 * TextViewDialog
 */
public class TextViewDialog extends ValueViewDialog {

    private Object data;

    public TextViewDialog(Shell shell, IGridRowData row, DBCColumnMetaData columnInfo, Object data) {
        super(shell, row, columnInfo);
        this.data = data;
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite dialogGroup = (Composite)super.createDialogArea(parent);

        Label label = new Label(dialogGroup, SWT.NONE);
        label.setText("Value: ");

        Text text = new Text(dialogGroup, SWT.BORDER | SWT.MULTI | SWT.READ_ONLY);

        text.setText(data == null ? "[NULL]" : data.toString());
        text.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_WHITE));
        
        GridData ld = new GridData(GridData.FILL_BOTH);
        ld.heightHint = 200;
        ld.widthHint = 300;
        text.setLayoutData(ld);

        return dialogGroup;
    }

}
