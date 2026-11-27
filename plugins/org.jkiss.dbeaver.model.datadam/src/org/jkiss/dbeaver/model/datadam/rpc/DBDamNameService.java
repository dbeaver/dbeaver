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
package org.jkiss.dbeaver.model.datadam.rpc;

import com.dbeaver.datadam.dam.remote.api.model.DDConnectedRemoteDam;
import com.dbeaver.datadam.dam.remote.api.transport.DDRemoteDamSubscription;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.datadam.DDAccountClient;
import org.jkiss.dbeaver.model.datadam.DDEndpoints;
import org.jkiss.dbeaver.model.datadam.DDProductConfig;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DBDamAccessService;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.IPluginService;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** Persists the initial desktop name after the server registers the Dam. */
public class DBDamNameService implements IPluginService {
    private static final Log log = Log.getLog(DBDamNameService.class);
    private static final long NAME_RETRY_DELAY = 5_000;

    private final BooleanSupplier integrationEnabled;
    @Nullable
    private DDRemoteDamSubscription subscription;
    @Nullable
    private AbstractJob job;
    private boolean stopped;

    public DBDamNameService() {
        this(DDProductConfig::isIntegrationEnabled);
    }

    DBDamNameService(@NotNull BooleanSupplier integrationEnabled) {
        this.integrationEnabled = integrationEnabled;
    }

    @Override
    public synchronized void activateService() {
        if (stopped || subscription != null) {
            return;
        }
        try {
            var session = DBDamSessionService.getInstance();
            var account = new DDAccountClient(URI.create(DDEndpoints.getAccountBaseUrl()), DBDamAccessService.getInstance());
            activateService(session, account);
        } catch (DBException e) {
            log.debug("Cannot initialize DataDam desktop naming", e);
        }
    }

    synchronized void activateService(@NotNull DBDamSessionService session, @NotNull DDAccountClient account) {
        if (!stopped && subscription == null) {
            subscription = session.getChannel().observeState(status -> updateName(session, account, status.isReady()));
        }
    }

    private synchronized void updateName(
        @NotNull DBDamSessionService session, @NotNull DDAccountClient account, boolean ready
    ) {
        if (stopped) {
            return;
        }
        if (job != null) {
            job.cancel();
        }
        if (!ready) {
            return;
        }
        job = new AbstractJob("Initialize DataDam desktop name") {
            private boolean failureReported;

            @NotNull
            @Override
            protected IStatus run(@NotNull DBRProgressMonitor monitor) {
                if (monitor.isCanceled() || !integrationEnabled.getAsBoolean()
                    || !session.getChannel().getStatus().isReady()) {
                    return Status.CANCEL_STATUS;
                }
                try {
                    var connectedSession = session.getChannel().getStatus().session();
                    var dam = getOrInitializeName(account, session.getClientDamId());
                    if (dam != null) {
                        session.cacheDam(connectedSession, dam);
                        return Status.OK_STATUS;
                    }
                } catch (DBException e) {
                    if (!failureReported) {
                        log.debug("Cannot save initial DataDam desktop name", e);
                        failureReported = true;
                    }
                }
                // Registration may not be visible yet, and transient Account failures must not require a reconnect.
                synchronized (DBDamNameService.this) {
                    if (!stopped && job == this && !monitor.isCanceled()
                        && integrationEnabled.getAsBoolean() && session.getChannel().getStatus().isReady()) {
                        schedule(NAME_RETRY_DELAY);
                    }
                }
                return Status.OK_STATUS;
            }
        };
        job.setSystem(true);
        job.schedule();
    }

    @Override
    public synchronized void deactivateService() {
        stopped = true;
        if (job != null) {
            job.cancel();
        }
        if (subscription != null) {
            subscription.close();
            subscription = null;
        }
    }

    @Nullable
    public static DDConnectedRemoteDam getOrInitializeName(@NotNull DDAccountClient account, @NotNull String clientDamId)
        throws DBException {
        return getOrInitializeName(account, clientDamId, DBDamNameService::suggestDamName);
    }

    @Nullable
    static synchronized DDConnectedRemoteDam getOrInitializeName(
        @NotNull DDAccountClient account, @NotNull String clientDamId, @NotNull Supplier<String> suggestedName
    ) throws DBException {
        DDConnectedRemoteDam dam = account.getDam(clientDamId);
        if (dam != null && dam.damName() == null && account.canRenameDam()) {
            return account.renameDam(clientDamId, suggestedName.get());
        }
        return dam;
    }

    @NotNull
    public static String suggestDamName() {
        String computer = System.getenv("COMPUTERNAME");
        if (computer == null || computer.isBlank()) {
            computer = System.getenv("HOSTNAME");
        }
        if (computer == null || computer.isBlank()) {
            try {
                computer = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                // The product name is still a usable suggestion without a computer name.
            }
        }
        String product = GeneralUtils.getProductName();
        return computer == null || computer.isBlank() ? product : product + ": " + computer;
    }
}
