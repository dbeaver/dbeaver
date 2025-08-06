/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISaveablePart;
import org.eclipse.ui.progress.UIJob;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceTask;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.qm.QMUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressListener;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSInstance;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.jobs.ConnectJob;
import org.jkiss.dbeaver.runtime.jobs.DisconnectJob;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.actions.DataSourceHandlerUtils;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;
import org.jkiss.dbeaver.ui.editors.entity.handlers.SaveChangesHandler;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public class DataSourceHandler {
    private static final Log log = Log.getLog(DataSourceHandler.class);

    public static final int END_TRANSACTION_WAIT_TIME = 3000;

    /**
     * Connects datasource
     *
     * @param monitor             progress monitor or null. If nul then new job will be started
     * @param dataSourceContainer container to connect
     * @param onFinish            finish handler
     */
    public static void connectToDataSource(
        @Nullable DBRProgressMonitor monitor,
        @NotNull DBPDataSourceContainer dataSourceContainer,
        @Nullable final DBRProgressListener onFinish
    ) {
        if (dataSourceContainer instanceof final DataSourceDescriptor dataSource && !dataSourceContainer.isConnected()) {
            Job[] connectJobs = Job.getJobManager().find(dataSource);
            if (!ArrayUtils.isEmpty(connectJobs)) {
                // Already connecting/disconnecting - just return
                IStatus lastResult = null;
                if (monitor != null && connectJobs.length == 1) {
                    try {
                        connectJobs[0].join(0, monitor.getNestedMonitor());
                        lastResult = connectJobs[0].getResult();
                    } catch (InterruptedException e) {
                        log.debug(e);
                    }
                }
                if (onFinish != null) {
                    if (lastResult == null) {
                        lastResult = dataSourceContainer.isConnected() ? Status.OK_STATUS : Status.CANCEL_STATUS;
                    }
                    onFinish.onTaskFinished(lastResult);
                }
                return;
            }

            // Ask for additional credentials if needed
            if (!DataSourceHandlerUtils.resolveSharedCredentials(dataSource, onFinish)) {
                return;
            }

            ConnectionFeatures.CONNECTION_OPEN.use(Map.of(
                "driver", dataSourceContainer.getDriver().getPreconfiguredId(),
                "origin", dataSourceContainer.getOrigin().getFullType()
            ));
            final ConnectJob connectJob = new ConnectJob(dataSource);
            final JobChangeAdapter jobChangeAdapter = new JobChangeAdapter() {
                @Override
                public void done(IJobChangeEvent event) {
                    IStatus result = connectJob.getConnectStatus();
                    if (onFinish != null) {
                        onFinish.onTaskFinished(result);
                    } else if (!result.isOK()) {
                        DBWorkbench.getPlatformUI().showError(connectJob.getName(), null, result);
                    }
                }
            };

            if (monitor != null) {
                connectJob.runSync(monitor);
                jobChangeAdapter.done(new IJobChangeEvent() {
                    @Override
                    public long getDelay() {
                        return 0;
                    }

                    @Override
                    public Job getJob() {
                        return connectJob;
                    }

                    @Override
                    public IStatus getResult() {
                        return connectJob.getConnectStatus();
                    }

                    public IStatus getJobGroupResult() {
                        return null;
                    }
                });
            } else {
                connectJob.addJobChangeListener(jobChangeAdapter);
                // Schedule in UI because connect may be initiated during application startup
                // and UI is still not initiated. In this case no progress dialog will appear
                // to be sure run in UI async
                UIUtils.asyncExec(connectJob::schedule);
            }
        }
    }

    public static void disconnectDataSource(DBPDataSourceContainer dataSourceContainer, @Nullable final Runnable onFinish) {

        // Save users
        for (DBPDataSourceTask user : dataSourceContainer.getTasks()) {
            if (user instanceof ISaveablePart) {
                if (!SaveChangesHandler.validateAndSave(new VoidProgressMonitor(), (ISaveablePart) user)) {
                    return;
                }
            }
        }
        if (!checkAndCloseActiveTransaction(dataSourceContainer, false)) {
            return;
        }

        if (dataSourceContainer instanceof final DataSourceDescriptor dataSourceDescriptor && dataSourceContainer.isConnected()) {
            if (!ArrayUtils.isEmpty(Job.getJobManager().find(dataSourceDescriptor))) {
                // Already connecting/disconnecting - just return
                return;
            }

            ConnectionFeatures.CONNECTION_CLOSE.use(Map.of(
                "driver", dataSourceContainer.getDriver().getPreconfiguredId(),
                "origin", dataSourceContainer.getOrigin().getFullType()
            ));

            final DisconnectJob disconnectJob = new DisconnectJob(dataSourceDescriptor);
            disconnectJob.addJobChangeListener(new JobChangeAdapter() {
                @Override
                public void done(IJobChangeEvent event) {
                    IStatus result = disconnectJob.getConnectStatus();
                    if (onFinish != null) {
                        onFinish.run();
                    } else if (result != null && !result.isOK()) {
                        DBWorkbench.getPlatformUI().showError(disconnectJob.getName(), null, result);
                    }
                    //DataSourcePropertyTester.firePropertyChange(DataSourcePropertyTester.PROP_CONNECTED);
                }
            });
            disconnectJob.schedule();
        }
    }

    public static void reconnectDataSource(final DBRProgressMonitor monitor, final DBPDataSourceContainer dataSourceContainer) {
        disconnectDataSource(dataSourceContainer, () ->
            connectToDataSource(monitor, dataSourceContainer, null));
    }

    public static boolean checkAndCloseActiveTransaction(DBPDataSourceContainer container, boolean isReconnect) {
        return checkAndCloseActiveTransaction(container, isReconnect, false);
    }

    public static boolean checkAndCloseActiveTransaction(DBPDataSourceContainer container, boolean isReconnect, boolean forceRollback) {
        try {
            DBPDataSource dataSource = container.getDataSource();
            if (dataSource == null) {
                return true;
            }

            for (DBSInstance instance : dataSource.getAvailableInstances()) {
                if (!checkAndCloseActiveTransaction(instance.getAllContexts(), isReconnect, forceRollback)) {
                    return false;
                }
            }
        } catch (Throwable e) {
            log.debug(e);
        }
        return true;
    }

    public static boolean checkAndCloseActiveTransaction(DBCExecutionContext[] contexts, boolean isReconnect) {
        return checkAndCloseActiveTransaction(contexts, isReconnect, false);
    }

    public static boolean checkAndCloseActiveTransaction(DBCExecutionContext[] contexts, boolean isReconnect, boolean forceRollback) {
        if (contexts == null) {
            return true;
        }

        Boolean commitTxn = null;
        for (final DBCExecutionContext context : contexts) {
            // First rollback active transaction
            try {
                if (QMUtils.isTransactionActive(context)) {
                    if (!forceRollback && commitTxn == null) {
                        // Ask for confirmation
                        TransactionCloseConfirmer closeConfirmer = new TransactionCloseConfirmer(
                            context.getDataSource().getContainer().getName() + " (" + context.getContextName() + ")",
                            isReconnect
                        );
                        UIUtils.syncExec(closeConfirmer);
                        switch (closeConfirmer.result) {
                            case IDialogConstants.YES_ID -> commitTxn = true;
                            case IDialogConstants.NO_ID -> commitTxn = false;
                            default -> {
                                return false;
                            }
                        }
                    }
                    final boolean commit = !forceRollback && commitTxn;
                    UIUtils.runInProgressService(monitor -> closeActiveTransaction(monitor, context, commit));
                }
            } catch (Throwable e) {
                log.warn("Can't rollback active transaction before disconnect", e);
            }
        }
        return true;
    }

/*
    public static int checkActiveTransaction(DBCExecutionContext context)
    {
        // First rollback active transaction
        if (isContextTransactionAffected(context)) {
            // Ask for confirmation
            TransactionCloseConfirmer closeConfirmer = new TransactionCloseConfirmer(context.getDataSource().getContainer().getName());
            DBeaverUI.syncExec(closeConfirmer);
            switch (closeConfirmer.result) {
                case IDialogConstants.YES_ID:
                    return ISaveablePart2.YES;
                case IDialogConstants.NO_ID:
                    return ISaveablePart2.NO;
                default:
                    return ISaveablePart2.CANCEL;
            }
        }
        return ISaveablePart2.YES;
    }
*/

    public static void closeActiveTransaction(DBRProgressMonitor monitor, DBCExecutionContext context, boolean commitTxn) {
        monitor.beginTask("Close active transaction", 1);
        try (DBCSession session = context.openSession(monitor, DBCExecutionPurpose.UTIL, "End active transaction")) {
            // Disable logging to avoid commit mode recovery and other UI callbacks
            session.enableLogging(false);
            monitor.subTask("End active transaction");
            EndTransactionTask task = new EndTransactionTask(session, commitTxn);
            RuntimeUtils.runTask(task, "Close active transactions", END_TRANSACTION_WAIT_TIME);
        } finally {
            monitor.done();
        }
    }

    public static boolean confirmTransactionsClose(DBCExecutionContext[] contexts) {
        if (contexts.length == 0) {
            return false;
        }
        TransactionEndConfirmer closeConfirmer = new TransactionEndConfirmer(contexts[0].getDataSource());
        UIUtils.syncExec(closeConfirmer);
        return closeConfirmer.result;
    }

    private static class EndTransactionTask implements DBRRunnableWithProgress {
        private final DBCSession session;
        private final boolean commit;

        private EndTransactionTask(DBCSession session, boolean commit) {
            this.session = session;
            this.commit = commit;
        }

        @Override
        public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
            DBCTransactionManager txnManager = DBUtils.getTransactionManager(session.getExecutionContext());
            if (txnManager != null) {
                try {
                    if (commit) {
                        txnManager.commit(session);
                    } else {
                        txnManager.rollback(session, null);
                    }
                } catch (DBCException e) {
                    throw new InvocationTargetException(e);
                }
            }
        }
    }

    private static class TransactionCloseConfirmer implements Runnable {
        final String name;
        final boolean isReconnect;
        int result = IDialogConstants.NO_ID;

        private TransactionCloseConfirmer(String name, boolean isReconnect) {
            this.name = name;
            this.isReconnect = isReconnect;
        }

        @Override
        public void run() {
            result = ConfirmationDialog.confirmAction(
                null,
                this.isReconnect ? ConnectionPreferences.CONFIRM_TXN_RECONNECT : ConnectionPreferences.CONFIRM_TXN_DISCONNECT,
                ConfirmationDialog.QUESTION_WITH_CANCEL,
                name);
        }
    }

    private static class TransactionEndConfirmer implements Runnable {
        final DBPDataSource dataSource;
        boolean result;

        private TransactionEndConfirmer(DBPDataSource dataSource) {
            this.dataSource = dataSource;
        }

        @Override
        public void run() {
            Display.getCurrent().beep();
            TransactionEndConfirmDialog dialog = new TransactionEndConfirmDialog(dataSource);
            result = dialog.open() == IDialogConstants.OK_ID;
        }
    }

    static class TransactionEndConfirmDialog extends MessageDialog {

        private int countdown = 10;

        public TransactionEndConfirmDialog(DBPDataSource dataSource) {
            super(UIUtils.getActiveShell(),
                "End transaction",
                DBeaverIcons.getImage(UIIcon.TXN_ROLLBACK),
                "Transactions in database '" + dataSource.getName() + "' will be ended because of the long idle period." +
                    "\nPress '" + IDialogConstants.CANCEL_LABEL + "' to prevent this.",
                MessageDialog.WARNING,
                null,
                0);
        }

        @Override
        public int open() {
            UIJob countdownJob = new UIJob("Confirmation countdown") {
                {
                    setUser(false);
                    setSystem(true);
                }
                @Override
                public IStatus runInUIThread(IProgressMonitor monitor) {
                    Shell shell = getShell();
                    if (shell == null || shell.isDisposed()) {
                        return Status.OK_STATUS;
                    }
                    Button okButton = getButton(IDialogConstants.OK_ID);
                    if (okButton != null) {
                        okButton.setText(IDialogConstants.OK_LABEL + " (" + countdown + ")");
                    }

                    countdown--;
                    if (countdown <= 0) {
                        UIUtils.asyncExec(() -> close());
                    } else {
                        schedule(1000);
                    }
                    return Status.OK_STATUS;
                }
            };
            countdownJob.schedule(100);

            return super.open();
        }

        @Override
        protected void createButtonsForButtonBar(Composite parent) {
            Button okButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, false);
            Button cancelButton = createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, true);
            setButtons(okButton, cancelButton);
        }
    }

}
