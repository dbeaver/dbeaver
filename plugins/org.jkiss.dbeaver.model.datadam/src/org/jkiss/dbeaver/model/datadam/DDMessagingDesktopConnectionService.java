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
package org.jkiss.dbeaver.model.datadam;

import com.dbeaver.datadam.messaging.api.model.DDMessagingHandshakeRequest;
import com.dbeaver.datadam.messaging.api.transport.DDMessagingChannel;
import com.dbeaver.messaging.rpc.MessagingRpcEndpoint;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopAccessListener;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopAccessService;
import org.jkiss.dbeaver.model.datadam.transport.DDMessagingDesktopChannel;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DDMessagingDesktopConnectionService implements AutoCloseable {

    private static final String APPLICATION_RUN_ID = UUID.randomUUID().toString();

    private static DDMessagingDesktopConnectionService instance;

    private final DDDesktopAccessService access;
    private final DDMessagingDesktopChannel channel;
    private final DDMessagingDesktopRpcService rpc;
    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();

    private final DDDesktopAccessListener accessListener = new DDDesktopAccessListener() {

        @Override
        public void accessChanging() {
            rpc.suspend(false);
            channel.pause();
        }

        @Override
        public void accessChanged() {
            updateAccess(false);
        }
    };

    public DDMessagingDesktopConnectionService(@NotNull DDDesktopAccessService access, @NotNull DDMessagingDesktopChannel channel) {
        this.access = access;
        this.channel = channel;
        this.rpc = new DDMessagingDesktopRpcService(channel);
    }

    @NotNull
    public static synchronized DDMessagingDesktopConnectionService getInstance() throws DBException {
        if (instance == null) {
            DDDesktopAccessService access = DDDesktopAccessService.getInstance();
            DDMessagingHandshakeRequest hello = new DDMessagingHandshakeRequest(
                DBWorkbench.getPlatform().getDeploymentId(), APPLICATION_RUN_ID,
                GeneralUtils.getProductName(), GeneralUtils.getProductVersion().toString());
            DDMessagingDesktopChannel channel = new DDMessagingDesktopChannel(
                URI.create(DDEndpoints.getMessagingWebSocketUrl()), hello,
                resource -> access.getStatus() == DDDesktopAccessService.Status.SIGNED_IN
                    ? access.getAccessTokenForResource(resource) : null);
            instance = new DDMessagingDesktopConnectionService(access, channel);
        }
        return instance;
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
    }

    @NotNull
    public DDMessagingChannel getChannel() {
        return channel;
    }

    @NotNull
    public MessagingRpcEndpoint getRpc() throws DBException {
        return rpc.getRpc();
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            access.removeAccessListener(accessListener);
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
        if (access.getStatus() == DDDesktopAccessService.Status.SIGNED_IN) {
            if (rpc.resume(initial)) {
                channel.start();
            }
        } else if (rpc.suspend(initial)) {
            channel.pause();
        }
    }
}
