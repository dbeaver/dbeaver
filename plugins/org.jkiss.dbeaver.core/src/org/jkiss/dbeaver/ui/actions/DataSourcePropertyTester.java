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
package org.jkiss.dbeaver.ui.actions;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.Log;
import org.jkiss.dbeaver.model.DBPContextProvider;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCTransactionManager;
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
    static protected final Log log = Log.getLog(DataSourcePropertyTester.class);

    public static final String NAMESPACE = "org.jkiss.dbeaver.core.datasource";
    public static final String PROP_CONNECTED = "connected";
    public static final String PROP_TRANSACTIONAL = "transactional";
    public static final String PROP_TRANSACTION_ACTIVE = "transactionActive";

    public DataSourcePropertyTester() {
        super();
    }

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if (!(receiver instanceof DBPContextProvider)) {
            return false;
        }
        DBPContextProvider contextProvider = (DBPContextProvider)receiver;
        @Nullable
        DBCExecutionContext context = contextProvider.getExecutionContext();
        if (PROP_CONNECTED.equals(property)) {
            boolean isConnected = Boolean.TRUE.equals(expectedValue);
            return isConnected ? context != null && context.isConnected() : context == null || !context.isConnected();
        } else if (PROP_TRANSACTIONAL.equals(property)) {
            if (context == null) {
                return false;
            }
            if (!context.isConnected()) {
                return Boolean.FALSE.equals(expectedValue);
            }
            DBCTransactionManager txnManager = DBUtils.getTransactionManager(context);
            try {
                return txnManager != null && Boolean.valueOf(!txnManager.isAutoCommit()).equals(expectedValue);
            } catch (DBCException e) {
                log.debug("Error checking auto-commit state", e);
                return false;
            }
        } else if (PROP_TRANSACTION_ACTIVE.equals(property)) {
            if (context != null && context.isConnected()) {
                QMMSessionInfo session = DBeaverCore.getInstance().getQueryManager().getMetaCollector().getSessionInfo(context);
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