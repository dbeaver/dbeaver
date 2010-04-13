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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.registry.event.DataSourceEvent;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.DBPTransactionIsolation;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.dbc.DBCSession;
import org.jkiss.dbeaver.utils.DBeaverUtils;


public class TransactionModeAction extends SessionAction implements IWorkbenchWindowPulldownDelegate, IWorkbenchWindowPulldownDelegate2
{

    public void run(IAction action)
    {
        try {
            // Toggle autocommit flag
            DBCSession session = isConnected() ? getSession() : null;
            if (session != null) {
                session.setAutoCommit(!session.isAutoCommit());
                DataSourceRegistry.getDefault().fireDataSourceEvent(DataSourceEvent.Action.CHANGE, getDataSource(), this);
            }
        } catch (DBException e) {
            DBeaverUtils.showErrorDialog(getWindow().getShell(), "Auto-Commit", "Error while toggle auto-commit", e);
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
        final DBPDataSourceInfo dsInfo;
        try {
            dsInfo = dataSource.getInfo();
        } catch (DBException ex) {
            log.error("Can't obtain datasource info", ex);
            return;
        }
        final DBCSession session;
        try {
            session = getSession();
        } catch (DBException e) {
            log.error("Can't obtain database session", e);
            return;
        }
        if (session == null) {
            return;
        }
        // Auto-commit
        MenuItem autoCommit = new MenuItem(menu, SWT.CHECK);
        autoCommit.setText("Auto-commit");
        try {
            autoCommit.setSelection(session.isAutoCommit());
        } catch (DBCException ex) {
            log.warn("Can't check auto-commit status", ex);
        }
        autoCommit.addSelectionListener(new SelectionAdapter()
        {
            public void widgetSelected(SelectionEvent e)
            {
                try {
                    session.setAutoCommit(!session.isAutoCommit());
                    DataSourceRegistry.getDefault().fireDataSourceEvent(DataSourceEvent.Action.CHANGE, getDataSource(), this);
                } catch (DBCException ex) {
                    log.error("Can't change auto-commit status", ex);
                }
            }
        });

        new MenuItem(menu, SWT.SEPARATOR);

        // Transactions
        DBPTransactionIsolation curTxi = null;
        try {
            curTxi = session.getTransactionIsolation();
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
                        if (!session.getTransactionIsolation().equals(newTxi)) {
                            session.setTransactionIsolation(newTxi);
                        }
                    } catch (DBCException ex) {
                        log.warn("Can't change current transaction isolation level", ex);
                    }
                }
            });
        }
    }

}