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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPErrorAssistant;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.net.DBWForwarder;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.DBWHandlerType;
import org.jkiss.dbeaver.model.net.DBWNetworkHandler;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableParametrized;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.jobs.InvalidateJob;
import org.jkiss.dbeaver.runtime.net.GlobalProxyAuthenticator;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.net.Authenticator;
import java.util.ArrayList;
import java.util.List;

/**
 * Execution utils
 */
public class DBExecUtils {

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
                log.debug("Invalidate datasource '" + dataSource.getContainer().getName() + "' connections...");
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
                        InvalidateJob.invalidateTransaction(monitor, dataSource);
                    } else {
                        // Do not recover if connection was canceled
                        InvalidateJob.invalidateDataSource(monitor, dataSource, false,
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
}