/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jkiss.dbeaver.ui.dialogs.tools;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
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
        setShellStyle(SWT.CLOSE | SWT.MAX | SWT.MIN | SWT.TITLE | SWT.BORDER | SWT.RESIZE | getDefaultOrientation());
        setHelpAvailable(false);
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
