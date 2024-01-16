/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.IHelpContextIdProvider;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseTitleDialog;
import org.jkiss.dbeaver.ui.dialogs.IDialogPageContainer;
import org.jkiss.utils.CommonUtils;

class EditObjectDialog extends BaseTitleDialog implements IDialogPageContainer {

    private final IDialogPage dialogPage;

    public EditObjectDialog(Shell shell, IDialogPage dialogPage) {
        super(shell, null);
        this.dialogPage = dialogPage;
        if (this.dialogPage instanceof BaseObjectEditPage editPage) {
            editPage.setContainer(this);
        }
        if (dialogPage instanceof IHelpContextIdProvider hcp && hcp.getHelpContextId() != null) {
            setHelpAvailable(true);
        } else {
            setHelpAvailable(false);
        }
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings() {
        String dialogId = "DBeaver.EditObjectDialog." + dialogPage.getClass().getSimpleName();
        return UIUtils.getDialogSettings(dialogId);
    }

    @Override
    protected int getShellStyle() {
        return super.getShellStyle();
    }

    @Override
    protected Point getInitialSize() {
        Point proposedSize = super.getInitialSize();
        Point minSize = getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT);
        if (proposedSize.x < minSize.x) proposedSize.x = minSize.x;
        if (proposedSize.y < minSize.y) proposedSize.y = minSize.y;
        return proposedSize;
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    protected Composite createDialogArea(Composite parent)
    {
        String title = dialogPage.getTitle();
        if (dialogPage instanceof BaseObjectEditPage editPage) {
            DBSObject object = editPage.getObject();
            if (object != null) {
                title = DBUtils.getObjectTypeName(object);
            }
        }
        getShell().setText(title);
        setTitle(dialogPage.getTitle());
        Composite group = super.createDialogArea(parent);

        Composite composite = new Composite(group, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        dialogPage.createControl(composite);

        if (dialogPage instanceof IHelpContextIdProvider) {
            UIUtils.setHelp(dialogPage.getControl(), ((IHelpContextIdProvider) dialogPage).getHelpContextId());
        }

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
                DBWorkbench.getPlatformUI().showError("Error saving data", null, e);
                return;
            }
        }
        super.okPressed();
    }

    public void updateButtons() {
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

    public void updateMessage() {
        String errorMessage = dialogPage.getErrorMessage();
        setErrorMessage(errorMessage);
        Button okButton = getButton(IDialogConstants.OK_ID);
        if (okButton != null) {
            okButton.setEnabled(CommonUtils.isEmpty(errorMessage));
        }
    }
}
