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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.qm.QMEventFilter;
import org.jkiss.dbeaver.model.qm.QMUtils;
import org.jkiss.dbeaver.model.qm.meta.QMMObject;
import org.jkiss.dbeaver.model.qm.meta.QMMSessionInfo;
import org.jkiss.dbeaver.model.qm.meta.QMMStatementExecuteInfo;
import org.jkiss.dbeaver.model.qm.meta.QMMTransactionSavepointInfo;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.querylog.QueryLogViewer;
import org.jkiss.utils.CommonUtils;

public abstract class TransactionInfoDialog extends Dialog {

    private static final QMEventFilter VOID_FILTER = event -> false;


    private final IWorkbenchPart activeEditor;
    protected QueryLogViewer logViewer;
    private Button showAllCheck;
    protected Button showPreviousCheck;

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
        QMEventFilter filter = context == null ? VOID_FILTER : createContextFilter(context);
        logViewer = new QueryLogViewer(composite, activeEditor.getSite(), filter, false, true);
        logViewer.setUseDefaultFilter(false);
        final Object gd = logViewer.getControl().getLayoutData();
        if (gd instanceof GridData) {
            ((GridData) gd).heightHint = logViewer.getControl().getHeaderHeight() + logViewer.getControl().getItemHeight() * 5;
        }

        showAllCheck = UIUtils.createCheckbox(composite, "Show all queries", "Show all transaction queries. Otherwise shows only modifying queries.", false, 1);
        showAllCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateTransactionFilter();
            }
        });

        showPreviousCheck = UIUtils.createCheckbox(composite, "Show previous transactions", "Show previous transactions. Otherwise shows only active one.", false, 1);
        showPreviousCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateTransactionFilter();
            }
        });

    }

    protected void updateTransactionFilter() {
        DBCExecutionContext context = getCurrentContext();
        QMEventFilter filter = context == null ? VOID_FILTER : createContextFilter(context);
        logViewer.setFilter(filter);
        logViewer.refresh();
    }

    protected QMEventFilter createContextFilter(DBCExecutionContext executionContext) {
        if (executionContext == null) {
            return VOID_FILTER;
        }
        final boolean showAll = showAllCheck != null && showAllCheck.getSelection();
        final boolean showPrevious = showPreviousCheck != null && showPreviousCheck.getSelection();

        final QMMSessionInfo currentSession = QMUtils.getCurrentSession(executionContext);
        final QMMTransactionSavepointInfo currentSP = QMUtils.getCurrentTransaction(executionContext);

        QMEventFilter filter = event -> {
            QMMObject object = event.getObject();
            if (object instanceof QMMStatementExecuteInfo) {
                QMMStatementExecuteInfo exec = (QMMStatementExecuteInfo) object;
                if (!showPrevious && !CommonUtils.equalObjects(exec.getSavepoint(), currentSP)) {
                    return false;
                }
                if (!showAll && !CommonUtils.equalObjects(exec.getStatement().getSession(), currentSession)) {
                    return false;
                }
                DBCExecutionPurpose purpose = exec.getStatement().getPurpose();
                if (purpose == DBCExecutionPurpose.META || purpose == DBCExecutionPurpose.UTIL) {
                    return false;
                }
                return (showAll || exec.isTransactional());
            }
            return false;
        };
        return filter;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.CLOSE_LABEL, true);
    }

}
