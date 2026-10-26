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

import com.dbeaver.datadam.messaging.api.rpc.DDMessagingGsonRpcCodec;
import com.dbeaver.datadam.messaging.api.rpc.DDMessagingRpcTransport;
import com.dbeaver.datadam.messaging.api.transport.DDMessagingChannel;
import com.dbeaver.messaging.rpc.MessagingRpcEndpoint;
import com.google.gson.Gson;
import org.eclipse.core.runtime.jobs.Job;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;

final class DDMessagingDesktopRpcService implements AutoCloseable {
    private static final Log log = Log.getLog(DDMessagingDesktopRpcService.class);

    private final Object lock = new Object();
    private final DDMessagingChannel channel;
    @Nullable
    private MessagingRpcEndpoint endpoint;
    private boolean invalidated;
    private boolean accessObserved;
    private boolean active;
    private boolean closed;

    DDMessagingDesktopRpcService(@NotNull DDMessagingChannel channel) {
        this.channel = channel;
    }

    boolean resume(boolean initial) {
        synchronized (lock) {
            if (closed || initial && accessObserved) {
                return false;
            }
            accessObserved |= !initial;
            active = true;
            return true;
        }
    }

    boolean suspend(boolean initial) {
        MessagingRpcEndpoint stopped;
        synchronized (lock) {
            if (closed || initial && accessObserved) {
                return false;
            }
            accessObserved |= !initial;
            active = false;
            invalidated = true;
            stopped = endpoint;
        }
        closeEndpoint(stopped);
        return true;
    }

    @NotNull
    MessagingRpcEndpoint getRpc() throws DBException {
        synchronized (lock) {
            requireActive();
            if (endpoint != null && endpoint.isCloseComplete()) {
                endpoint = null;
            }
            if (endpoint != null) {
                if (invalidated || endpoint.isClosed()) {
                    throw new DBException("DataDam RPC endpoint is still closing");
                }
                return endpoint;
            }
            if (!channel.getStatus().isReady()) {
                throw new DBException("DataDam RPC requires a ready message channel");
            }
            try {
                endpoint = MessagingRpcEndpoint.open(
                    topic -> DDMessagingRpcTransport.forChannel(channel, topic, DDMessagingDesktopRpcService::executeRequest),
                    new DDMessagingGsonRpcCodec(new Gson()));
                invalidated = false;
                return endpoint;
            } catch (RuntimeException e) {
                throw new DBException("Cannot open DataDam RPC endpoint", e);
            }
        }
    }

    private static void executeRequest(@NotNull Runnable request) {
        Job.createSystem("Handle DataDam RPC request", monitor -> {
            request.run();
        }).schedule();
    }

    private void requireActive() throws DBException {
        if (closed) {
            throw new DBException("Desktop DataDam connection service is closed");
        }
        if (!active) {
            throw new DBException("Desktop DataDam authorization is unavailable");
        }
    }

    private static void closeEndpoint(@Nullable MessagingRpcEndpoint endpoint) {
        if (endpoint != null) {
            try {
                endpoint.close();
            } catch (RuntimeException e) {
                log.warn("Cannot close DataDam RPC endpoint", e);
            }
        }
    }

    @Override
    public void close() {
        MessagingRpcEndpoint stopped;
        synchronized (lock) {
            if (closed) {
                return;
            }
            closed = true;
            active = false;
            invalidated = true;
            stopped = endpoint;
        }
        closeEndpoint(stopped);
    }
}
