/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs.misc;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.internal.WorkbenchMessages;
import org.eclipse.ui.internal.IWorkbenchHelpContextIds;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.ui.DBIcon;

/**
 * About box
 */
public class AboutBoxDialog extends Dialog
{

    public AboutBoxDialog(Shell shell)
    {
        super(shell);
    }

    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("About DBeaver");
        PlatformUI.getWorkbench().getHelpSystem().setHelp(newShell,
				IWorkbenchHelpContextIds.ABOUT_DIALOG);
    }

    protected boolean isResizable()
    {
        return true;
    }

    protected Control createDialogArea(Composite parent)
    {
        Color background = JFaceColors.getBannerBackground(parent.getDisplay());
        Color foreground = JFaceColors.getBannerForeground(parent.getDisplay());
        parent.setBackground(background);
        parent.setForeground(foreground);

        Label imageLabel = new Label(parent, SWT.NONE);
        imageLabel.setBackground(background);
        //imageLabel.setForeground(foreground);

        GridData data = new GridData();
        data.verticalAlignment = GridData.BEGINNING;
        data.horizontalAlignment = GridData.CENTER;
        data.grabExcessHorizontalSpace = true;
        data.horizontalIndent = 20;
        data.verticalIndent = 20;
        imageLabel.setLayoutData(data);
        imageLabel.setImage(DBIcon.ABOUT.getImage());

        return parent;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        parent.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        createButton(
            parent,
            IDialogConstants.OK_ID, 
            IDialogConstants.OK_LABEL,
            true);
    }

}