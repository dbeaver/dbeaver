/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPMessageType;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.qm.QMTransactionState;
import org.jkiss.dbeaver.model.qm.QMUtils;
import org.jkiss.dbeaver.runtime.DBeaverNotifications;
import org.jkiss.dbeaver.runtime.TasksJob;
import org.jkiss.dbeaver.ui.actions.AbstractDataSourceHandler;
import org.jkiss.dbeaver.ui.controls.txn.TransactionLogDialog;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.lang.reflect.InvocationTargetException;

public class DataSourceRollbackHandler extends AbstractDataSourceHandler
{
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        DBCExecutionContext context = getActiveExecutionContext(event, true);
        if (context != null && context.isConnected()) {
            execute(context);
        }
        return null;
    }

    public static void execute(final DBCExecutionContext context) {
        TasksJob.runTask("Rollback transaction", monitor -> {
            DBCTransactionManager txnManager = DBUtils.getTransactionManager(context);
            if (txnManager != null) {
                QMTransactionState txnInfo = QMUtils.getTransactionState(context);
                try (DBCSession session = context.openSession(monitor, DBCExecutionPurpose.UTIL, "Rollback transaction")) {
                    txnManager.rollback(session, null);
                } catch (DBCException e) {
                    throw new InvocationTargetException(e);
                }

                if (context.getDataSource().getContainer().getPreferenceStore().getBoolean(ModelPreferences.TRANSACTIONS_SHOW_NOTIFICATIONS)) {
                    DBeaverNotifications.showNotification(
                        context.getDataSource(),
                        DBeaverNotifications.NT_ROLLBACK,
                        "Transaction has been rolled back\n\n" +
                            "Query count: " + txnInfo.getUpdateCount() + "\n" +
                            "Duration: " + RuntimeUtils.formatExecutionTime(System.currentTimeMillis() - txnInfo.getTransactionStartTime()) + "\n",
                        DBPMessageType.ERROR,
                        () -> TransactionLogDialog.showDialog(null, context, true));
                }
            }
        });
    }

}