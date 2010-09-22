package org.jkiss.dbeaver.ui.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ui.UIUtils;

public class EnterNameDialog extends Dialog {

    private String propertyName;
    private Text propNameText;
    private String result;

    public EnterNameDialog(Shell parentShell, String propertyName)
    {
        super(parentShell);
        this.propertyName = propertyName;
    }

    public String getResult()
    {
        return result;
    }

    protected Control createDialogArea(Composite parent)
    {
        getShell().setText(propertyName);

        Composite propGroup = new Composite(parent, SWT.NONE);
        GridLayout gl = new GridLayout(1, false);
        gl.marginHeight = 10;
        gl.marginWidth = 10;
        propGroup.setLayout(gl);
        GridData gd = new GridData(GridData.FILL_BOTH);
        propGroup.setLayoutData(gd);

        propNameText = UIUtils.createLabelText(propGroup, propertyName, "");

        return parent;
    }

    protected void okPressed()
    {
        result = propNameText.getText();
        super.okPressed();
    }

    public String chooseName()
    {
        if (open() == IDialogConstants.OK_ID) {
            return result;
        } else {
            return null;
        }
    }

    public static String chooseName(Shell parentShell, String propertyName)
    {
        EnterNameDialog dialog = new EnterNameDialog(parentShell, propertyName);
        return dialog.chooseName();
    }
}
