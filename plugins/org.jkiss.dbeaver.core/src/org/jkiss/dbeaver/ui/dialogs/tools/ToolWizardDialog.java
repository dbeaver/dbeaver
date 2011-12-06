/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs.tools;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardDialog;

/**
 * Tooo wizard dialog
 */
public class ToolWizardDialog extends ActiveWizardDialog
{
    public ToolWizardDialog(IWorkbenchWindow window, IWizard wizard)
    {
        super(window, wizard);
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        super.createButtonsForButtonBar(parent);
        Button cancelButton = getButton(IDialogConstants.CANCEL_ID);
        cancelButton.setText(IDialogConstants.CLOSE_LABEL);
        Button finishButton = getButton(IDialogConstants.FINISH_ID);
        finishButton.setText("Start");
    }
}
