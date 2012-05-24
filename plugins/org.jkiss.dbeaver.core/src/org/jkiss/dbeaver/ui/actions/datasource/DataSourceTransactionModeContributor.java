/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
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
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.ICommandIds;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
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

            menuItems.add(new Separator());

            // Transactions
            DBPTransactionIsolation txnLevelCurrent = null;
            try {
                txnLevelCurrent = txnManager.getTransactionIsolation();
            } catch (DBCException ex) {
                log.warn("Can't determine current transaction isolation level", ex);
            }
            for (DBPTransactionIsolation txi : CommonUtils.safeCollection(dsInfo.getSupportedTransactionIsolations())) {
                if (!txi.isEnabled()) {
                    continue;
                }
                menuItems.add(ActionUtils.makeActionContribution(
                    new TransactionIsolationAction(dataSource, txi, txi.equals(txnLevelCurrent)),
                    true));
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
            try {
                DBeaverCore.getInstance().runInProgressService(new DBRRunnableWithProgress()
                {
                    @Override
                    public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                    {
                        DBCExecutionContext context = dataSource.openContext(monitor, DBCExecutionPurpose.META, "Check connection's auto-commit state");
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
                });
            } catch (InvocationTargetException e) {
                UIUtils.showErrorDialog(
                    null,
                    "Transaction mode change",
                    "Can't set transaction isolation",
                    e.getTargetException());
            } catch (InterruptedException e) {
                // ok
            }
        }
    }

}