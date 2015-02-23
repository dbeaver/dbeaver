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
package org.jkiss.dbeaver.ui.actions;

import org.jkiss.dbeaver.core.Log;
import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.runtime.qm.meta.QMMSessionInfo;
import org.jkiss.dbeaver.runtime.qm.meta.QMMStatementExecuteInfo;
import org.jkiss.dbeaver.runtime.qm.meta.QMMTransactionInfo;
import org.jkiss.dbeaver.runtime.qm.meta.QMMTransactionSavepointInfo;
import org.jkiss.dbeaver.ui.ActionUtils;

/**
 * DatabaseEditorPropertyTester
 */
public class DataSourcePropertyTester extends PropertyTester
{
    static final Log log = Log.getLog(DataSourcePropertyTester.class);

    public static final String NAMESPACE = "org.jkiss.dbeaver.core.datasource";
    public static final String PROP_CONNECTED = "connected";
    public static final String PROP_TRANSACTIONAL = "transactional";
    public static final String PROP_TRANSACTION_ACTIVE = "transactionActive";

    public DataSourcePropertyTester() {
        super();
    }

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if (!(receiver instanceof DBSDataSourceContainer)) {
            return false;
        }
        DBSDataSourceContainer dataSourceContainer = (DBSDataSourceContainer)receiver;
        if (PROP_CONNECTED.equals(property)) {
            return dataSourceContainer.isConnected() == Boolean.valueOf(String.valueOf(expectedValue));
        } else if (PROP_TRANSACTIONAL.equals(property)) {
            if (!dataSourceContainer.isConnected()) {
                return Boolean.FALSE.equals(expectedValue);
            }
            return Boolean.valueOf(!dataSourceContainer.isConnectionAutoCommit()).equals(expectedValue);
        } else if (PROP_TRANSACTION_ACTIVE.equals(property)) {
            if (dataSourceContainer.isConnected()) {
                DBPDataSource dataSource = dataSourceContainer.getDataSource();
                QMMSessionInfo session = DBeaverCore.getInstance().getQueryManager().getMetaCollector().getSessionInfo(dataSource);
                QMMTransactionInfo transaction = session.getTransaction();
                if (transaction != null) {
                    QMMTransactionSavepointInfo savepoint = transaction.getCurrentSavepoint();
                    if (savepoint != null) {
                        QMMStatementExecuteInfo execute = savepoint.getLastExecute();
                        if (execute != null) {
                            return Boolean.TRUE.equals(expectedValue);
                        }
                    }
                }
            }
            return Boolean.FALSE.equals(expectedValue);
        }
        return false;
    }

    public static void firePropertyChange(String propName)
    {
        ActionUtils.evaluatePropertyState(NAMESPACE + "." + propName);
    }

    public static void fireCommandRefresh(final String commandID)
    {
        // Update commands
        final ICommandService commandService = (ICommandService) PlatformUI.getWorkbench().getService(ICommandService.class);
        if (commandService != null) {
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run()
                {
                    commandService.refreshElements(commandID, null);
                }
            });
        }
    }
}