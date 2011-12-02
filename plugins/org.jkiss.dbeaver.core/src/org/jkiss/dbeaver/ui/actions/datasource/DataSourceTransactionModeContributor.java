/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.datasource;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.menus.CommandContributionItem;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.DBPTransactionIsolation;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCTransactionManager;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.ICommandIds;

import java.util.List;

public class DataSourceTransactionModeContributor extends DataSourceMenuContributor
{
    @Override
    protected void fillContributionItems(final List<IContributionItem> menuItems, final DBPDataSource dataSource, final DBSObject selectedObject)
    {
        final DBPDataSourceInfo dsInfo = dataSource.getInfo();

        DBCExecutionContext context = dataSource.openContext(VoidProgressMonitor.INSTANCE, DBCExecutionPurpose.META, "Check connection's auto-commit state");
        try {
            final DBCTransactionManager txnManager = context.getTransactionManager();
            menuItems.add(ActionUtils.makeCommandContribution(
                DBeaverCore.getActiveWorkbenchWindow(),
                ICommandIds.CMD_TOGGLE_AUTOCOMMIT,
                CommandContributionItem.STYLE_CHECK));
/*
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
*/

            menuItems.add(new Separator());

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
                menuItems.add(ActionUtils.makeActionContribution(
                    new TransactionIsolationAction(dataSource, txi, txi.equals(txnLevelCurrent)),
                    true));
/*
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
*/
            }
        }
        finally {
            context.close();
        }
    }

    private static class TransactionIsolationAction extends Action
    {

        private final DBPDataSource dataSource;
        private final DBPTransactionIsolation level;
        private final boolean checked;
        public TransactionIsolationAction(DBPDataSource dataSource, DBPTransactionIsolation level, boolean checked)
        {
            this.dataSource = dataSource;
            this.level = level;
            this.checked = checked;
        }

        @Override
        public int getStyle()
        {
            return AS_RADIO_BUTTON;
        }

        @Override
        public boolean isChecked()
        {
            return checked;
        }

        @Override
        public String getText()
        {
            return level.getName();
        }

        @Override
        public void run()
        {
            DBCExecutionContext context = dataSource.openContext(VoidProgressMonitor.INSTANCE, DBCExecutionPurpose.META, "Check connection's auto-commit state");
            final DBCTransactionManager txnManager = context.getTransactionManager();
            try {
                if (!txnManager.getTransactionIsolation().equals(level)) {
                    txnManager.setTransactionIsolation(level);
                }
            } catch (DBCException ex) {
                log.warn("Can't change current transaction isolation level", ex);
            } finally {
                context.close();
            }
        }
    }

}