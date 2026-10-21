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

import com.dbeaver.datadam.external.api.rpc.DDRpcServices;
import com.dbeaver.datadam.external.api.rpc.DDRpcTransport;
import com.dbeaver.datadam.external.api.transport.DDChannelStatus;
import com.dbeaver.datadam.external.api.transport.DDMessageChannel;
import com.dbeaver.datadam.external.api.transport.DDSubscription;
import com.dbeaver.messaging.requestreply.RequestReplyException;
import com.dbeaver.messaging.rpc.RpcCodec;
import com.dbeaver.messaging.rpc.RpcEndpoint;
import com.google.gson.JsonElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class DDDesktopRpcService implements AutoCloseable {
    private static final Log log = Log.getLog(DDDesktopRpcService.class);
    private static final int MAX_CUSTOM_SERVICES = 32;

    private final Object lock = new Object();
    private final DDMessageChannel channel;
    private final RpcCodec<JsonElement> codec;
    private final Map<String, Lane> lanes = new HashMap<>();
    @Nullable
    private DDSubscription observation;
    private boolean observing;
    private boolean accessObserved;
    private boolean active;
    private boolean closed;

    DDDesktopRpcService(@NotNull DDMessageChannel channel, @NotNull RpcCodec<JsonElement> codec) {
        this.channel = channel;
        this.codec = codec;
    }

    void start() {
        synchronized (lock) {
            if (closed || observing) {
                return;
            }
            observing = true;
        }
        DDSubscription opened = channel.observeState(this::stateChanged);
        synchronized (lock) {
            if (!closed) {
                observation = opened;
                return;
            }
        }
        opened.close();
    }

    boolean resume(boolean initial) {
        synchronized (lock) {
            if (closed || initial && accessObserved) {
                return false;
            }
            accessObserved |= !initial;
            active = true;
        }
        stateChanged(channel.getStatus());
        return true;
    }

    boolean suspend(boolean initial) {
        List<RpcEndpoint> stopped;
        synchronized (lock) {
            if (closed || initial && accessObserved) {
                return false;
            }
            accessObserved |= !initial;
            active = false;
            stopped = invalidate();
        }
        closeEndpoints(stopped);
        return true;
    }

    @NotNull
    RpcEndpoint getRpc(@NotNull String serviceId) throws DBException {
        String topic;
        try {
            topic = DDRpcServices.topic(serviceId);
        } catch (IllegalArgumentException e) {
            throw new DBException("Invalid DataDam RPC service ID", e);
        }
        while (true) {
            Lane lane;
            synchronized (lock) {
                requireActive();
                lanes.values().removeIf(value -> value.endpoint != null && value.endpoint.isCloseComplete());
                lane = lanes.get(serviceId);
                if (lane != null && (lane.invalidated || lane.endpoint != null && lane.endpoint.isClosed())) {
                    throw new DBException("DataDam RPC service is still closing");
                }
                if ((lane == null || lane.endpoint == null) && !channel.getStatus().isReady()) {
                    throw new DBException("DataDam RPC requires a ready message channel");
                }
                if (lane == null) {
                    if (!isDefault(serviceId)
                        && lanes.keySet().stream().filter(id -> !isDefault(id)).count() >= MAX_CUSTOM_SERVICES) {
                        throw new DBException("DataDam RPC service limit reached");
                    }
                    lane = new Lane();
                    lanes.put(serviceId, lane);
                }
            }
            synchronized (lane) {
                synchronized (lock) {
                    requireActive();
                    if (lanes.get(serviceId) != lane) {
                        throw new DBException("DataDam RPC identity or endpoint changed while opening");
                    }
                    if (lane.invalidated) {
                        throw new DBException("DataDam RPC service is still closing");
                    }
                    if (lane.endpoint != null) {
                        if (lane.endpoint.isClosed()) {
                            if (lane.endpoint.isCloseComplete()) {
                                lanes.remove(serviceId, lane);
                                continue;
                            }
                            throw new DBException("DataDam RPC service is still closing");
                        }
                        return lane.endpoint;
                    }
                }
                RpcEndpoint opened;
                try {
                    opened = RpcEndpoint.open(DDRpcTransport.forChannel(channel, topic), codec);
                } catch (RequestReplyException | RuntimeException e) {
                    synchronized (lock) {
                        lanes.remove(serviceId, lane);
                    }
                    throw new DBException("Cannot open DataDam RPC service " + serviceId, e);
                }
                synchronized (lock) {
                    if (!closed && active && !lane.invalidated && lanes.get(serviceId) == lane) {
                        lane.endpoint = opened;
                        return opened;
                    }
                    lane.endpoint = opened;
                    lane.invalidated = true;
                }
                closeEndpoints(List.of(opened));
                synchronized (lock) {
                    if (opened.isCloseComplete()) {
                        lanes.remove(serviceId, lane);
                    }
                }
                throw new DBException("DataDam RPC identity changed while opening");
            }
        }
    }

    private void stateChanged(@NotNull DDChannelStatus status) {
        if (status.isReady()) {
            for (String serviceId : List.of(DDRpcServices.ACCOUNT, DDRpcServices.STORAGE)) {
                try {
                    getRpc(serviceId);
                } catch (DBException e) {
                    log.debug("DataDam RPC service is unavailable: " + serviceId, e);
                }
            }
        }
    }

    private void requireActive() throws DBException {
        if (closed) {
            throw new DBException("Desktop DataDam connection service is closed");
        }
        if (!active) {
            throw new DBException("Desktop DataDam authorization is unavailable");
        }
    }

    private static boolean isDefault(@NotNull String serviceId) {
        return DDRpcServices.ACCOUNT.equals(serviceId) || DDRpcServices.STORAGE.equals(serviceId);
    }

    @NotNull
    private List<RpcEndpoint> invalidate() {
        List<RpcEndpoint> stopped = new ArrayList<>();
        for (Lane lane : lanes.values()) {
            lane.invalidated = true;
            if (lane.endpoint != null) {
                stopped.add(lane.endpoint);
            }
        }
        return stopped;
    }

    private static void closeEndpoints(@NotNull List<RpcEndpoint> endpoints) {
        for (RpcEndpoint endpoint : endpoints) {
            try {
                endpoint.close();
            } catch (RuntimeException e) {
                log.warn("Cannot close DataDam RPC endpoint", e);
            }
        }
    }

    @Override
    public void close() {
        List<RpcEndpoint> stopped;
        DDSubscription stoppedObservation;
        synchronized (lock) {
            if (closed) {
                return;
            }
            closed = true;
            active = false;
            stopped = invalidate();
            lanes.clear();
            stoppedObservation = observation;
            observation = null;
        }
        closeEndpoints(stopped);
        if (stoppedObservation != null) {
            stoppedObservation.close();
        }
    }

    private static final class Lane {
        private boolean invalidated;
        @Nullable
        private RpcEndpoint endpoint;
    }
}
