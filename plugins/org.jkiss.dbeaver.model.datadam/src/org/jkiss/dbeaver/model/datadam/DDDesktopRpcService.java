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

import com.dbeaver.datadam.external.api.rpc.DDGsonRpcCodec;
import com.dbeaver.datadam.external.api.rpc.DDRpcTransport;
import com.dbeaver.datadam.external.api.transport.DDMessageChannel;
import com.dbeaver.messaging.rpc.RpcEndpoint;
import com.google.gson.Gson;
import org.eclipse.core.runtime.jobs.Job;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;

final class DDDesktopRpcService implements AutoCloseable {
    private static final Log log = Log.getLog(DDDesktopRpcService.class);

    private final Object lock = new Object();
    private final DDMessageChannel channel;
    @Nullable
    private RpcEndpoint endpoint;
    private boolean invalidated;
    private boolean accessObserved;
    private boolean active;
    private boolean closed;

    DDDesktopRpcService(@NotNull DDMessageChannel channel) {
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
        RpcEndpoint stopped;
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
    RpcEndpoint getRpc() throws DBException {
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
                endpoint = RpcEndpoint.open(
                    topic -> DDRpcTransport.forChannel(channel, topic, DDDesktopRpcService::executeRequest),
                    new DDGsonRpcCodec(new Gson()));
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

    private static void closeEndpoint(@Nullable RpcEndpoint endpoint) {
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
        RpcEndpoint stopped;
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
