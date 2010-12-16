/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs.search;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.ui.DBIcon;

public class FindObjectsDialog extends Dialog {

    private DBPDataSource dataSource;
    private Text textControl;
    private Image image;

    public FindObjectsDialog(Shell parentShell, DBPDataSource dataSource)
    {
        super(parentShell);
        this.dataSource = dataSource;
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
        getShell().setText("Find database objects");
        getShell().setImage(DBIcon.FIND.getImage());

        Composite composite = (Composite) super.createDialogArea(parent);

        textControl = new Text(composite, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
        textControl.setText("");
        textControl.setEditable(false);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.minimumHeight = 200;
        gd.minimumWidth = 300;
        textControl.setLayoutData(gd);

        return parent;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.CLOSE_LABEL, true);
    }

}
