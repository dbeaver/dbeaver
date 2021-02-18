/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;

public class TransactionLogDialog extends TransactionInfoDialog {

    private static final String DIALOG_ID = "DBeaver.TransactionLogDialog";//$NON-NLS-1$

    private final DBCExecutionContext context;
    private final boolean showPreviousTxn;

    private TransactionLogDialog(Shell parentShell, DBCExecutionContext context, IWorkbenchPart activeEditor, boolean showPreviousTxn)
    {
        super(parentShell,
            NLS.bind(CoreMessages.transaction_log_dialog_header_transaction_log, context.getDataSource().getContainer().getName(), context.getContextName()),
            activeEditor);
        this.context = context;
        this.showPreviousTxn = showPreviousTxn;
    }

    @Override
    protected boolean isResizable() {
    	return true;
    }

    @Override
    protected DBCExecutionContext getCurrentContext() {
        return context;
    }

    protected IDialogSettings getDialogBoundsSettings()
    {
        return UIUtils.getDialogSettings(DIALOG_ID);
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite composite = (Composite) super.createDialogArea(parent);

        super.createTransactionLogPanel(composite);

        showPreviousCheck.setSelection(showPreviousTxn);
        updateTransactionFilter();

        return parent;
    }

    public static void showDialog(Shell shell, DBCExecutionContext executionContext) {
        showDialog(shell, executionContext, false);
    }

    public static void showDialog(Shell shell, DBCExecutionContext executionContext, boolean showPreviousTxn) {
        IEditorPart activeEditor = UIUtils.getActiveWorkbenchWindow().getActivePage().getActiveEditor();
        if (activeEditor == null) {
            DBWorkbench.getPlatformUI().showError(
                    CoreMessages.transaction_log_dialog_error_no_editor,
                CoreMessages.transaction_log_dialog_error_open_database);
        } else if (executionContext == null) {
            DBWorkbench.getPlatformUI().showError(
                CoreMessages.transaction_log_dialog_error_not_connected,
                CoreMessages.transaction_log_dialog_error_connect_to_a_database);
        } else {
            final TransactionLogDialog dialog = new TransactionLogDialog(shell, executionContext, activeEditor, showPreviousTxn);
            dialog.setModeless(true);
            dialog.open();
        }
    }
}
