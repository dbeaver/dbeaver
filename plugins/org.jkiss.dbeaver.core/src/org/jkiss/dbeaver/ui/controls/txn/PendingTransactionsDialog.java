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
package org.jkiss.dbeaver.ui.controls.txn;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IWorkbenchPart;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.ui.UIUtils;

public class PendingTransactionsDialog extends TransactionInfoDialog {

    private static final String DIALOG_ID = "DBeaver.PendingTransactionsDialog";//$NON-NLS-1$

    public PendingTransactionsDialog(Shell parentShell, IWorkbenchPart activePart) {
        super(parentShell, activePart);
    }

    @Override
    protected boolean isResizable() {
    	return true;
    }

    @Override
    protected DBCExecutionContext getCurrentContext() {
        return null;
    }

    protected IDialogSettings getDialogBoundsSettings()
    {
        return UIUtils.getDialogSettings(DIALOG_ID);
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        getShell().setText("Pending transactions");

        Composite composite = (Composite) super.createDialogArea(parent);

        Tree contextTree = new Tree(composite, SWT.FULL_SELECTION | SWT.BORDER);
        contextTree.setLayoutData(new GridData(GridData.FILL_BOTH));

        super.createTransactionLogPanel(composite);

        return parent;
    }

    public static void showDialog(Shell shell) {
        IWorkbenchPart activePart = DBeaverUI.getActiveWorkbenchWindow().getActivePage().getActivePart();
        if (activePart == null) {
            UIUtils.showErrorDialog(
                shell,
                "No active part",
                "No active part.");
        } else {
            final PendingTransactionsDialog dialog = new PendingTransactionsDialog(shell, activePart);
            dialog.open();
        }
    }

}
