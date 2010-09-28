/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.datasource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IWorkbenchPart;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.DBPTransactionIsolation;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCTransactionManager;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.actions.DataSourceHandler;

public class DataSourceTransactionModeContributor extends ContributionItem
{
    static final Log log = LogFactory.getLog(DataSourceTransactionModeContributor.class);

    @Override
    public void fill(Menu menu, int index)
    {
        createMenu(menu);
    }
    
    private void createMenu(final Menu menu)
    {
        final DBPDataSource dataSource = getDataSource();
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
                    DataSourceTransactionModeHandler.execute(menu.getShell(), dataSource.getContainer());
                }
            });

            new MenuItem(menu, SWT.SEPARATOR);

            // Transactions
            DBPTransactionIsolation txnLevelCurrent = null;
            try {
                txnLevelCurrent = txnManager.getTransactionIsolation();
            } catch (DBCException ex) {
                log.warn("Can't determine current transaction isolation level", ex);
            }
            for (DBPTransactionIsolation txi : dsInfo.getSupportedTransactionIsolations()) {
                if (!txi.isEnabled()) {
                    continue;
                }
                final DBPTransactionIsolation txnLevel = txi;
                MenuItem txnItem = new MenuItem(menu, SWT.RADIO);
                txnItem.setText(txnLevel.getName());
                txnItem.setSelection(txnLevel == txnLevelCurrent);
                txnItem.setData(txnLevel);
                txnItem.addSelectionListener(new SelectionAdapter()
                {
                    public void widgetSelected(SelectionEvent e)
                    {
                        try {
                            if (!txnManager.getTransactionIsolation().equals(txnLevel)) {
                                txnManager.setTransactionIsolation(txnLevel);
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

    private DBPDataSource getDataSource()
    {
        IWorkbenchPart activePart = DBeaverCore.getActiveWorkbenchWindow().getActivePage().getActiveEditor();
        DBSDataSourceContainer container = DataSourceHandler.getDataSourceContainer(activePart);
        return container == null ? null : container.getDataSource();
    }

}