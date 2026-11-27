/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.datadam;

import com.dbeaver.datadam.dam.remote.api.model.DDConnectedRemoteDam;
import com.dbeaver.datadam.dam.remote.api.model.DDRemoteDamSessionInfo;
import com.dbeaver.datadam.dam.remote.api.transport.DDRemoteDamSubscription;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.datadam.DDAccountClient;
import org.jkiss.dbeaver.model.datadam.DDEndpoints;
import org.jkiss.dbeaver.model.datadam.DDProductConfig;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DBDamAccessService;
import org.jkiss.dbeaver.model.datadam.rpc.DBDamNameService;
import org.jkiss.dbeaver.model.datadam.rpc.DBDamSessionService;
import org.jkiss.dbeaver.model.datadam.rpc.DBDamSettingsStore;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class DBDamService {
    private static final long CONNECTION_WAIT_TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(45);

    @Nullable
    private static DBDamService instance;

    private final DDAccountClient account = new DDAccountClient(
        URI.create(DDEndpoints.getAccountBaseUrl()), DBDamAccessService.getInstance());
    @Nullable
    private volatile RenamePermission renamePermission;
    @Nullable
    private volatile SettingsPermission settingsPermission;

    private DBDamService() {
    }

    @NotNull
    public static synchronized DBDamService getInstance() {
        if (instance == null) {
            instance = new DBDamService();
        }
        return instance;
    }

    @Nullable
    public DDConnectedRemoteDam getDam(@NotNull DBRProgressMonitor monitor) throws DBException, InterruptedException {
        var session = DBDamSessionService.getInstance();
        checkRegistration(session);
        var cached = session.getCachedDam();
        if (cached != null) {
            return cached;
        }
        String clientDamId = session.getClientDamId();
        var connectedSession = session.getChannel().getStatus().session();
        DDConnectedRemoteDam dam = account.getDam(clientDamId);
        if (dam == null) {
            awaitConnection(session, monitor);
            connectedSession = session.getChannel().getStatus().session();
            dam = account.getDam(clientDamId);
        }
        session.cacheDam(connectedSession, dam);
        return dam;
    }

    @Nullable
    public DDConnectedRemoteDam getCachedDam() throws DBException {
        return DBDamSessionService.getInstance().getCachedDam();
    }

    @NotNull
    public DDConnectedRemoteDam renameDam(@NotNull String name) throws DBException {
        var session = DBDamSessionService.getInstance();
        var connectedSession = session.getChannel().getStatus().session();
        var renamed = account.renameDam(session.getClientDamId(), name);
        session.cacheDam(connectedSession, renamed);
        return renamed;
    }

    public boolean canRenameDam() throws DBException {
        var status = DBDamSessionService.getInstance().getChannel().getStatus();
        var session = status.isReady() ? status.session() : null;
        var cached = renamePermission;
        if (session != null && cached != null && session.isSameSession(cached.session())) {
            return cached.allowed();
        }
        boolean allowed = account.canRenameDam();
        if (session != null) {
            renamePermission = new RenamePermission(session, allowed);
        }
        return allowed;
    }

    @NotNull
    public DDAccountClient.DamSettingsState getDamSettings() throws DBException {
        var cached = getCachedDamSettings();
        if (cached != null) {
            return cached;
        }
        var session = DBDamSessionService.getInstance();
        var connectedSession = session.getChannel().getStatus().session();
        var settings = account.getDamSettings(session.getClientDamId());
        if (connectedSession != null) {
            settingsPermission = new SettingsPermission(connectedSession, settings);
        }
        return settings;
    }

    @Nullable
    public DDAccountClient.DamSettingsState getCachedDamSettings() throws DBException {
        var status = DBDamSessionService.getInstance().getChannel().getStatus();
        var cached = settingsPermission;
        var settings = DBDamSettingsStore.getInstance().getSettings();
        if (!status.isReady() || cached == null || !cached.session().isSameSession(status.session()) || settings == null) {
            return null;
        }
        // Settings are pushed over the live channel; only the Account identity and permission need caching.
        return new DDAccountClient.DamSettingsState(cached.state().accountId(), settings, cached.state().canManage());
    }

    private record RenamePermission(@NotNull DDRemoteDamSessionInfo session, boolean allowed) {
    }

    private record SettingsPermission(
        @NotNull DDRemoteDamSessionInfo session, @NotNull DDAccountClient.DamSettingsState state
    ) {
    }

    @NotNull
    public String suggestDamName() {
        return DBDamNameService.suggestDamName();
    }

    private void awaitConnection(
        @NotNull DBDamSessionService session, @NotNull DBRProgressMonitor monitor
    ) throws DBException, InterruptedException {
        var channel = session.getChannel();
        if (!channel.getStatus().isReady()) {
            CountDownLatch settled = new CountDownLatch(1);
            long started = System.nanoTime();
            try (DDRemoteDamSubscription ignored = channel.observeState(status -> {
                if (status.isReady() || session.getMonitor().getRegistrationRejection() != null) {
                    settled.countDown();
                }
            })) {
                while (!settled.await(250, TimeUnit.MILLISECONDS)) {
                    if (monitor.isCanceled()) {
                        throw new InterruptedException("Waiting for DataDam connection was canceled");
                    }
                    if (System.nanoTime() - started >= CONNECTION_WAIT_TIMEOUT_NANOS) {
                        throw new DBException("Timed out waiting for DataDam connection");
                    }
                }
            }
        }
        if (monitor.isCanceled()) {
            throw new InterruptedException("Waiting for DataDam connection was canceled");
        }
        checkRegistration(session);
    }

    private static void checkRegistration(@NotNull DBDamSessionService session) throws DamRegistrationRejectedException {
        String reason = session.getMonitor().getRegistrationRejection();
        if (reason != null) {
            throw new DamRegistrationRejectedException(reason);
        }
    }

    static final class DamRegistrationRejectedException extends DBException {
        DamRegistrationRejectedException(@NotNull String reason) {
            super(reason);
        }
    }

    /** An Account request executed outside the UI thread. */
    @FunctionalInterface
    public interface ServiceCall<S, R> {
        @Nullable
        R run(@NotNull S service) throws Exception;
    }

    @NotNull
    public <R> AbstractJob createJob(
        @NotNull String jobName,
        @NotNull ServiceCall<DDAccountClient, R> operation,
        @NotNull Consumer<R> onSuccess,
        @NotNull Consumer<DBException> onFailure
    ) {
        AbstractJob job = new AbstractJob(jobName) {
            @NotNull
            @Override
            protected IStatus run(@NotNull DBRProgressMonitor monitor) {
                if (monitor.isCanceled() || !DDProductConfig.isIntegrationEnabled()) {
                    return Status.CANCEL_STATUS;
                }
                try {
                    R result = operation.run(account);
                    UIUtils.asyncExec(() -> {
                        if (!monitor.isCanceled() && !isCanceled()) {
                            onSuccess.accept(result);
                        }
                    });
                } catch (DBException e) {
                    notifyFailure(e, monitor);
                } catch (Exception e) {
                    notifyFailure(new DBException(jobName + " failed", e), monitor);
                }
                return Status.OK_STATUS;
            }

            private void notifyFailure(@NotNull DBException failure, @NotNull DBRProgressMonitor monitor) {
                UIUtils.asyncExec(() -> {
                    if (!monitor.isCanceled() && !isCanceled()) {
                        onFailure.accept(failure);
                    }
                });
            }
        };
        job.setSystem(true);
        job.schedule();
        return job;
    }

}
