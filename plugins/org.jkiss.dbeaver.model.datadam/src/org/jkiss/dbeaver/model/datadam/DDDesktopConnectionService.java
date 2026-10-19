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

import com.dbeaver.datadam.external.api.model.DDHello;
import com.dbeaver.datadam.external.api.transport.DDMessageChannel;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopAccessListener;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopAccessService;
import org.jkiss.dbeaver.model.datadam.transport.DDDesktopMessageChannel;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DDDesktopConnectionService implements AutoCloseable {
    private static final String APPLICATION_RUN_ID = UUID.randomUUID().toString();
    @Nullable
    private static DDDesktopConnectionService instance;

    private final DDDesktopAccessService access;
    private final DDDesktopMessageChannel channel;
    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final DDDesktopAccessListener accessListener = new DDDesktopAccessListener() {
        @Override
        public void accessChanging() {
            channel.pause();
        }

        @Override
        public void accessChanged() {
            updateAccess();
        }
    };

    public DDDesktopConnectionService(@NotNull DDDesktopAccessService access, @NotNull DDDesktopMessageChannel channel) {
        this.access = access;
        this.channel = channel;
    }

    @NotNull
    public static synchronized DDDesktopConnectionService getInstance() throws DBException {
        if (instance == null) {
            DDDesktopAccessService access = DDDesktopAccessService.getInstance();
            DDHello hello = new DDHello(
                DBWorkbench.getPlatform().getDeploymentId(), APPLICATION_RUN_ID,
                GeneralUtils.getProductName(), GeneralUtils.getProductVersion().toString());
            DDDesktopMessageChannel channel = new DDDesktopMessageChannel(
                URI.create(DDEndpoints.getExternalDamWebSocketUrl()), hello,
                resource -> access.getStatus() == DDDesktopAccessService.Status.SIGNED_IN
                    ? access.getAccessTokenForResource(resource) : null);
            instance = new DDDesktopConnectionService(access, channel);
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
        updateAccess();
    }

    @NotNull
    public DDMessageChannel getChannel() {
        return channel;
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            access.removeAccessListener(accessListener);
            channel.close();
        }
    }

    private void updateAccess() {
        if (closed.get() || !started.get()) {
            return;
        }
        if (access.getStatus() == DDDesktopAccessService.Status.SIGNED_IN) {
            channel.start();
        } else {
            channel.pause();
        }
    }
}
