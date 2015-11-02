/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.actions.datasource;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.menus.CommandContributionItem;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.DBPTransactionIsolation;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCTransactionManager;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.ICommandIds;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.AbstractDataSourceHandler;
import org.jkiss.utils.CommonUtils;

import java.util.List;

public class DataSourceTransactionModeContributor extends DataSourceMenuContributor
{
    @Override
    protected void fillContributionItems(final List<IContributionItem> menuItems)
    {
        IWorkbenchWindow window = DBeaverUI.getActiveWorkbenchWindow();
        if (window == null) {
            return;
        }
        IEditorPart activePart = window.getActivePage().getActiveEditor();
        DBPDataSourceContainer container = AbstractDataSourceHandler.getDataSourceContainer(activePart);

        DBPDataSource dataSource = null;
        if (container != null) {
            dataSource = container.getDataSource();
        }
        if (dataSource == null) {
            return;
        }
        final DBPDataSourceInfo dsInfo = dataSource.getInfo();

        DBCTransactionManager txnManager = DBUtils.getTransactionManager(dataSource.getDefaultContext(false));
        if (txnManager != null) {
            menuItems.add(ActionUtils.makeCommandContribution(
                window,
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
            for (DBPTransactionIsolation txi : CommonUtils.safeCollection(dsInfo.getSupportedTransactionsIsolation())) {
                if (!txi.isEnabled()) {
                    continue;
                }
                menuItems.add(ActionUtils.makeActionContribution(
                    new TransactionIsolationAction(dataSource, txi, txi.equals(txnLevelCurrent)),
                    true));
            }
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
            return level.getTitle();
        }

        @Override
        public void run()
        {
            try {
                dataSource.getContainer().setDefaultTransactionsIsolation(level);
            } catch (DBException e) {
                UIUtils.showErrorDialog(
                    null,
                    "Transactions Isolation",
                    "Can't set transaction isolation level to '" + level + "'",
                    e);
                return;
            }
            dataSource.getContainer().persistConfiguration();
        }
    }

}