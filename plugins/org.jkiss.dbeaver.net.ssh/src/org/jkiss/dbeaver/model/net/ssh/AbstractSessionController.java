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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.exec.DBCInvalidatePhase;
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

public abstract class AbstractSessionController<T extends AbstractSession> implements SSHSessionController {
    private static final Log log = Log.getLog(AbstractSessionController.class);

    protected final Map<SSHHostConfiguration, ShareableSession<T>> sessions = new ConcurrentHashMap<>();

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    public SSHSession acquireSession(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBWHandlerConfiguration configuration,
        @NotNull SSHHostConfiguration destination,
        @Nullable SSHSession origin,
        @Nullable SSHPortForwardConfiguration portForward
    ) throws DBException {
        if (origin == null) {
            return acquireDirectSession(monitor, configuration, destination, portForward);
        } else {
            return acquireJumpSession(monitor, (DirectSession<T>) origin, configuration, destination, portForward);
        }
    }

    @NotNull
    private DirectSession<T> acquireDirectSession(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBWHandlerConfiguration configuration,
        @NotNull SSHHostConfiguration destination,
        @Nullable SSHPortForwardConfiguration portForward
    ) throws DBException {
        ShareableSession<T> session = sessions.get(destination);

        if (session == null) {
            log.debug("SSHSessionController: Creating new session to " + destination);
            session = new ShareableSession<>(destination, createSession());
            session.connect(monitor, destination, configuration);
            sessions.put(destination, session);
        } else {
            log.debug("SSHSessionController: Reusing existing session to " + destination);
        }

        session = session.retain(configuration.getDataSource());

        final DirectSession<T> directSession = new DirectSession<>(session, destination, portForward);

        if (portForward != null) {
            directSession.setupPortForward(portForward);
        }

        return directSession;
    }

    @NotNull
    private JumpSession<T> acquireJumpSession(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DirectSession<T> origin,
        @NotNull DBWHandlerConfiguration configuration,
        @NotNull SSHHostConfiguration destination,
        @Nullable SSHPortForwardConfiguration portForward
    ) throws DBException {
        final JumpSession<T> session = new JumpSession<>(this, origin, configuration, destination, portForward);
        session.connect(monitor, destination, configuration);

        return session;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void release(
        @NotNull DBRProgressMonitor monitor,
        @NotNull SSHSession session,
        @NotNull DBWHandlerConfiguration configuration,
        long timeout
    ) throws DBException {
        if (session instanceof DirectSession<?> direct) {
            log.debug("SSHSessionController: Releasing session to " + direct.host);

            if (direct.inner.release(configuration.getDataSource())) {
                direct.disconnect();
                sessions.remove(direct.host);
            }
        } else if (session instanceof JumpSession<?> jump) {
            throw new DBException("Not implemented");
        } else {
            throw new DBException("Unsupported session type: " + session.getClass().getName());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void invalidate(
        @NotNull DBRProgressMonitor monitor,
        @NotNull SSHSession session,
        @NotNull DBCInvalidatePhase phase,
        @NotNull DBWHandlerConfiguration configuration,
        long timeout
    ) throws DBException {
        if (phase == DBCInvalidatePhase.BEFORE_INVALIDATE) {
            release(monitor, session, configuration, timeout);
        }

        if (phase == DBCInvalidatePhase.INVALIDATE) {
            final DirectSession<T> directSession = (DirectSession<T>) session;
            directSession.connect(monitor, directSession.host, configuration);

            final ShareableSession<T> shareableSession = directSession.inner;
            sessions.put(directSession.host, shareableSession.retain(configuration.getDataSource()));
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
        return getShareableSession(session).dataSources.keySet().toArray(new DBPDataSourceContainer[0]);
    }

    @NotNull
    protected abstract T createSession();

    @NotNull
    @SuppressWarnings("unchecked")
    protected ShareableSession<T> getShareableSession(@NotNull SSHSession session) {
        if (session instanceof ShareableSession<?> wrapper) {
            return (ShareableSession<T>) wrapper;
        } else if (session instanceof DirectSession<?> direct) {
            return (ShareableSession<T>) direct.inner;
        } else {
            throw new IllegalStateException("Unexpected session: " + session);
        }
    }

    protected static class JumpSession<T extends AbstractSession> extends SessionDelegate<T> {
        private final AbstractSessionController<T> controller;
        private final DirectSession<T> origin;
        private DirectSession<T> destination;
        private DBWHandlerConfiguration configuration;
        private SSHPortForwardConfiguration portForward;

        private SSHPortForwardConfiguration jumpPortForward;
        private SSHHostConfiguration jumpHost;

        public JumpSession(
            @NotNull AbstractSessionController<T> controller,
            @NotNull DirectSession<T> origin,
            @NotNull DBWHandlerConfiguration configuration,
            @NotNull SSHHostConfiguration host,
            @Nullable SSHPortForwardConfiguration portForward
        ) {
            super(host);
            this.controller = controller;
            this.origin = origin;
            this.configuration = configuration;
            this.portForward = portForward;
        }

        @Override
        public void connect(@NotNull DBRProgressMonitor monitor, @NotNull SSHHostConfiguration destination, @NotNull DBWHandlerConfiguration configuration) throws DBException {
            jumpPortForward = origin.setupPortForward(
                destination.getHostname(),
                destination.getPort()
            );

            jumpHost = new SSHHostConfiguration(
                destination.getUsername(),
                jumpPortForward.getLocalHost(),
                jumpPortForward.getLocalPort(),
                destination.getAuthConfiguration()
            );

            this.destination = controller.acquireDirectSession(
                monitor,
                configuration,
                jumpHost,
                portForward
            );

            this.session = this.destination.session;
        }

        @Override
        public void disconnect() throws DBException {
            super.disconnect();
            origin.disconnect();

            destination = null;
            jumpPortForward = null;
            jumpHost = null;
        }
    }

    protected static class DirectSession<T extends AbstractSession> extends SessionDelegate<T> {
        private final ShareableSession<T> inner;
        private final SSHPortForwardConfiguration portForward;

        public DirectSession(
            @NotNull ShareableSession<T> inner,
            @NotNull SSHHostConfiguration host,
            @Nullable SSHPortForwardConfiguration portForward
        ) {
            super(host, inner.session);
            this.inner = inner;
            this.portForward = portForward;
        }

        @Override
        public void connect(
            @NotNull DBRProgressMonitor monitor,
            @NotNull SSHHostConfiguration destination,
            @NotNull DBWHandlerConfiguration configuration
        ) throws DBException {
            log.debug("SSHSessionController: Connecting session to " + host);

            super.connect(monitor, destination, configuration);

            if (portForward != null) {
                setupPortForward(portForward);
            }
        }

        @Override
        public void disconnect() throws DBException {
            log.debug("SSHSessionController: Disconnecting session to " + host);

            if (portForward != null) {
                removePortForward(portForward);
            }

            super.disconnect();
        }
    }

    protected static class ShareableSession<T extends AbstractSession> extends SessionDelegate<T> {
        protected final Map<DBPDataSourceContainer, AtomicInteger> dataSources = new HashMap<>();

        public ShareableSession(@NotNull SSHHostConfiguration host, @NotNull T session) {
            super(host, session);
        }

        @NotNull
        protected synchronized ShareableSession<T> retain(@NotNull DBPDataSourceContainer container) {
            // FIXME: Session can be used multiple times by the same data source in case
            //  it's connected and SSH connection test was performed. We should pass a flag
            //  to the 'acquireSession' method to indicate that the session must not be shared.
            dataSources
                .computeIfAbsent(container, c -> new AtomicInteger())
                .incrementAndGet();
            return this;
        }

        protected synchronized boolean release(@NotNull DBPDataSourceContainer container) {
            final AtomicInteger counter = dataSources.get(container);
            if (counter == null) {
                throw new IllegalStateException("Session is not acquired for " + container.getName());
            }
            if (counter.decrementAndGet() == 0) {
                dataSources.remove(container);
                return true;
            }
            return false;
        }
    }

    protected static class SessionDelegate<T extends AbstractSession> extends AbstractSession {
        protected final SSHHostConfiguration host;
        protected T session;

        public SessionDelegate(@NotNull SSHHostConfiguration host, @Nullable T session) {
            this.host = host;
            this.session = session;
        }

        public SessionDelegate(@NotNull SSHHostConfiguration host) {
            this(host, null);
        }

        @Override
        public void connect(
            @NotNull DBRProgressMonitor monitor,
            @NotNull SSHHostConfiguration destination,
            @NotNull DBWHandlerConfiguration configuration
        ) throws DBException {
            session.connect(monitor, destination, configuration);
        }

        @Override
        public void disconnect() throws DBException {
            session.disconnect();
        }

        @NotNull
        @Override
        public SSHPortForwardConfiguration setupPortForward(@NotNull String remoteHost, int remotePort) throws DBException {
            log.debug("SSHSessionController: Set up port forwarding to %s:%d".formatted(remoteHost, remotePort));
            return session.setupPortForward(remoteHost, remotePort);
        }

        @NotNull
        @Override
        public SSHPortForwardConfiguration setupPortForward(@NotNull SSHPortForwardConfiguration configuration) throws DBException {
            log.debug("SSHSessionController: Set up port forwarding " + configuration);
            return session.setupPortForward(configuration);
        }

        @Override
        public void removePortForward(@NotNull SSHPortForwardConfiguration configuration) throws DBException {
            log.debug("SSHSessionController: Remove port forwarding " + configuration);
            session.removePortForward(configuration);
        }

        @Override
        public void getFile(
            @NotNull String src,
            @NotNull OutputStream dst,
            @NotNull DBRProgressMonitor monitor
        ) throws DBException, IOException {
            session.getFile(src, dst, monitor);
        }

        @Override
        public void putFile(
            @NotNull InputStream src,
            @NotNull String dst,
            @NotNull DBRProgressMonitor monitor
        ) throws DBException, IOException {
            session.putFile(src, dst, monitor);
        }

        @NotNull
        @Override
        public String getClientVersion() {
            return session.getClientVersion();
        }

        @NotNull
        @Override
        public String getServerVersion() {
            return session.getServerVersion();
        }
    }
}
