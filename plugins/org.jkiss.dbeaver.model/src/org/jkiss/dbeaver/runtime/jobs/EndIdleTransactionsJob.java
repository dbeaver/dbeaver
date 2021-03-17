/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.runtime.jobs;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPMessageType;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.DBeaverNotifications;
import org.jkiss.dbeaver.runtime.ui.UIServiceConnections;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * EndIdleTransactionsJob
 */
class EndIdleTransactionsJob extends AbstractJob {
    private static final Log log = Log.getLog(EndIdleTransactionsJob.class);

    private static final Set<String> activeDataSources = new HashSet<>();
    private static final Object CONFIRM_SYNC = new Object();

    private final DBPDataSource dataSource;
    private final Map<DBCExecutionContext, DBCTransactionManager> txnToEnd;

    public static boolean isInProcess(DBPDataSourceContainer ds) {
        synchronized (activeDataSources) {
            return activeDataSources.contains(ds.getId());
        }
    }

    EndIdleTransactionsJob(DBPDataSource dataSource, Map<DBCExecutionContext, DBCTransactionManager> txnToEnd) {
        super("Connection ping (" + dataSource.getContainer().getName() + ")");
        setUser(false);
        setSystem(true);
        this.dataSource = dataSource;
        this.txnToEnd = txnToEnd;
    }

    @Override
    protected IStatus run(DBRProgressMonitor monitor) {
        String dsId = dataSource.getContainer().getId();
        synchronized (activeDataSources) {
            if (activeDataSources.contains(dsId)) {
                return Status.CANCEL_STATUS;
            }
            activeDataSources.add(dsId);
        }
        try {
            UIServiceConnections serviceConnections = DBWorkbench.getService(UIServiceConnections.class);
            if (serviceConnections != null) {
                synchronized (CONFIRM_SYNC) {
                    if (!serviceConnections.confirmTransactionsClose(txnToEnd.keySet().toArray(new DBCExecutionContext[0]))) {
                        return Status.CANCEL_STATUS;
                    }
                }
            }
            log.debug("End idle " + txnToEnd.size() + " transactions for " + dsId);
            for (Map.Entry<DBCExecutionContext, DBCTransactionManager> tee : txnToEnd.entrySet()) {
                try (DBCSession session = tee.getKey().openSession(monitor, DBCExecutionPurpose.UTIL, "End idle transaction")) {
                    try {
                        tee.getValue().rollback(session, null);
                    } catch (DBCException e) {
                        log.error("Error ending idle transaction", e);
                    }
                }
            }
            DBeaverNotifications.showNotification(
                dataSource,
                DBeaverNotifications.NT_ROLLBACK,
                "Transactions have been rolled back after long idle period",
                DBPMessageType.ERROR);

        } finally {
            synchronized (activeDataSources) {
                activeDataSources.remove(dsId);
            }
        }
        return Status.OK_STATUS;
    }

/*
    class EndTransactionConfirmationJob extends UIJob {

        public EndTransactionConfirmationJob() {
            super("Show end transaction confirmation for " + dataSource.getContainer().getName());
            setUser(false);
            setSystem(true);
        }

        @Override
        public IStatus runInUIThread(IProgressMonitor monitor) {

            return Status.OK_STATUS;
        }
    }

    class EndTransactionConfirmationDialog extends ConfirmationDialog {

    }
*/

}
