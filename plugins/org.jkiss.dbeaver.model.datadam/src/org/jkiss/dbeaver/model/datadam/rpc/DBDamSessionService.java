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
import com.dbeaver.datadam.dam.remote.api.model.DDRemoteDamHandshakeRequest;
import com.dbeaver.datadam.dam.remote.api.model.DDRemoteDamSessionInfo;
import com.dbeaver.datadam.dam.remote.api.transport.DDRemoteDamChannel;
import com.dbeaver.datadam.dam.remote.api.transport.DDRemoteDamSubscription;
import com.dbeaver.messaging.rpc.MessagingRpcEndpoint;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.datadam.DDEndpoints;
import org.jkiss.dbeaver.model.datadam.DDProductConfig;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DBDamAccessListener;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DBDamAccessService;
import org.jkiss.dbeaver.model.datadam.internal.DDConnectionMessages;
import org.jkiss.dbeaver.model.datadam.transport.DBDamChannel;
import org.jkiss.dbeaver.model.datadam.transport.DDConnectionMonitor;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceListener;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.SystemJob;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.net.SocketException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class DBDamSessionService implements AutoCloseable {
    private static final Log log = Log.getLog(DBDamSessionService.class);

    private static final String APP_INSTANCE_ID = UUID.randomUUID().toString();
    private static final long RPC_RETRY_DELAY = 1_000;
    private static final String RPC_INITIALIZATION_ERROR = DDConnectionMessages.rpc_initialization_failed;

    private static DBDamSessionService instance;

    private final DBDamAccessService access;
    private final DBDamChannel channel;
    private final DBDamRpcService rpc;
    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();
    private volatile boolean connectionEnabled = true;
    private DBDamAccessService.Status accessStatus = DBDamAccessService.Status.UNLOADED;
    private String accessIdentity = "";
    @Nullable
    private DBPPreferenceStore preferenceStore;
    private final DBPPreferenceListener preferenceListener = event -> {
        if (DDProductConfig.INTEGRATION_ENABLED.equals(event.getProperty())) {
            setConnectionEnabled(DDProductConfig.isIntegrationEnabled());
        }
    };
    private final AtomicReference<DDRemoteDamSubscription> readyObservation = new AtomicReference<>();
    private final AtomicReference<CachedDam> cachedDam = new AtomicReference<>();
    private final SystemJob rpcInitializationRetry = new SystemJob("Retry DataDam RPC initialization", monitor -> {
        initializeRpc();
    });

    private final DBDamAccessListener accessListener = new DBDamAccessListener() {

        @Override
        public void accessTokenRefreshed() {
            channel.refreshAuthentication();
        }

        @Override
        public void accessChanging() {
            rpc.suspend(false);
            rpcInitializationRetry.cancel();
            channel.pause();
        }

        @Override
        public void accessChanged() {
            var next = access.getStatus();
            String identity = next == DBDamAccessService.Status.SIGNED_IN ? getUserIdentity() : "";
            if (accessStatus != next || !accessIdentity.equals(identity)) {
                accessStatus = next;
                accessIdentity = identity;
                DDConnectionMonitor monitor = channel.getMonitor();
                var type = switch (next) {
                    case SIGNED_IN -> DDConnectionMonitor.EventType.SIGNED_IN;
                    case SIGNED_OUT -> DDConnectionMonitor.EventType.SIGNED_OUT;
                    case INVALID_TOKEN -> DDConnectionMonitor.EventType.INVALID_TOKEN;
                    case UNLOADED -> null;
                };
                if (type != null) {
                    monitor.record(type, identity);
                }
            }
            updateAccess(false);
        }
    };

    public DBDamSessionService(@NotNull DBDamAccessService access, @NotNull DBDamChannel channel) {
        this(access, channel, DBDamRpcRegistrationRegistry.getInstance());
    }

    public DBDamSessionService(
        @NotNull DBDamAccessService access,
        @NotNull DBDamChannel channel,
        @NotNull DBDamRpcRegistrationRegistry registrations
    ) {
        this.access = access;
        this.channel = channel;
        this.rpc = new DBDamRpcService(channel, registrations);
    }

    @NotNull
    public static synchronized DBDamSessionService getInstance() throws DBException {
        if (instance == null) {
            DBDamAccessService access = DBDamAccessService.getInstance();
            String workspaceId = DBWorkbench.getPlatform().getWorkspace().getWorkspaceId();
            String clientDamId = formatClientDamId(workspaceId, readMacHash());
            DDRemoteDamHandshakeRequest hello = new DDRemoteDamHandshakeRequest(
                DBWorkbench.getPlatform().getDeploymentId(), APP_INSTANCE_ID,
                workspaceId, clientDamId,
                GeneralUtils.getProductName(), GeneralUtils.getProductVersion().toString());
            DBDamChannel channel = new DBDamChannel(
                URI.create(DDEndpoints.getRemoteDamWebSocketUrl()), hello,
                resource -> access.getStatus() == DBDamAccessService.Status.SIGNED_IN
                    ? access.getAccessTokenForResource(resource) : null);
            instance = new DBDamSessionService(access, channel);
            instance.preferenceStore = DBWorkbench.getPlatform().getPreferenceStore();
            instance.connectionEnabled = DDProductConfig.isIntegrationEnabled();
            instance.preferenceStore.addPropertyChangeListener(instance.preferenceListener);
            log.debug("Created DataDam desktop session service");
        }
        return instance;
    }

    @NotNull
    private static String readMacHash() {
        try {
            byte[] address = RuntimeUtils.selectMacAddress();
            return address == null ? RuntimeUtils.NO_MAC_HASH : RuntimeUtils.hashMacAddress(address);
        } catch (SocketException | NoSuchAlgorithmException e) {
            log.debug(e);
            return RuntimeUtils.MAC_HASH_ERROR;
        }
    }

    @NotNull
    static String formatClientDamId(@NotNull String workspaceId, @NotNull String macHash) {
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString((workspaceId + ':' + macHash).getBytes(StandardCharsets.UTF_8));
    }

    public void start() {
        if (closed.get() || !started.compareAndSet(false, true)) {
            return;
        }
        access.addAccessListener(accessListener);
        if (closed.get()) {
            access.removeAccessListener(accessListener);
            return;
        }
        updateAccess(true);
        if (!closed.get()) {
            DDRemoteDamSubscription observation = channel.observeState(status -> {
                if (status.isReady() && !closed.get()) {
                    DBDamSettingsStore.getInstance().updateSettings(status.session().settings());
                    initializeRpc();
                } else {
                    cachedDam.set(null);
                    DBDamSettingsStore.getInstance().updateSettings(null);
                    rpcInitializationRetry.cancel();
                }
            });
            readyObservation.set(observation);
            if (closed.get()) {
                DDRemoteDamSubscription stopped = readyObservation.getAndSet(null);
                if (stopped != null) {
                    stopped.close();
                }
            }
        }
    }

    private void initializeRpc() {
        if (closed.get() || !channel.getStatus().isReady()) {
            return;
        }
        try {
            rpc.getRpc();
            channel.getMonitor().clearError(RPC_INITIALIZATION_ERROR);
        } catch (DBException e) {
            log.warn("Cannot open desktop DataDam RPC endpoint", e);
            channel.getMonitor().failed(RPC_INITIALIZATION_ERROR);
            if (!closed.get() && channel.getStatus().isReady() && access.getStatus() == DBDamAccessService.Status.SIGNED_IN) {
                rpcInitializationRetry.schedule(RPC_RETRY_DELAY);
            }
        }
    }

    @NotNull
    public String getClientDamId() {
        return channel.getClientDamId();
    }

    @Nullable
    public DDConnectedRemoteDam getCachedDam() {
        var status = channel.getStatus();
        var cached = cachedDam.get();
        return !closed.get() && status.isReady() && cached != null && cached.session() == status.session()
            ? cached.dam() : null;
    }

    /** Associates Account data with the session that requested it, so a reconnect cannot reuse stale data. */
    public void cacheDam(@Nullable DDRemoteDamSessionInfo session, @Nullable DDConnectedRemoteDam dam) {
        if (session != null && dam != null) {
            cachedDam.updateAndGet(cached -> {
                var status = channel.getStatus();
                return !closed.get() && status.isReady() && session == status.session()
                    ? new CachedDam(session, dam) : cached;
            });
        }
    }

    private record CachedDam(@NotNull DDRemoteDamSessionInfo session, @NotNull DDConnectedRemoteDam dam) {
    }

    @NotNull
    public DDRemoteDamChannel getChannel() {
        return channel;
    }

    @NotNull
    public DDConnectionMonitor getMonitor() {
        return channel.getMonitor();
    }

    @NotNull
    private String getUserIdentity() {
        var user = access.getLoggedUserIdentity();
        return user.userName() == null ? "" : user.userName();
    }

    public synchronized void setConnectionEnabled(boolean enabled) {
        if (closed.get()) {
            return;
        }
        connectionEnabled = enabled;
        if (!enabled) {
            rpcInitializationRetry.cancel();
            rpc.suspend(false);
            channel.pause();
        } else {
            updateAccess(false);
        }
    }

    @NotNull
    public MessagingRpcEndpoint getRpc() throws DBException {
        return rpc.getRpc();
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            DBDamSettingsStore.getInstance().updateSettings(null);
            if (preferenceStore != null) {
                preferenceStore.removePropertyChangeListener(preferenceListener);
            }
            rpcInitializationRetry.cancel();
            access.removeAccessListener(accessListener);
            DDRemoteDamSubscription observation = readyObservation.getAndSet(null);
            if (observation != null) {
                observation.close();
            }
            try {
                rpc.close();
            } finally {
                channel.close();
            }
        }
    }

    private void updateAccess(boolean initial) {
        if (closed.get() || !started.get()) {
            return;
        }
        var status = access.getStatus();
        synchronized (this) {
            if (closed.get()) {
                return;
            }
            if (connectionEnabled && status == DBDamAccessService.Status.SIGNED_IN) {
                if (rpc.resume(initial)) {
                    channel.start();
                }
            } else if (rpc.suspend(initial)) {
                channel.pause();
            }
        }
    }
}
