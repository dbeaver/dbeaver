/*
 * Copyright (C) 2010-2015 Serge Rieder serge@jkiss.org
 * Copyright (C) 2011-2012 Eugene Fradkin eugene.fradkin@gmail.com
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

package org.jkiss.dbeaver.ui.dialogs.tools;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardDialog;

/**
 * Tool wizard dialog
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
        finishButton.setText(CoreMessages.tools_wizard_dialog_button_start);
    }
}
