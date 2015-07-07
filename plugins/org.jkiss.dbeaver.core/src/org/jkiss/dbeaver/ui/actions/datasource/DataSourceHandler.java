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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISaveablePart;
import org.eclipse.ui.ISaveablePart2;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceUser;
import org.jkiss.dbeaver.model.DBPEvent;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.access.DBAAuthInfo;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.qm.QMMCollector;
import org.jkiss.dbeaver.model.qm.meta.QMMSessionInfo;
import org.jkiss.dbeaver.model.qm.meta.QMMTransactionInfo;
import org.jkiss.dbeaver.model.qm.meta.QMMTransactionSavepointInfo;
import org.jkiss.dbeaver.model.runtime.DBRProgressListener;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.runtime.jobs.ConnectJob;
import org.jkiss.dbeaver.runtime.jobs.DisconnectJob;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;
import org.jkiss.utils.ArrayUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

public class DataSourceHandler
{
    static final Log log = Log.getLog(DataSourceHandler.class);

    public static final int END_TRANSACTION_WAIT_TIME = 3000;

    /**
     * Connects datasource
     * @param monitor progress monitor or null. If nul then new job will be started
     * @param dataSourceContainer    container to connect
     * @param onFinish               finish handler
     */
    public static void connectToDataSource(
        @Nullable DBRProgressMonitor monitor,
        @NotNull DBSDataSourceContainer dataSourceContainer,
        @Nullable final DBRProgressListener onFinish)
    {
        if (dataSourceContainer instanceof DataSourceDescriptor && !dataSourceContainer.isConnected()) {
            final DataSourceDescriptor dataSourceDescriptor = (DataSourceDescriptor)dataSourceContainer;
            if (!ArrayUtils.isEmpty(Job.getJobManager().find(dataSourceDescriptor))) {
                // Already connecting/disconnecting - just return
                return;
            }
            final String oldName = dataSourceDescriptor.getConnectionConfiguration().getUserName();
            final String oldPassword = dataSourceDescriptor.getConnectionConfiguration().getUserPassword();
            if (!dataSourceDescriptor.isSavePassword()) {
                // Ask for password
                if (!askForPassword(dataSourceDescriptor, null)) {
                    updateDataSourceObject(dataSourceDescriptor);
                    return;
                }
            }
            for (DBWHandlerConfiguration handler : dataSourceDescriptor.getConnectionConfiguration().getDeclaredHandlers()) {
                if (handler.isEnabled() && handler.isSecured() && !handler.isSavePassword()) {
                    if (!askForPassword(dataSourceDescriptor, handler)) {
                        updateDataSourceObject(dataSourceDescriptor);
                        return;
                    }
                }
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
                        DBUserInterface.getInstance().showError(
                            connectJob.getName(),
                            null,//NLS.bind(CoreMessages.runtime_jobs_connect_status_error, dataSourceContainer.getName()),
                            result);
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
                Display.getDefault().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        connectJob.schedule();
                    }
                });
            }
        }
    }

    private static void updateDataSourceObject(DataSourceDescriptor dataSourceDescriptor)
    {
        dataSourceDescriptor.getRegistry().fireDataSourceEvent(
            DBPEvent.Action.OBJECT_UPDATE,
            dataSourceDescriptor,
            false);
    }

    public static boolean askForPassword(@NotNull final DataSourceDescriptor dataSourceContainer, @Nullable final DBWHandlerConfiguration networkHandler)
    {
        String prompt = networkHandler != null ?
            NLS.bind(CoreMessages.dialog_connection_auth_title_for_handler, networkHandler.getTitle()) :
            "'" + dataSourceContainer.getName() + CoreMessages.dialog_connection_auth_title; //$NON-NLS-1$
        String user = networkHandler != null ? networkHandler.getUserName() : dataSourceContainer.getConnectionConfiguration().getUserName();
        String password = networkHandler != null ? networkHandler.getPassword() : dataSourceContainer.getConnectionConfiguration().getUserPassword();

        DBAAuthInfo authInfo = DBUserInterface.getInstance().promptUserCredentials(prompt, user, password);
        if (authInfo == null) {
            return false;
        }

        if (networkHandler != null) {
            networkHandler.setUserName(authInfo.getUserName());
            networkHandler.setPassword(authInfo.getUserPassword());
            networkHandler.setSavePassword(authInfo.isSavePassword());
        } else {
            dataSourceContainer.getConnectionConfiguration().setUserName(authInfo.getUserName());
            dataSourceContainer.getConnectionConfiguration().setUserPassword(authInfo.getUserPassword());
            dataSourceContainer.setSavePassword(authInfo.isSavePassword());
        }
        if (dataSourceContainer.isSavePassword()) {
            // Update connection properties
            dataSourceContainer.getRegistry().updateDataSource(dataSourceContainer);
        }

        return true;
    }

    public static void disconnectDataSource(DBSDataSourceContainer dataSourceContainer, @Nullable final Runnable onFinish) {

        // Save users
        for (DBPDataSourceUser user : dataSourceContainer.getUsers()) {
            if (user instanceof ISaveablePart) {
                if (!UIUtils.validateAndSave(VoidProgressMonitor.INSTANCE, (ISaveablePart) user)) {
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
                        UIUtils.showErrorDialog(
                            null,
                            disconnectJob.getName(),
                            null,
                            result);
                    }
                }
            });
            disconnectJob.schedule();
        }
    }

    public static boolean checkAndCloseActiveTransaction(DBSDataSourceContainer container) {
        DBPDataSource dataSource = container.getDataSource();
        if (dataSource == null) {
            return true;
        }

        return checkAndCloseActiveTransaction(container, dataSource.getAllContexts());
    }

    public static boolean checkAndCloseActiveTransaction(DBSDataSourceContainer container, Collection<? extends DBCExecutionContext> contexts)
    {
        if (container.getDataSource() == null) {
            return true;
        }

        Boolean commitTxn = null;
        for (final DBCExecutionContext context : contexts) {
            // First rollback active transaction
            try {
                if (isContextTransactionAffected(context)) {
                    if (commitTxn == null) {
                        // Ask for confirmation
                        TransactionCloseConfirmer closeConfirmer = new TransactionCloseConfirmer(container.getName());
                        UIUtils.runInUI(null, closeConfirmer);
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
                    DBeaverUI.runInProgressService(new DBRRunnableWithProgress() {
                        @Override
                        public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                            closeActiveTransaction(monitor, context, commit);
                        }
                    });
                }
            } catch (Throwable e) {
                log.warn("Can't rollback active transaction before disconnect", e);
            }
        }
        return true;
    }

    public static int checkActiveTransaction(DBCExecutionContext context)
    {
        // First rollback active transaction
        if (isContextTransactionAffected(context)) {
            // Ask for confirmation
            TransactionCloseConfirmer closeConfirmer = new TransactionCloseConfirmer(context.getDataSource().getContainer().getName());
            UIUtils.runInUI(null, closeConfirmer);
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

    public static void closeActiveTransaction(DBRProgressMonitor monitor, DBCExecutionContext context, boolean commitTxn) {
        DBCSession session = context.openSession(monitor, DBCExecutionPurpose.UTIL, "End active transaction");
        try {
            monitor.subTask("End active transaction");
            EndTransactionTask task = new EndTransactionTask(session, commitTxn);
            RuntimeUtils.runTask(task, END_TRANSACTION_WAIT_TIME);
        } finally {
            session.close();
        }
    }

    public static boolean isContextTransactionAffected(DBCExecutionContext context) {
        DBCTransactionManager txnManager = DBUtils.getTransactionManager(context);
        if (txnManager == null) {
            return false;
        }
        try {
            if (txnManager.isAutoCommit()) {
                return false;
            }
        } catch (DBCException e) {
            log.warn(e);
            return false;
        }

        // If there are some executions in last savepoint then ask user about commit/rollback
        QMMCollector qmm = DBeaverCore.getInstance().getQueryManager().getMetaCollector();
        if (qmm != null) {
            QMMSessionInfo qmmSession = qmm.getSessionInfo(context);
            QMMTransactionInfo txn = qmmSession == null ? null : qmmSession.getTransaction();
            QMMTransactionSavepointInfo sp = txn == null ? null : txn.getCurrentSavepoint();
            if (sp != null && (sp.getPrevious() != null || sp.getLastExecute() != null)) {
                return true;
/*
                boolean hasUserExec = false;
                if (true) {
                    // Do not check whether we have user queries, just ask for confirmation
                    hasUserExec = true;
                } else {
                    for (QMMTransactionSavepointInfo psp = sp; psp != null; psp = psp.getPrevious()) {
                        if (psp.hasUserExecutions()) {
                            hasUserExec = true;
                            break;
                        }
                    }
                }
*/
            }
        }
        return false;
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
                null,
                DBeaverPreferences.CONFIRM_TXN_DISCONNECT,
                ConfirmationDialog.QUESTION_WITH_CANCEL,
                name);
        }
    }
}
