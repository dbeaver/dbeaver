/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.editors.object.struct;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.ui.UIUtils;

class EditObjectDialog extends TrayDialog {

    private final IDialogPage dialogPage;

    public EditObjectDialog(Shell shell, IDialogPage dialogPage)
    {
        super(shell);
        this.dialogPage = dialogPage;
        if (this.dialogPage instanceof BaseObjectEditPage) {
            ((BaseObjectEditPage) this.dialogPage).setContainer(this);
        }
        //setHelpAvailable(false);
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings() {
        String dialogId = "DBeaver.EditObjectDialog." + dialogPage.getClass().getSimpleName();
        return UIUtils.getDialogSettings(dialogId);
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        getShell().setText(dialogPage.getTitle());
        Composite group = (Composite) super.createDialogArea(parent);
        GridData gd = new GridData(GridData.FILL_BOTH);
        group.setLayoutData(gd);

        dialogPage.createControl(group);

        return group;
    }

    @Override
    protected Control createContents(Composite parent) {
        Control contents = super.createContents(parent);
        updateButtons();
        return contents;
    }

    @Override
    protected void okPressed() {
        if (dialogPage instanceof BaseObjectEditPage) {
            try {
                ((BaseObjectEditPage) dialogPage).performFinish();
            } catch (Exception e) {
                DBUserInterface.getInstance().showError("Error saving data", null, e);
                return;
            }
        }
        super.okPressed();
    }

    void updateButtons() {
        boolean enabled = false;
        if (dialogPage instanceof BaseObjectEditPage) {
            enabled = ((BaseObjectEditPage) dialogPage).isPageComplete();
        }
        getButton(IDialogConstants.OK_ID).setEnabled(enabled);
    }

    public static boolean showDialog(IDialogPage dialogPage) {
        return showDialog(UIUtils.getActiveWorkbenchShell(), dialogPage);
    }

    public static boolean showDialog(Shell shell, IDialogPage dialogPage) {
        EditObjectDialog dialog = new EditObjectDialog(shell, dialogPage);
        return dialog.open() == IDialogConstants.OK_ID;
    }

}
