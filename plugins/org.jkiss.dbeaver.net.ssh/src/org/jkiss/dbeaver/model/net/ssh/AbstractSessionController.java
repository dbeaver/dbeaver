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
            session = createJumpSession(getShareableSession(origin), configuration, destination, portForward);
        } else {
            session = createDirectSession(destination, portForward);
        }

        session.connect(monitor, destination, configuration);

        return session;
    }

    @NotNull
    private DirectSession<T> createDirectSession(
        @NotNull SSHHostConfiguration destination,
        @Nullable SSHPortForwardConfiguration portForward
    ) {
        final ShareableSession<T> session = sessions.computeIfAbsent(destination, host -> {
            log.debug("SSHSessionController: Creating new session to " + host);
            return new ShareableSession<>(this, host);
        });

        return new DirectSession<>(session, portForward);
    }

    @NotNull
    private AbstractSession createJumpSession(
        @NotNull DelegateSession<T> origin,
        @NotNull DBWHandlerConfiguration configuration,
        @NotNull SSHHostConfiguration destination,
        @Nullable SSHPortForwardConfiguration portForward
    ) throws DBException {
        throw new DBException("Jump sessions are not supported");
    }

    @Override
    public void release(
        @NotNull DBRProgressMonitor monitor,
        @NotNull SSHSession session,
        @NotNull DBWHandlerConfiguration configuration,
        long timeout
    ) throws DBException {
        getDelegateSession(session).disconnect(monitor, configuration);
    }

    @Override
    public void invalidate(
        @NotNull DBRProgressMonitor monitor,
        @NotNull SSHSession session,
        @NotNull DBCInvalidatePhase phase,
        @NotNull DBWHandlerConfiguration configuration,
        long timeout
    ) throws DBException {
        final DelegateSession<T> delegate = getDelegateSession(session);

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
        return getShareableSession(session).dataSources.keySet().toArray(DBPDataSourceContainer[]::new);
    }

    @NotNull
    protected abstract T createSession();

    @NotNull
    @SuppressWarnings("unchecked")
    protected ShareableSession<T> getShareableSession(@NotNull SSHSession session) {
        if (session instanceof DirectSession<?> ds) {
            return (ShareableSession<T>) ds.inner;
        } else {
            throw new IllegalStateException("Unexpected session type: " + session + " (" + session.getClass().getName() + ")");
        }
    }

    @NotNull
    @SuppressWarnings("unchecked")
    protected DelegateSession<T> getDelegateSession(@NotNull SSHSession session) {
        if (session instanceof DelegateSession<?> delegate) {
            return (DelegateSession<T>) delegate;
        } else {
            throw new IllegalStateException("Unexpected session type: " + session + " (" + session.getClass().getName() + ")");
        }
    }

    protected void registerSession(@NotNull ShareableSession<T> session) {
        sessions.put(session.destination, session);
    }

    protected void unregisterSession(@NotNull ShareableSession<T> session) {
        sessions.remove(session.destination);
    }

    protected static class DirectSession<T extends AbstractSession> extends WrapperSession<T> {
        private final SSHPortForwardConfiguration portForward;

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
                super.setupPortForward(portForward);
            }
        }

        @Override
        public synchronized void disconnect(
            @NotNull DBRProgressMonitor monitor,
            @NotNull DBWHandlerConfiguration configuration
        ) throws DBException {
            if (portForward != null) {
                super.removePortForward(portForward);
            }

            super.disconnect(monitor, configuration);
        }
    }

    protected static class WrapperSession<T extends AbstractSession> extends DelegateSession<T> {
        protected final ShareableSession<T> inner;

        public WrapperSession(@NotNull ShareableSession<T> inner) {
            super(inner.session, inner.destination);
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
        public void disconnect(@NotNull DBRProgressMonitor monitor, @NotNull DBWHandlerConfiguration configuration) throws DBException {
            inner.disconnect(monitor, configuration);
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
    }

    protected static class ShareableSession<T extends AbstractSession> extends DelegateSession<T> {
        protected record PortForwardInfo(@NotNull SSHPortForwardConfiguration resolved, @NotNull AtomicInteger usages) {}

        protected final Map<DBPDataSourceContainer, AtomicInteger> dataSources = new HashMap<>();
        protected final Map<SSHPortForwardConfiguration, PortForwardInfo> portForwards = new HashMap<>();
        protected final AbstractSessionController<T> controller;

        public ShareableSession(@NotNull AbstractSessionController<T> controller, @NotNull SSHHostConfiguration destination) {
            super(controller.createSession(), destination);
            this.controller = controller;
        }

        @Override
        public synchronized void connect(
            @NotNull DBRProgressMonitor monitor,
            @NotNull SSHHostConfiguration destination,
            @NotNull DBWHandlerConfiguration configuration
        ) throws DBException {
            if (dataSources.isEmpty()) {
                super.connect(monitor, destination, configuration);
                controller.registerSession(this);
            }
            final DBPDataSourceContainer container = configuration.getDataSource();
            final AtomicInteger counter = dataSources.get(container);
            if (counter == null) {
                dataSources.put(container, new AtomicInteger(1));
            } else {
                log.debug("SSHSessionController: Reusing session for " + container.getName());
                counter.incrementAndGet();
            }
        }

        @Override
        public synchronized void disconnect(@NotNull DBRProgressMonitor monitor, @NotNull DBWHandlerConfiguration configuration) throws DBException {
            final DBPDataSourceContainer container = configuration.getDataSource();
            final AtomicInteger counter = dataSources.get(container);
            if (counter == null) {
                throw new DBException("Session is not acquired for " + container.getName());
            }
            if (counter.decrementAndGet() == 0) {
                log.debug("SSHSessionController: Releasing session for " + container.getName());
                dataSources.remove(container);
            }
            if (dataSources.isEmpty()) {
                controller.unregisterSession(this);
                super.disconnect(monitor, configuration);
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
                portForwards.put(configuration, new PortForwardInfo(resolved, new AtomicInteger(1)));
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

        @Property(viewable = true, order = 1, name = "Destination")
        public SSHHostConfiguration getDestination() {
            return destination;
        }

        @Property(viewable = true, order = 2, name = "Used By")
        public String getConsumerInfo() {
            return dataSources.entrySet().stream()
                .map(entry -> "%s (%s)".formatted(entry.getKey().getName(), entry.getValue()))
                .collect(Collectors.joining(", "));
        }

        @Property(viewable = true, order = 3, name = "Port Forwards")
        public String getPortForwardingInfo() {
            return portForwards.values().stream()
                .map(info -> "%s (%d)".formatted(info.resolved, info.usages.get()))
                .collect(Collectors.joining(", "));
        }
    }

    protected static class DelegateSession<T extends AbstractSession> extends AbstractSession {
        protected final T session;
        protected final SSHHostConfiguration destination;

        public DelegateSession(@NotNull T session, @NotNull SSHHostConfiguration destination) {
            this.session = session;
            this.destination = destination;
        }

        @Override
        public void connect(
            @NotNull DBRProgressMonitor monitor,
            @NotNull SSHHostConfiguration destination,
            @NotNull DBWHandlerConfiguration configuration
        ) throws DBException {
            log.debug("SSHSessionController: Connecting session to " + destination);
            session.connect(monitor, destination, configuration);
        }

        @Override
        public void disconnect(@NotNull DBRProgressMonitor monitor, @NotNull DBWHandlerConfiguration configuration) throws DBException {
            log.debug("SSHSessionController: Disconnecting session to " + destination);
            session.disconnect(monitor, configuration);
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
