/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.actions.datasource;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.menus.CommandContributionItem;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.DBPTransactionIsolation;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCTransactionManager;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.ICommandIds;
import org.jkiss.dbeaver.ui.actions.DataSourceHandler;
import org.jkiss.utils.CommonUtils;

import java.util.List;

public class DataSourceTransactionModeContributor extends DataSourceMenuContributor
{
    @Override
    protected void fillContributionItems(final List<IContributionItem> menuItems)
    {
        IEditorPart activePart = DBeaverUI.getActiveWorkbenchWindow().getActivePage().getActiveEditor();
        DBSDataSourceContainer container = DataSourceHandler.getDataSourceContainer(activePart);

        DBPDataSource dataSource = null;
        if (container != null) {
            dataSource = container.getDataSource();
        }
        if (dataSource == null) {
            return;
        }
        final DBPDataSourceInfo dsInfo = dataSource.getInfo();

        DBCTransactionManager txnManager = DBUtils.getTransactionManager(dataSource);
        if (txnManager != null) {
            menuItems.add(ActionUtils.makeCommandContribution(
                DBeaverUI.getActiveWorkbenchWindow(),
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
            dataSource.getContainer().setDefaultTransactionsIsolation(level);
            dataSource.getContainer().persistConfiguration();
        }
    }

}