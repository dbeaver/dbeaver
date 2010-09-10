/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IWorkbenchWindowPulldownDelegate;
import org.eclipse.ui.IWorkbenchWindowPulldownDelegate2;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.DBPEvent;
import org.jkiss.dbeaver.model.DBPTransactionIsolation;
import org.jkiss.dbeaver.model.dbc.DBCTransactionManager;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.dbc.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.utils.DBeaverUtils;

import java.lang.reflect.InvocationTargetException;


public class TransactionModeAction extends SessionAction implements IWorkbenchWindowPulldownDelegate, IWorkbenchWindowPulldownDelegate2
{

    public void run(IAction action)
    {
        try {
            final DBPDataSource dataSource = getDataSource();
            if (dataSource != null && dataSource.getContainer().isConnected()) {
                DBeaverCore.getInstance().runAndWait2(true, true, new DBRRunnableWithProgress() {
                    public void run(DBRProgressMonitor monitor)
                        throws InvocationTargetException, InterruptedException
                    {
                        DBCExecutionContext context = dataSource.openContext(monitor, "Change auto-commit flag");
                        try {
                            DBCTransactionManager txnManager = context.getTransactionManager();
                            txnManager.setAutoCommit(!txnManager.isAutoCommit());
                        }
                        catch (DBCException e) {
                            throw new InvocationTargetException(e);
                        }
                        finally {
                            context.close();
                        }
                    }
                });
                DataSourceRegistry.getDefault().fireDataSourceEvent(
                    DBPEvent.Action.OBJECT_UPDATE,
                    dataSource.getContainer());
            } else {
                DBeaverUtils.showErrorDialog(getWindow().getShell(), "Auto-Commit", "No active database");
            }
        } catch (InvocationTargetException e) {
            DBeaverUtils.showErrorDialog(getWindow().getShell(), "Auto-Commit", "Error while toggle auto-commit", e);
        } catch (InterruptedException e) {
            // do nothing
        }
    }

    public Menu getMenu(Control parent) {
        final Menu menu = new Menu(parent);
        createMenu(menu);
        return menu;
    }

    public Menu getMenu(Menu parent) {
        final Menu menu = new Menu(parent);
        createMenu(menu);
        return menu;
    }

    private void createMenu(Menu menu) {
        DBPDataSource dataSource = getDataSource();
        if (dataSource == null) {
            return;
        }
        final DBPDataSourceInfo dsInfo = dataSource.getInfo();

        DBCExecutionContext context = dataSource.openContext(VoidProgressMonitor.INSTANCE);
        try {
            final DBCTransactionManager txnManager = context.getTransactionManager();
            // Auto-commit
            MenuItem autoCommit = new MenuItem(menu, SWT.CHECK);
            autoCommit.setText("Auto-commit");
            try {
                autoCommit.setSelection(txnManager.isAutoCommit());
            } catch (DBCException ex) {
                log.warn("Can't check auto-commit status", ex);
            }
            autoCommit.addSelectionListener(new SelectionAdapter()
            {
                public void widgetSelected(SelectionEvent e)
                {
                    TransactionModeAction.this.run(null);
                }
            });

            new MenuItem(menu, SWT.SEPARATOR);

            // Transactions
            DBPTransactionIsolation curTxi = null;
            try {
                curTxi = txnManager.getTransactionIsolation();
            } catch (DBCException ex) {
                log.warn("Can't determine current transaction isolation level", ex);
            }
            for (DBPTransactionIsolation txi : dsInfo.getSupportedTransactionIsolations()) {
                if (!txi.isEnabled()) {
                    continue;
                }
                MenuItem txnItem = new MenuItem(menu, SWT.RADIO);
                txnItem.setText(txi.getName());
                txnItem.setSelection(txi == curTxi);
                txnItem.setData(txi);
                txnItem.addSelectionListener(new SelectionAdapter()
                {
                    public void widgetSelected(SelectionEvent e)
                    {
                        try {
                            DBPTransactionIsolation newTxi = (DBPTransactionIsolation) e.widget.getData();
                            if (!txnManager.getTransactionIsolation().equals(newTxi)) {
                                txnManager.setTransactionIsolation(newTxi);
                            }
                        } catch (DBCException ex) {
                            log.warn("Can't change current transaction isolation level", ex);
                        }
                    }
                });
            }
        }
        finally {
            context.close();
        }
    }

}