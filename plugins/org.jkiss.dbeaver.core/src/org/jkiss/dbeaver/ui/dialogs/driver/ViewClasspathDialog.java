/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs.driver;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.core.CoreMessages;

import java.util.StringTokenizer;

/**
 * EditDriverDialog
 */
public class ViewClasspathDialog extends Dialog
{
    public ViewClasspathDialog(Shell shell)
    {
        super(shell);
    }

    protected boolean isResizable()
    {
        return true;
    }

    protected Control createDialogArea(Composite parent)
    {
        getShell().setText(CoreMessages.dialog_view_classpath_title);

        Composite group = (Composite) super.createDialogArea(parent);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 300;
        gd.widthHint = 400;
        group.setLayoutData(gd);

        {
            ListViewer libsTable = new ListViewer(group, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
            libsTable.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));

            String classPath = System.getProperty("java.class.path"); //$NON-NLS-1$
            StringTokenizer st = new StringTokenizer(classPath, ";"); //$NON-NLS-1$
            while (st.hasMoreTokens()) {
                libsTable.getList().add(st.nextToken());
            }
/*
            for (DriverLibraryDescriptor lib : driver.getLibraries()) {
                libsTable.getList().add(lib.getFile().getPath());
            }
*/
        }
        return group;
    }

    protected void createButtonsForButtonBar(Composite parent)
    {
        createButton(
            parent,
            IDialogConstants.OK_ID,
            IDialogConstants.OK_LABEL,
            true);
    }

}