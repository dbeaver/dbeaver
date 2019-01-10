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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.ISaveablePart;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.access.DBAAuthInfo;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
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
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;
import org.jkiss.dbeaver.ui.editors.entity.handlers.SaveChangesHandler;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;

import java.lang.reflect.InvocationTargetException;

public class DataSourceHandler
{
    private static final Log log = Log.getLog(DataSourceHandler.class);

    public static final int END_TRANSACTION_WAIT_TIME = 3000;

    /**
     * Connects datasource
     * @param monitor progress monitor or null. If nul then new job will be started
     * @param dataSourceContainer    container to connect
     * @param onFinish               finish handler
     */
    public static void connectToDataSource(
        @Nullable DBRProgressMonitor monitor,
        @NotNull DBPDataSourceContainer dataSourceContainer,
        @Nullable final DBRProgressListener onFinish)
    {
        if (dataSourceContainer instanceof DataSourceDescriptor && !dataSourceContainer.isConnected()) {
            final DataSourceDescriptor dataSourceDescriptor = (DataSourceDescriptor)dataSourceContainer;
            if (!ArrayUtils.isEmpty(Job.getJobManager().find(dataSourceDescriptor))) {
                // Already connecting/disconnecting - just return
                return;
            }

            final ConnectJob connectJob = new ConnectJob(dataSourceDescriptor);
            final JobChangeAdapter jobChangeAdapter = new JobChangeAdapter() {
                @Override
                public void done(IJobChangeEvent event)
                {
                    IStatus result = connectJob.getConnectStatus();
                    if (result.isOK()) {
                        if (!dataSourceDescriptor.isSavePassword()) {
                            // Rest password back to null
                            // TODO: to be correct we need to reset password info.
                            // but we need a password to open isolated contexts (e.g. for data export)
                            // Currently it is not possible to ask for password from isolation context opening
                            // procedure. We need to do something here...
                            //dataSourceDescriptor.getConnectionConfiguration().setUserName(oldName);
                            //dataSourceDescriptor.getConnectionConfiguration().setUserPassword(oldPassword);
                        }
                    }
                    if (onFinish != null) {
                        onFinish.onTaskFinished(result);
                    } else if (!result.isOK()) {
                        UIUtils.asyncExec(() -> DBWorkbench.getPlatformUI().showError(
                            connectJob.getName(),
                            null,//NLS.bind(CoreMessages.runtime_jobs_connect_status_error, dataSourceContainer.getName()),
                            result));
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
                UIUtils.asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        connectJob.schedule();
                    }
                });
            }
        }
    }

    public static void updateDataSourceObject(DataSourceDescriptor dataSourceDescriptor)
    {
        dataSourceDescriptor.getRegistry().notifyDataSourceListeners(new DBPEvent(
            DBPEvent.Action.OBJECT_UPDATE,
            dataSourceDescriptor,
            false));
    }

    public static boolean askForPassword(@NotNull final DataSourceDescriptor dataSourceContainer, @Nullable final DBWHandlerConfiguration networkHandler, final boolean passwordOnly)
    {
        final String prompt = networkHandler != null ?
            NLS.bind(CoreMessages.dialog_connection_auth_title_for_handler, networkHandler.getTitle()) :
            "'" + dataSourceContainer.getName() + CoreMessages.dialog_connection_auth_title; //$NON-NLS-1$
        final String user = networkHandler != null ? networkHandler.getUserName() : dataSourceContainer.getConnectionConfiguration().getUserName();
        final String password = networkHandler != null ? networkHandler.getPassword() : dataSourceContainer.getConnectionConfiguration().getUserPassword();

        DBAAuthInfo authInfo = new UITask<DBAAuthInfo>() {
            @Override
            protected DBAAuthInfo runTask() {
                return DBWorkbench.getPlatformUI().promptUserCredentials(prompt, user, password, passwordOnly, !dataSourceContainer.isTemporary());
            }
        }.execute();
        if (authInfo == null) {
            return false;
        }

        if (networkHandler != null) {
            if (!passwordOnly) {
                networkHandler.setUserName(authInfo.getUserName());
            }
            networkHandler.setPassword(authInfo.getUserPassword());
            networkHandler.setSavePassword(authInfo.isSavePassword());
        } else {
            if (!passwordOnly) {
                dataSourceContainer.getConnectionConfiguration().setUserName(authInfo.getUserName());
            }
            dataSourceContainer.getConnectionConfiguration().setUserPassword(authInfo.getUserPassword());
            dataSourceContainer.setSavePassword(authInfo.isSavePassword());
        }
        if (authInfo.isSavePassword()) {
            // Update connection properties
            dataSourceContainer.getRegistry().updateDataSource(dataSourceContainer);
        }

        return true;
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
        if (!checkAndCloseActiveTransaction(dataSourceContainer)) {
            return;
        }

        if (dataSourceContainer instanceof DataSourceDescriptor && dataSourceContainer.isConnected()) {
            final DataSourceDescriptor dataSourceDescriptor = (DataSourceDescriptor)dataSourceContainer;
            if (!ArrayUtils.isEmpty(Job.getJobManager().find(dataSourceDescriptor))) {
                // Already connecting/disconnecting - just return
                return;
            }
            final DisconnectJob disconnectJob = new DisconnectJob(dataSourceDescriptor);
            disconnectJob.addJobChangeListener(new JobChangeAdapter() {
                @Override
                public void done(IJobChangeEvent event)
                {
                    IStatus result = disconnectJob.getConnectStatus();
                    if (onFinish != null) {
                        onFinish.run();
                    } else if (!result.isOK()) {
                        DBWorkbench.getPlatformUI().showError(
                                disconnectJob.getName(),
                            null,
                            result);
                    }
                }
            });
            // Run in UI thread to update actions (some Eclipse magic)
            UIUtils.asyncExec(disconnectJob::schedule);
        }
    }

    public static void reconnectDataSource(final DBRProgressMonitor monitor, final DBPDataSourceContainer dataSourceContainer) {
        disconnectDataSource(dataSourceContainer, new Runnable() {
            @Override
            public void run() {
                connectToDataSource(monitor, dataSourceContainer, null);
            }
        });
    }

    public static boolean checkAndCloseActiveTransaction(DBPDataSourceContainer container) {
        DBPDataSource dataSource = container.getDataSource();
        if (dataSource == null) {
            return true;
        }

        for (DBSInstance instance : dataSource.getAvailableInstances()) {
            if (!checkAndCloseActiveTransaction(instance.getAllContexts())) {
                return false;
            }
        }
        return true;
    }

    public static boolean checkAndCloseActiveTransaction(DBCExecutionContext[] contexts)
    {
        if (contexts == null) {
            return true;
        }

        Boolean commitTxn = null;
        for (final DBCExecutionContext context : contexts) {
            // First rollback active transaction
            try {
                if (QMUtils.isTransactionActive(context)) {
                    if (commitTxn == null) {
                        // Ask for confirmation
                        TransactionCloseConfirmer closeConfirmer = new TransactionCloseConfirmer(context.getDataSource().getContainer().getName());
                        UIUtils.syncExec(closeConfirmer);
                        switch (closeConfirmer.result) {
                            case IDialogConstants.YES_ID:
                                commitTxn = true;
                                break;
                            case IDialogConstants.NO_ID:
                                commitTxn = false;
                                break;
                            default:
                                return false;
                        }
                    }
                    final boolean commit = commitTxn;
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
        try (DBCSession session = context.openSession(monitor, DBCExecutionPurpose.UTIL, "End active transaction")) {
            monitor.subTask("End active transaction");
            EndTransactionTask task = new EndTransactionTask(session, commitTxn);
            RuntimeUtils.runTask(task, "Close active transactions", END_TRANSACTION_WAIT_TIME);
        }
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
        int result = IDialogConstants.NO_ID;

        private TransactionCloseConfirmer(String name) {
            this.name = name;
        }

        @Override
        public void run()
        {
            result = ConfirmationDialog.showConfirmDialog(
                DBeaverActivator.getCoreResourceBundle(),
                null,
                DBeaverPreferences.CONFIRM_TXN_DISCONNECT,
                ConfirmationDialog.QUESTION_WITH_CANCEL,
                name);
        }
    }
}
