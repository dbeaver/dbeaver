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
package org.jkiss.dbeaver.model.exec;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPErrorAssistant;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistActionComment;
import org.jkiss.dbeaver.model.net.DBWForwarder;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.DBWHandlerType;
import org.jkiss.dbeaver.model.net.DBWNetworkHandler;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableParametrized;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.jobs.InvalidateJob;
import org.jkiss.dbeaver.runtime.net.GlobalProxyAuthenticator;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.net.Authenticator;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Execution utils
 */
public class DBExecUtils {

    public static final int DEFAULT_READ_FETCH_SIZE = 10000;

    private static final Log log = Log.getLog(DBExecUtils.class);

    /**
     * Current execution context. Used by global authenticators and network handlers
     */
    private static final ThreadLocal<DBPDataSourceContainer> ACTIVE_CONTEXT = new ThreadLocal<>();
    private static final List<DBPDataSourceContainer> ACTIVE_CONTEXTS = new ArrayList<>();

    public static DBPDataSourceContainer getCurrentThreadContext() {
        return ACTIVE_CONTEXT.get();
    }

    public static List<DBPDataSourceContainer> getActiveContexts() {
        synchronized (ACTIVE_CONTEXTS) {
            return new ArrayList<>(ACTIVE_CONTEXTS);
        }
    }

    public static void startContextInitiation(DBPDataSourceContainer context) {
        ACTIVE_CONTEXT.set(context);
        synchronized (ACTIVE_CONTEXTS) {
            ACTIVE_CONTEXTS.add(context);
        }
        // Set proxy auth (if required)
        // Note: authenticator may be changed by Eclipse frameword on startup or later.
        // That's why we set new default authenticator on connection initiation
        boolean hasProxy = false;
        for (DBWHandlerConfiguration handler : context.getConnectionConfiguration().getHandlers()) {
            if (handler.isEnabled() && handler.getType() == DBWHandlerType.PROXY) {
                hasProxy = true;
                break;
            }
        }
        if (hasProxy) {
            Authenticator.setDefault(new GlobalProxyAuthenticator());
        }
    }

    public static void finishContextInitiation(DBPDataSourceContainer context) {
        ACTIVE_CONTEXT.remove();
        synchronized (ACTIVE_CONTEXTS) {
            ACTIVE_CONTEXTS.remove(context);
        }
    }

    public static DBPDataSourceContainer findConnectionContext(String host, int port, String path) {
        DBPDataSourceContainer curContext = getCurrentThreadContext();
        if (curContext != null) {
            return contextMatches(host, port, curContext) ? curContext : null;
        }
        synchronized (ACTIVE_CONTEXTS) {
            for (DBPDataSourceContainer ctx : ACTIVE_CONTEXTS) {
                if (contextMatches(host, port, ctx)) {
                    return ctx;
                }
            }
        }
        return null;
    }

    private static boolean contextMatches(String host, int port, DBPDataSourceContainer ctx) {
        DBPConnectionConfiguration cfg = ctx.getConnectionConfiguration();
        if (CommonUtils.equalObjects(cfg.getHostName(), host) && String.valueOf(port).equals(cfg.getHostPort())) {
            return true;
        }
        for (DBWNetworkHandler networkHandler : ctx.getActiveNetworkHandlers()) {
            if (networkHandler instanceof DBWForwarder && ((DBWForwarder) networkHandler).matchesParameters(host, port)) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    public static DBPErrorAssistant.ErrorType discoverErrorType(@NotNull DBPDataSource dataSource, @NotNull Throwable error) {
        DBPErrorAssistant errorAssistant = DBUtils.getAdapter(DBPErrorAssistant.class, dataSource);
        if (errorAssistant != null) {
            return ((DBPErrorAssistant) dataSource).discoverErrorType(error);
        }

        return DBPErrorAssistant.ErrorType.NORMAL;
    }

    /**
     * @param param DBRProgressProgress monitor or DBCSession
     *
     */
    public static <T> boolean tryExecuteRecover(@NotNull T param, @NotNull DBPDataSource dataSource, @NotNull DBRRunnableParametrized<T> runnable) throws DBException {
        int tryCount = 1;
        boolean recoverEnabled = dataSource.getContainer().getPreferenceStore().getBoolean(ModelPreferences.EXECUTE_RECOVER_ENABLED);
        if (recoverEnabled) {
            tryCount += dataSource.getContainer().getPreferenceStore().getInt(ModelPreferences.EXECUTE_RECOVER_RETRY_COUNT);
        }
        Throwable lastError = null;
        for (int i = 0; i < tryCount; i++) {
            try {
                runnable.run(param);
                lastError = null;
                break;
            } catch (InvocationTargetException e) {
                lastError = e.getTargetException();
                if (!recoverEnabled) {
                    // Can't recover
                    break;
                }
                DBPErrorAssistant.ErrorType errorType = discoverErrorType(dataSource, lastError);
                if (errorType != DBPErrorAssistant.ErrorType.TRANSACTION_ABORTED && errorType != DBPErrorAssistant.ErrorType.CONNECTION_LOST) {
                    // Some other error
                    break;
                }
                DBRProgressMonitor monitor;
                if (param instanceof DBRProgressMonitor) {
                    monitor = (DBRProgressMonitor) param;
                } else if (param instanceof DBCSession) {
                    monitor = ((DBCSession) param).getProgressMonitor();
                } else {
                    monitor = new VoidProgressMonitor();
                }
                if (!monitor.isCanceled()) {

                    if (errorType == DBPErrorAssistant.ErrorType.TRANSACTION_ABORTED) {
                        // Transaction aborted
                        DBCExecutionContext executionContext = null;
                        if (lastError instanceof DBCException) {
                            executionContext = ((DBCException) lastError).getExecutionContext();
                        }
                        if (executionContext != null) {
                            log.debug("Invalidate context [" + executionContext.getDataSource().getContainer().getName() + "/" + executionContext.getContextName() + "] transactions");
                        } else {
                            log.debug("Invalidate datasource [" + dataSource.getContainer().getName() + "] transactions");
                        }
                        InvalidateJob.invalidateTransaction(monitor, dataSource, executionContext);
                    } else {
                        // Do not recover if connection was canceled
                        log.debug("Invalidate datasource '" + dataSource.getContainer().getName() + "' connections...");
                        InvalidateJob.invalidateDataSource(
                            monitor,
                            dataSource,
                            false,
                            true,
                            () -> DBWorkbench.getPlatformUI().openConnectionEditor(dataSource.getContainer()));
                        if (i < tryCount - 1) {
                            log.error("Operation failed. Retry count remains = " + (tryCount - i - 1), lastError);
                        }
                    }
                }
            } catch (InterruptedException e) {
                log.error("Operation interrupted");
                return false;
            }
        }
        if (lastError != null) {
            if (lastError instanceof DBException) {
                throw (DBException) lastError;
            } else {
                throw new DBException(lastError, dataSource);
            }
        }
        return true;
    }

    public static void setStatementFetchSize(DBCStatement dbStat, long firstRow, long maxRows, int fetchSize) {
        boolean useFetchSize = fetchSize > 0 || dbStat.getSession().getDataSource().getContainer().getPreferenceStore().getBoolean(ModelPreferences.RESULT_SET_USE_FETCH_SIZE);
        if (useFetchSize) {
            if (fetchSize <= 0) {
                fetchSize = DEFAULT_READ_FETCH_SIZE;
            }
            try {
                dbStat.setResultsFetchSize(
                    firstRow < 0 || maxRows <= 0 ? fetchSize : (int) (firstRow + maxRows));
            } catch (Exception e) {
                log.warn(e);
            }
        }
    }

    public static void executeScript(DBRProgressMonitor monitor, DBCExecutionContext executionContext, String jobName, List<DBEPersistAction> persistActions) {
        boolean ignoreErrors = false;
        monitor.beginTask(jobName, persistActions.size());
        try (DBCSession session = executionContext.openSession(monitor, DBCExecutionPurpose.UTIL, jobName)) {
            for (DBEPersistAction action : persistActions) {
                if (monitor.isCanceled()) {
                    break;
                }
                monitor.subTask(action.getTitle());
                try {
                    if (action instanceof SQLDatabasePersistActionComment) {
                        continue;
                    }
                    String script = action.getScript();
                    if (!CommonUtils.isEmpty(script)) {
                        try (final Statement statement = ((JDBCSession) session).createStatement()) {
                            statement.execute(script);
                        }
                    }
                } catch (Exception e) {
                    log.debug("Error executing query", e);
                    if (ignoreErrors) {
                        continue;
                    }
                    boolean keepRunning = true;
                    switch (DBWorkbench.getPlatformUI().showErrorStopRetryIgnore(jobName, e, true)) {
                        case STOP:
                            keepRunning = false;
                            break;
                        case RETRY:
                            // just make it again
                            continue;
                        case IGNORE:
                            // Just do nothing
                            break;
                        case IGNORE_ALL:
                            ignoreErrors = true;
                            break;
                    }
                    if (!keepRunning) {
                        break;
                    }
                } finally {
                    monitor.worked(1);
                }
            }
        } finally {
            monitor.done();
        }
    }

    public static void checkSmartAutoCommit(DBCSession session, String queryText) {
        DBCTransactionManager txnManager = DBUtils.getTransactionManager(session.getExecutionContext());
        if (txnManager != null) {
            try {
                if (!txnManager.isAutoCommit()) {
                    return;
                }

                SQLDialect sqlDialect = SQLUtils.getDialectFromDataSource(session.getDataSource());
                if (!sqlDialect.isTransactionModifyingQuery(queryText)) {
                    return;
                }

                if (txnManager.isAutoCommit()) {
                    txnManager.setAutoCommit(session.getProgressMonitor(), false);
                }
            } catch (DBCException e) {
                log.warn(e);
            }
        }
    }

    public static void setExecutionContextDefaults(DBRProgressMonitor monitor, DBPDataSource dataSource, DBCExecutionContext executionContext, @Nullable String newInstanceName, @Nullable String curInstanceName, @Nullable String newObjectName) throws DBException {
        DBSObjectContainer rootContainer = DBUtils.getAdapter(DBSObjectContainer.class, dataSource);
        if (rootContainer == null) {
            return;
        }

        DBCExecutionContextDefaults contextDefaults = null;
        if (executionContext != null) {
            contextDefaults = executionContext.getContextDefaults();
        }
        if (contextDefaults != null && (contextDefaults.supportsSchemaChange() || contextDefaults.supportsCatalogChange())) {
            changeDefaultObject(monitor, rootContainer, contextDefaults, newInstanceName, curInstanceName, newObjectName);
        }
    }

    @SuppressWarnings("unchecked")
    public static void changeDefaultObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSObjectContainer rootContainer,
        @NotNull DBCExecutionContextDefaults contextDefaults,
        @Nullable String newCatalogName,
        @Nullable String curCatalogName,
        @Nullable String newObjectName) throws DBException
    {
        DBSCatalog newCatalog = null;
        DBSSchema newSchema = null;

        if (newCatalogName != null) {
            DBSObject newInstance = rootContainer.getChild(monitor, newCatalogName);
            if (newInstance instanceof DBSCatalog) {
                newCatalog = (DBSCatalog) newInstance;
            }
        }
        DBSObject newObject;
        if (newObjectName != null) {
            if (newCatalog == null) {
                newObject = rootContainer.getChild(monitor, newObjectName);
            } else {
                newObject = newCatalog.getChild(monitor, newObjectName);
            }
            if (newObject instanceof DBSSchema) {
                newSchema = (DBSSchema) newObject;
            } else if (newObject instanceof DBSCatalog) {
                newCatalog = (DBSCatalog) newObject;
            }
        }

        boolean changeCatalog = (curCatalogName != null ? !CommonUtils.equalObjects(curCatalogName, newCatalogName) : newCatalog != null);

        if (newCatalog != null && newSchema != null && changeCatalog) {
            contextDefaults.setDefaultCatalog(monitor, newCatalog, newSchema);
        } else if (newSchema != null) {
            contextDefaults.setDefaultSchema(monitor, newSchema);
        } else if (newCatalog != null && changeCatalog) {
            contextDefaults.setDefaultCatalog(monitor, newCatalog, null);
        }
    }

    public static void recoverSmartCommit(DBCExecutionContext executionContext) {
        DBPPreferenceStore preferenceStore = executionContext.getDataSource().getContainer().getPreferenceStore();
        if (preferenceStore.getBoolean(ModelPreferences.TRANSACTIONS_SMART_COMMIT) && preferenceStore.getBoolean(ModelPreferences.TRANSACTIONS_SMART_COMMIT_RECOVER)) {
            DBCTransactionManager transactionManager = DBUtils.getTransactionManager(executionContext);
            if (transactionManager != null) {
                new AbstractJob("Recover smart commit mode") {
                    @Override
                    protected IStatus run(DBRProgressMonitor monitor) {
                        try {
                            if (!transactionManager.isAutoCommit()) {
                                transactionManager.setAutoCommit(monitor,true);
                            }
                        } catch (DBCException e) {
                            return GeneralUtils.makeExceptionStatus(e);
                        }
                        return Status.OK_STATUS;
                    }
                }.schedule();
            }
        }
    }

}