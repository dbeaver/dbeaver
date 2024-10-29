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
package org.jkiss.dbeaver.model.net.ssh;

import com.jcraft.jsch.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.exec.DBCInvalidatePhase;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.ssh.config.SSHHostConfiguration;
import org.jkiss.dbeaver.model.net.ssh.config.SSHPortForwardConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public abstract class AbstractSessionController<T extends AbstractSession> implements SSHSessionController {
    private static final Log log = Log.getLog(AbstractSessionController.class);

    protected final Map<SSHHostConfiguration, ShareableSession<T>> sessions = new ConcurrentHashMap<>();
    protected AgentIdentityRepository agentIdentityRepository;

    @NotNull
    @Override
    public SSHSession acquireSession(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBWHandlerConfiguration configuration,
        @NotNull SSHHostConfiguration destination,
        @Nullable SSHSession origin,
        @Nullable SSHPortForwardConfiguration portForward
    ) throws DBException {
        final AbstractSession session;
        if (origin != null) {
            session = createJumpSession(getDelegateSession(origin), destination, portForward);
        } else {
            session = createDirectSession(configuration, destination, portForward);
        }

        session.connect(monitor, destination, configuration);

        return session;
    }

    @NotNull
    private DirectSession<T> createDirectSession(
        @NotNull DBWHandlerConfiguration configuration,
        @NotNull SSHHostConfiguration destination,
        @Nullable SSHPortForwardConfiguration portForward
    ) {
        ShareableSession<T> session = getSharedSession(configuration, destination);
        if (session == null) {
            // Session will be registered during connect
            session = new ShareableSession<>(this, destination);
        }
        return new DirectSession<>(session, portForward);
    }

    @NotNull
    private JumpSession<T> createJumpSession(
        @NotNull DelegateSession origin,
        @NotNull SSHHostConfiguration destination,
        @Nullable SSHPortForwardConfiguration portForward
    ) {
        return new JumpSession<>(this, origin, destination, portForward);
    }

    @Override
    public void release(
        @NotNull DBRProgressMonitor monitor,
        @NotNull SSHSession session,
        @NotNull DBWHandlerConfiguration configuration,
        long timeout
    ) throws DBException {
        getDelegateSession(session).disconnect(monitor, configuration, timeout);
    }

    @Override
    public void invalidate(
        @NotNull DBRProgressMonitor monitor,
        @NotNull SSHSession session,
        @NotNull DBCInvalidatePhase phase,
        @NotNull DBWHandlerConfiguration configuration,
        long timeout
    ) throws DBException {
        final DelegateSession delegate = getDelegateSession(session);

        if (phase == DBCInvalidatePhase.BEFORE_INVALIDATE) {
            release(monitor, delegate, configuration, timeout);
        }

        if (phase == DBCInvalidatePhase.INVALIDATE) {
            delegate.connect(monitor, delegate.destination, configuration);
        }
    }

    @NotNull
    @Override
    public SSHSession[] getSessions() {
        return sessions.values().toArray(SSHSession[]::new);
    }

    @NotNull
    @Override
    public DBPDataSourceContainer[] getDependentDataSources(@NotNull SSHSession session) {
        return getDelegateSession(session).getDataSources();
    }

    @NotNull
    protected IdentityRepository createAgentIdentityRepository() throws DBException {
        if (agentIdentityRepository == null) {
            AgentConnector connector = null;

            try {
                connector = new PageantConnector();
                log.debug("SSHSessionController: connected with pageant");
            } catch (Exception e) {
                log.debug("SSHSessionController: pageant connect exception", e);
            }

            if (connector == null) {
                try {
                    connector = new SSHAgentConnector(new JUnixSocketFactory());
                    log.debug("SSHSessionController: Connected with ssh-agent");
                } catch (Exception e) {
                    log.debug("SSHSessionController: ssh-agent connection exception", e);
                }
            }

            if (connector == null) {
                throw new DBException("Unable to initialize SSH agent");
            }

            agentIdentityRepository = new AgentIdentityRepository(connector);
        }

        return agentIdentityRepository;
    }

    @NotNull
    protected abstract T createSession();

    @NotNull
    protected DelegateSession getDelegateSession(@NotNull SSHSession session) {
        if (session instanceof DelegateSession delegate) {
            return delegate;
        } else {
            throw new IllegalStateException("Unexpected session type: " + session + " (" + session.getClass().getName() + ")");
        }
    }

    protected void registerSession(@NotNull ShareableSession<T> session, @NotNull DBWHandlerConfiguration configuration) {
        if (canShareSessionForConfiguration(configuration)) {
            sessions.put(session.destination, session);
        }
    }

    protected void unregisterSession(@NotNull ShareableSession<T> session, @NotNull DBWHandlerConfiguration configuration) {
        if (canShareSessionForConfiguration(configuration)) {
            sessions.remove(session.destination);
        }
    }

    @Nullable
    private ShareableSession<T> getSharedSession(
        @NotNull DBWHandlerConfiguration configuration,
        @NotNull SSHHostConfiguration destination
    ) {
        if (canShareSessionForConfiguration(configuration)) {
            return sessions.get(destination);
        } else {
            return null;
        }
    }

    protected static boolean canShareSessionForConfiguration(@NotNull DBWHandlerConfiguration configuration) {
        // Data source might be null if this tunnel is used for connection testing
        return !SSHUtils.DISABLE_SESSION_SHARING
            && configuration.getDataSource() != null
            && configuration.getBooleanProperty(SSHConstants.PROP_SHARE_TUNNELS, true);
    }

    protected static class JumpSession<T extends AbstractSession> extends DelegateSession {
        private final AbstractSessionController<T> controller;
        private final DelegateSession origin;
        private SSHPortForwardConfiguration portForward;
        private DelegateSession jumpDestination;
        private SSHPortForwardConfiguration jumpPortForward;
        private boolean registered;

        public JumpSession(
            @NotNull AbstractSessionController<T> controller,
            @NotNull DelegateSession origin,
            @NotNull SSHHostConfiguration destination,
            @Nullable SSHPortForwardConfiguration portForward
        ) {
            super(destination);
            this.controller = controller;
            this.origin = origin;
            this.portForward = portForward;
            this.registered = true;
        }

        @Override
        public void connect(
            @NotNull DBRProgressMonitor monitor,
            @NotNull SSHHostConfiguration host,
            @NotNull DBWHandlerConfiguration configuration
        ) throws DBException {
            if (!registered) {
                // When opening session for the first time, it will be already connected
                // When revalidating, it's closed and then must be opened again
                origin.connect(monitor, origin.destination, configuration);
                registered = true;
            }

            jumpPortForward = origin.setupPortForward(new SSHPortForwardConfiguration(
                DBConstants.HOST_LOCALHOST_IP,
                0,
                host.hostname(),
                host.port()
            ));

            final SSHHostConfiguration jumpHost = new SSHHostConfiguration(
                host.username(),
                jumpPortForward.localHost(),
                jumpPortForward.localPort(),
                host.auth()
            );

            jumpDestination = controller.createDirectSession(configuration, jumpHost, null);
            jumpDestination.connect(monitor, jumpHost, configuration);

            if (portForward != null) {
                portForward = jumpDestination.setupPortForward(portForward);
            }
        }

        @Override
        public void disconnect(
            @NotNull DBRProgressMonitor monitor,
            @NotNull DBWHandlerConfiguration configuration,
            long timeout
        ) throws DBException {
            if (portForward != null) {
                jumpDestination.removePortForward(portForward);
            }

            jumpDestination.disconnect(monitor, configuration, timeout);
            origin.removePortForward(jumpPortForward);
            origin.disconnect(monitor, configuration, timeout);

            registered = false;
            jumpDestination = null;
            jumpPortForward = null;
        }

        @NotNull
        @Override
        protected AbstractSession getSession() {
            return jumpDestination;
        }

        @NotNull
        @Override
        protected DBPDataSourceContainer[] getDataSources() {
            return origin.getDataSources();
        }
    }

    protected static class DirectSession<T extends AbstractSession> extends WrapperSession<T> {
        private SSHPortForwardConfiguration portForward;

        public DirectSession(
            @NotNull ShareableSession<T> inner,
            @Nullable SSHPortForwardConfiguration portForward
        ) {
            super(inner);
            this.portForward = portForward;
        }

        @Override
        public synchronized void connect(
            @NotNull DBRProgressMonitor monitor,
            @NotNull SSHHostConfiguration destination,
            @NotNull DBWHandlerConfiguration configuration
        ) throws DBException {
            super.connect(monitor, destination, configuration);

            if (portForward != null) {
                portForward = super.setupPortForward(portForward);
            }
        }

        @Override
        public synchronized void disconnect(
            @NotNull DBRProgressMonitor monitor,
            @NotNull DBWHandlerConfiguration configuration,
            long timeout
        ) throws DBException {
            if (portForward != null) {
                super.removePortForward(portForward);
            }

            super.disconnect(monitor, configuration, timeout);
        }
    }

    protected static class WrapperSession<T extends AbstractSession> extends DelegateSession {
        protected final ShareableSession<T> inner;

        public WrapperSession(@NotNull ShareableSession<T> inner) {
            super(inner.destination);
            this.inner = inner;
        }

        @Override
        public void connect(
            @NotNull DBRProgressMonitor monitor,
            @NotNull SSHHostConfiguration destination,
            @NotNull DBWHandlerConfiguration configuration
        ) throws DBException {
            inner.connect(monitor, destination, configuration);
        }

        @Override
        public void disconnect(
            @NotNull DBRProgressMonitor monitor,
            @NotNull DBWHandlerConfiguration configuration,
            long timeout
        ) throws DBException {
            inner.disconnect(monitor, configuration, timeout);
        }

        @NotNull
        @Override
        public SSHPortForwardConfiguration setupPortForward(@NotNull SSHPortForwardConfiguration configuration) throws DBException {
            return inner.setupPortForward(configuration);
        }

        @Override
        public void removePortForward(@NotNull SSHPortForwardConfiguration configuration) throws DBException {
            inner.removePortForward(configuration);
        }

        @NotNull
        @Override
        protected AbstractSession getSession() {
            return inner;
        }

        @NotNull
        @Override
        protected DBPDataSourceContainer[] getDataSources() {
            return inner.getDataSources();
        }
    }

    protected static class ShareableSession<T extends AbstractSession> extends DelegateSession {
        protected record PortForwardInfo(@NotNull SSHPortForwardConfiguration resolved, @NotNull AtomicInteger usages) {
        }

        protected final Map<DBPDataSourceContainer, AtomicInteger> dataSources = new HashMap<>();
        protected final Map<SSHPortForwardConfiguration, PortForwardInfo> portForwards = new HashMap<>();
        protected final AbstractSessionController<T> controller;
        protected final T session;

        public ShareableSession(@NotNull AbstractSessionController<T> controller, @NotNull SSHHostConfiguration destination) {
            super(destination);
            this.controller = controller;
            this.session = controller.createSession();
        }

        @Property(viewable = true, order = 1, name = "Destination")
        public String getDestinationInfo() {
            return destination.toDisplayString();
        }

        @Property(viewable = true, order = 2, name = "Used By")
        public String getConsumerInfo() {
            return dataSources.entrySet().stream()
                .map(entry -> "%s (%s)".formatted(entry.getKey(), entry.getValue()))
                .collect(Collectors.joining(", "));
        }

        @Property(viewable = true, order = 3, name = "Port Forwards")
        public String getPortForwardingInfo() {
            return portForwards.values().stream()
                .map(info -> "%s (%d)".formatted(info.resolved.toDisplayString(), info.usages.get()))
                .collect(Collectors.joining(", "));
        }

        @Override
        public synchronized void connect(
            @NotNull DBRProgressMonitor monitor,
            @NotNull SSHHostConfiguration destination,
            @NotNull DBWHandlerConfiguration configuration
        ) throws DBException {
            if (dataSources.isEmpty()) {
                log.debug("SSHSessionController: Creating new session to " + destination);
                super.connect(monitor, destination, configuration);
                controller.registerSession(this, configuration);
            }
            final DBPDataSourceContainer container = configuration.getDataSource();
            final AtomicInteger counter = dataSources.get(container);
            if (counter == null) {
                dataSources.put(container, new AtomicInteger(1));
            } else {
                log.debug("SSHSessionController: Reusing session to " + destination + " for " + container);
                counter.incrementAndGet();
            }
        }

        @Override
        public synchronized void disconnect(@NotNull DBRProgressMonitor monitor, @NotNull DBWHandlerConfiguration configuration, long timeout) throws DBException {
            final DBPDataSourceContainer container = configuration.getDataSource();
            final AtomicInteger counter = dataSources.get(container);
            if (counter == null) {
                throw new DBException("Session is not acquired for " + container);
            }
            if (counter.decrementAndGet() == 0) {
                log.debug("SSHSessionController: Releasing session for " + container);
                dataSources.remove(container);
            }
            if (dataSources.isEmpty()) {
                controller.unregisterSession(this, configuration);
                super.disconnect(monitor, configuration, timeout);
            }
        }

        @NotNull
        @Override
        public synchronized SSHPortForwardConfiguration setupPortForward(@NotNull SSHPortForwardConfiguration configuration) throws DBException {
            final PortForwardInfo info = portForwards.get(configuration);
            if (info != null) {
                log.debug("SSHSessionController: Reusing port forward " + configuration);
                info.usages.incrementAndGet();
                return info.resolved;
            } else {
                final SSHPortForwardConfiguration resolved = super.setupPortForward(configuration);
                portForwards.put(resolved, new PortForwardInfo(resolved, new AtomicInteger(1)));
                return resolved;
            }
        }

        @Override
        public void removePortForward(@NotNull SSHPortForwardConfiguration configuration) throws DBException {
            final PortForwardInfo info = portForwards.get(configuration);
            if (info == null) {
                throw new DBException("Port forward is not set up: " + configuration);
            }
            if (info.usages.decrementAndGet() == 0) {
                super.removePortForward(info.resolved);
                portForwards.remove(configuration);
            }
        }

        @NotNull
        @Override
        protected T getSession() {
            return session;
        }

        @NotNull
        @Override
        protected DBPDataSourceContainer[] getDataSources() {
            return dataSources.keySet().toArray(new DBPDataSourceContainer[0]);
        }
    }

    protected static abstract class DelegateSession extends AbstractSession {
        protected final SSHHostConfiguration destination;

        public DelegateSession(@NotNull SSHHostConfiguration destination) {
            this.destination = destination;
        }

        @Override
        public void connect(
            @NotNull DBRProgressMonitor monitor,
            @NotNull SSHHostConfiguration destination,
            @NotNull DBWHandlerConfiguration configuration
        ) throws DBException {
            log.debug("SSHSessionController: Connecting session to " + destination);
            getSession().connect(monitor, destination, configuration);
        }

        @Override
        public void disconnect(
            @NotNull DBRProgressMonitor monitor,
            @NotNull DBWHandlerConfiguration configuration,
            long timeout
        ) throws DBException {
            log.debug("SSHSessionController: Disconnecting session to " + destination);
            getSession().disconnect(monitor, configuration, timeout);
        }

        @NotNull
        @Override
        public SSHPortForwardConfiguration setupPortForward(@NotNull SSHPortForwardConfiguration configuration) throws DBException {
            log.debug("SSHSessionController: Set up port forwarding " + configuration);
            return getSession().setupPortForward(configuration);
        }

        @Override
        public void removePortForward(@NotNull SSHPortForwardConfiguration configuration) throws DBException {
            log.debug("SSHSessionController: Remove port forwarding " + configuration);
            getSession().removePortForward(configuration);
        }

        @Override
        public void getFile(
            @NotNull String src,
            @NotNull OutputStream dst,
            @NotNull DBRProgressMonitor monitor
        ) throws DBException, IOException {
            getSession().getFile(src, dst, monitor);
        }

        @Override
        public void putFile(
            @NotNull InputStream src,
            @NotNull String dst,
            @NotNull DBRProgressMonitor monitor
        ) throws DBException, IOException {
            getSession().putFile(src, dst, monitor);
        }

        @NotNull
        @Override
        public String getClientVersion() {
            return getSession().getClientVersion();
        }

        @NotNull
        @Override
        public String getServerVersion() {
            return getSession().getServerVersion();
        }

        @NotNull
        protected abstract AbstractSession getSession();

        @NotNull
        protected abstract DBPDataSourceContainer[] getDataSources();
    }
}
