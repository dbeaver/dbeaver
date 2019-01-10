/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.actions.datasource;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.menus.CommandContributionItem;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreCommands;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCTransactionManager;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.AbstractDataSourceHandler;
import org.jkiss.utils.CommonUtils;

import java.util.List;

public class DataSourceTransactionModeContributor extends DataSourceMenuContributor
{
    private static final Log log = Log.getLog(DataSourceTransactionModeContributor.class);

    @Override
    protected void fillContributionItems(final List<IContributionItem> menuItems)
    {
        IWorkbenchWindow window = UIUtils.getActiveWorkbenchWindow();
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

        DBCTransactionManager txnManager = DBUtils.getTransactionManager(dataSource.getDefaultInstance().getDefaultContext(false));
        if (txnManager != null) {
            menuItems.add(ActionUtils.makeCommandContribution(
                window,
                CoreCommands.CMD_TOGGLE_AUTOCOMMIT,
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
                DBWorkbench.getPlatformUI().showError(
                        "Transactions Isolation",
                    "Can't set transaction isolation level to '" + level + "'",
                    e);
                return;
            }
            dataSource.getContainer().persistConfiguration();
        }
    }

}