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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.qm.QMEventFilter;
import org.jkiss.dbeaver.model.qm.QMMetaEvent;
import org.jkiss.dbeaver.model.qm.QMUtils;
import org.jkiss.dbeaver.model.qm.meta.QMMObject;
import org.jkiss.dbeaver.model.qm.meta.QMMStatementExecuteInfo;
import org.jkiss.dbeaver.model.qm.meta.QMMTransactionSavepointInfo;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.querylog.QueryLogViewer;

public abstract class TransactionInfoDialog extends Dialog {

    private static final QMEventFilter VOID_FILTER = new QMEventFilter() {
        @Override
        public boolean accept(QMMetaEvent event) {
            return false;
        }
    };


    private final IWorkbenchPart activeEditor;
    protected QueryLogViewer logViewer;

    public TransactionInfoDialog(Shell parentShell, IWorkbenchPart activeEditor)
    {
        super(parentShell);
        this.activeEditor = activeEditor;
    }

    @Override
    protected boolean isResizable() {
    	return true;
    }

    protected abstract DBCExecutionContext getCurrentContext();

    protected void createTransactionLogPanel(Composite composite) {
        DBCExecutionContext context = getCurrentContext();
        QMEventFilter filter = context == null ? VOID_FILTER : createContextFilter(context, false);
        logViewer = new QueryLogViewer(composite, activeEditor.getSite(), filter, false);

        final Button showAllCheck = UIUtils.createCheckbox(composite, "Show all queries", "Show all transaction queries. Otherwise shows only modifying queries.", false, 1);
        showAllCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                DBCExecutionContext context = getCurrentContext();
                QMEventFilter filter = context == null ? VOID_FILTER : createContextFilter(context, showAllCheck.getSelection());
                logViewer.setFilter(filter);
                logViewer.refresh();
            }
        });

    }

    protected QMEventFilter createContextFilter(DBCExecutionContext executionContext, final boolean showAll) {
        final QMMTransactionSavepointInfo currentSP = QMUtils.getCurrentTransaction(executionContext);
        QMEventFilter filter = new QMEventFilter() {
            @Override
            public boolean accept(QMMetaEvent event) {
                QMMObject object = event.getObject();
                if (object instanceof QMMStatementExecuteInfo) {
                    QMMStatementExecuteInfo exec = (QMMStatementExecuteInfo) object;
                    return exec.getSavepoint() == currentSP && (showAll || exec.isTransactional());
                }
                return false;
            }
        };
        return filter;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.CLOSE_LABEL, true);
    }

}
