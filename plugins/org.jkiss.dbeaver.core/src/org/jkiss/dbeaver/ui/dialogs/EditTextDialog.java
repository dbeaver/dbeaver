/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class EditTextDialog extends Dialog {

    private String title;
    private String text;
    private Text textControl;
    private int textWidth = 300;
    private int textHeight = 200;
    private Image image;

    public EditTextDialog(Shell parentShell, String title, String text)
    {
        super(parentShell);
        this.title = title;
        this.text = text;
    }

    public void setImage(Image image)
    {
        this.image = image;
    }

    protected boolean isResizable() {
    	return true;
    }

    protected Control createDialogArea(Composite parent)
    {
        getShell().setText(title);
        if (image != null) {
            getShell().setImage(image);
        }

        Composite composite = (Composite) super.createDialogArea(parent);

        textControl = new Text(composite, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
        textControl.setText(text);
        textControl.setEditable(true);
        GridData gd = new GridData(GridData.FILL_BOTH);
        if (textWidth > 0) {
            gd.widthHint = textWidth;
        }
        if (textHeight > 0) {
            gd.heightHint = textHeight;
        }
        gd.minimumHeight = 100;
        gd.minimumWidth = 100;
        textControl.setLayoutData(gd);

        return parent;
    }

    @Override
    protected void okPressed()
    {
        text = textControl.getText();
        super.okPressed();
    }

    public static String editText(Shell parentShell, String title, String text)
    {
        EditTextDialog dialog = new EditTextDialog(parentShell, title, text);
        if (dialog.open() == IDialogConstants.OK_ID) {
            return dialog.text;
        } else {
            return null;
        }
    }

}
