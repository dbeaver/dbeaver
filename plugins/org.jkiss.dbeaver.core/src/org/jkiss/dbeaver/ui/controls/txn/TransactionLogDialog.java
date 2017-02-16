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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.qm.QMEventFilter;
import org.jkiss.dbeaver.model.qm.QMMetaEvent;
import org.jkiss.dbeaver.model.qm.QMUtils;
import org.jkiss.dbeaver.model.qm.meta.*;
import org.jkiss.dbeaver.runtime.qm.DefaultEventFilter;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.querylog.QueryLogViewer;

import java.util.Objects;

public class TransactionLogDialog extends Dialog {

    private static final String DIALOG_ID = "DBeaver.TransactionLogDialog";//$NON-NLS-1$

    private final DBCExecutionContext context;
    private final IEditorPart activeEditor;

    public TransactionLogDialog(Shell parentShell, DBCExecutionContext context, IEditorPart activeEditor)
    {
        super(parentShell);
        this.context = context;
        this.activeEditor = activeEditor;
    }

    @Override
    protected boolean isResizable() {
    	return true;
    }

    protected IDialogSettings getDialogBoundsSettings()
    {
        return UIUtils.getDialogSettings(DIALOG_ID);
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        getShell().setText("Transaction log [" + context.getContextName() + "]");

        Composite composite = (Composite) super.createDialogArea(parent);

        final QMMTransactionSavepointInfo currentSP = QMUtils.getCurrentTransaction(context);
        QMEventFilter filter = new QMEventFilter() {
            @Override
            public boolean accept(QMMetaEvent event) {
                QMMObject object = event.getObject();
                if (object instanceof QMMStatementExecuteInfo) {
                    QMMStatementExecuteInfo exec = (QMMStatementExecuteInfo) object;
                    return exec.getSavepoint() == currentSP && exec.isTransactional();
                }
                return false;
            }
        };
        QueryLogViewer logViewer = new QueryLogViewer(composite, activeEditor.getEditorSite(), filter, false);

        return parent;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
    }

    public static void showDialog(Shell shell, DBCExecutionContext executionContext) {
        IEditorPart activeEditor = DBeaverUI.getActiveWorkbenchWindow().getActivePage().getActiveEditor();
        if (activeEditor == null) {
            UIUtils.showErrorDialog(
                shell,
                "No editor",
                "Transaction log is not available.\nOpen database editor.");
        } else if (executionContext == null) {
            UIUtils.showErrorDialog(
                shell,
                "Not connected",
                "Transaction log is not available.\nConnect to a database.");
        } else {
            final TransactionLogDialog dialog = new TransactionLogDialog(shell, executionContext, activeEditor);
            dialog.open();
        }
    }
}
