package org.jkiss.dbeaver.ui.dialogs.data;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.model.data.DBDValueController;

/**
 * TextViewDialog
 */
public class TextViewDialog extends ValueViewDialog {

    public TextViewDialog(DBDValueController valueController) {
        super(valueController);
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite dialogGroup = (Composite)super.createDialogArea(parent);

        Label label = new Label(dialogGroup, SWT.NONE);
        label.setText("Value: ");

        int style = SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL;
        if (getValueController().isReadOnly()) {
            style |= SWT.READ_ONLY;
        }
        Text text = new Text(dialogGroup, style);

        Object value = getValueController().getValue();
        text.setText(value == null ? "" : value.toString());
        text.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_WHITE));
        
        GridData ld = new GridData(GridData.FILL_BOTH);
        ld.heightHint = 200;
        ld.widthHint = 300;
        text.setLayoutData(ld);

        return dialogGroup;
    }

}
