/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchPart;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.qm.QMTransactionState;
import org.jkiss.dbeaver.model.qm.QMUtils;
import org.jkiss.dbeaver.model.struct.DBSInstance;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.datasource.DataSourceCommitHandler;
import org.jkiss.dbeaver.ui.actions.datasource.DataSourceRollbackHandler;
import org.jkiss.utils.ArrayUtils;

import java.util.ArrayList;
import java.util.List;

public class PendingTransactionsDialog extends TransactionInfoDialog {

    private static final String DIALOG_ID = "DBeaver.PendingTransactionsDialog";//$NON-NLS-1$
    private Tree contextTree;
    private DBCExecutionContext selectedContext;
    private Button commitButton;
    private Button rollbackButton;

    private PendingTransactionsDialog(Shell parentShell, IWorkbenchPart activePart) {
        super(parentShell, "Pending transactions", activePart);
    }

    @Override
    protected boolean isResizable() {
    	return true;
    }

    @Override
    protected DBCExecutionContext getCurrentContext() {
        return selectedContext;
    }

    protected IDialogSettings getDialogBoundsSettings()
    {
        return UIUtils.getDialogSettings(DIALOG_ID);
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite composite = (Composite) super.createDialogArea(parent);

        contextTree = new Tree(composite, SWT.FULL_SELECTION | SWT.BORDER);
        contextTree.setHeaderVisible(true);
        contextTree.setLinesVisible(true);
        TreeColumn colName = new TreeColumn(contextTree, SWT.NONE);
        colName.setText("Connection");
        TreeColumn colTxn = new TreeColumn(contextTree, SWT.RIGHT);
        colTxn.setText("Transaction");
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = contextTree.getHeaderHeight() + contextTree.getItemHeight() * 5;
        contextTree.setLayoutData(gd);
        contextTree.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (e.item != null && e.item.getData() instanceof DBCExecutionContext) {
                    selectedContext = (DBCExecutionContext) e.item.getData();
                } else {
                    selectedContext = null;
                }
                boolean hasTransaction = selectedContext != null && QMUtils.isTransactionActive(selectedContext, false);
                commitButton.setEnabled(hasTransaction);
                rollbackButton.setEnabled(hasTransaction);
                logViewer.setFilter(createContextFilter(selectedContext));
                logViewer.refresh();
            }
        });

        closeOnFocusLost(contextTree);

        {
            Composite controlPanel = UIUtils.createPlaceholder(composite, 3, 5);
            controlPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            final Button showAllCheck = UIUtils.createCheckbox(controlPanel, "Show all connections", "Show all datasource connections. Otherwise shows only transactional connections.", false, 1);
            showAllCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    loadContexts(showAllCheck.getSelection());
                }
            });
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.grabExcessHorizontalSpace = true;
            showAllCheck.setLayoutData(gd);
            commitButton = UIUtils.createPushButton(controlPanel, "Commit", DBeaverIcons.getImage(UIIcon.TXN_COMMIT));
            commitButton.setEnabled(false);
            commitButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    endTransaction(true);
                }
            });
            rollbackButton = UIUtils.createPushButton(controlPanel, "Rollback", DBeaverIcons.getImage(UIIcon.TXN_ROLLBACK));
            rollbackButton.setEnabled(false);
            rollbackButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    endTransaction(false);
                }
            });

            closeOnFocusLost(showAllCheck, commitButton, rollbackButton);
        }

        super.createTransactionLogPanel(composite);

        loadContexts(false);

        return parent;
    }

    private void endTransaction(boolean commit) {
        if (selectedContext == null) {
            return;
        }
        if (commit) {
            DataSourceCommitHandler.execute(selectedContext);
        } else {
            DataSourceRollbackHandler.execute(selectedContext);
        }
        commitButton.setEnabled(false);
        rollbackButton.setEnabled(false);
    }

    private void loadContexts(boolean showAllContexts) {
        contextTree.removeAll();

        // Load all open context
        for (DBPDataSourceContainer dataSource : DataSourceRegistry.getAllDataSources()) {
            if (!dataSource.isConnected() || dataSource.getDataSource() == null) {
                continue;
            }
            for (DBSInstance instance : dataSource.getDataSource().getAvailableInstances()) {
                DBCExecutionContext[] allContexts = instance.getAllContexts();
                if (ArrayUtils.isEmpty(allContexts)) {
                    continue;
                }
                List<DBCExecutionContext> txnContexts = new ArrayList<>();
                for (DBCExecutionContext context : allContexts) {
                    if (showAllContexts || QMUtils.isTransactionActive(context, false)) {
                        txnContexts.add(context);
                    }
                }
                if (txnContexts.isEmpty()) {
                    continue;
                }
                TreeItem dsItem = new TreeItem(contextTree, SWT.NONE);
                dsItem.setText(dataSource.getName());
                dsItem.setImage(DBeaverIcons.getImage(dataSource.getDriver().getIcon()));
                dsItem.setData(dataSource);

                for (DBCExecutionContext context : txnContexts) {
                    QMTransactionState txnState = QMUtils.getTransactionState(context);
                    TreeItem contextItem = new TreeItem(dsItem, SWT.NONE);
                    contextItem.setText(0, context.getContextName());
                    String stateString = String.valueOf(txnState.getUpdateCount()) + "/" + String.valueOf(txnState.getExecuteCount());
                    contextItem.setText(1, stateString);
                    contextItem.setData(context);
                }
                dsItem.setExpanded(true);
            }
        }

        UIUtils.asyncExec(new Runnable() {
            @Override
            public void run() {
                UIUtils.packColumns(contextTree);
            }
        });
    }

    public static void showDialog(Shell shell) {
        IWorkbenchPart activePart = UIUtils.getActiveWorkbenchWindow().getActivePage().getActivePart();
        if (activePart == null) {
            DBWorkbench.getPlatformUI().showError(
                    "No active part",
                "No active part.");
        } else {
            final PendingTransactionsDialog dialog = new PendingTransactionsDialog(shell, activePart);
            dialog.setModeless(true);
            dialog.open();
        }
    }

}
