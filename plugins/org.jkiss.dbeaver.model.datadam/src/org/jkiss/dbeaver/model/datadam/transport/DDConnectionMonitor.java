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
package org.jkiss.dbeaver.model.datadam.transport;

import com.dbeaver.datadam.dam.remote.api.transport.DDRemoteDamChannelState;
import com.dbeaver.datadam.dam.remote.api.transport.DDRemoteDamChannelStatus;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.datadam.mcp.function.DDMcpAiFunctionCatalog;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bounded, in-memory diagnostics for the current or most recent WebSocket connection.
 * Traffic counts application payload bytes, excluding WebSocket framing, control frames and TLS overhead.
 */
public final class DDConnectionMonitor {
    private static final int MAX_EVENTS = 10_000;
    private static final int MAX_DETAILS_LENGTH = 100_000;

    public enum EventType {
        CONNECTING, CONNECTED, DISCONNECTED, CLOSED, SIGNED_IN, SIGNED_OUT, INVALID_TOKEN, ERROR,
        MCP_REQUEST
    }

    public record Event(@NotNull Instant time, @NotNull EventType type, @NotNull String details, @Nullable McpRequest request) {
        public Event(@NotNull Instant time, @NotNull EventType type, @NotNull String details) {
            this(time, type, details, null);
        }
    }

    public record McpRequest(
        @NotNull String operation,
        @Nullable String projectId,
        @Nullable String datasourceId,
        @NotNull Map<String, String> parameters
    ) {
        public McpRequest {
            parameters = Collections.unmodifiableMap(new LinkedHashMap<>(parameters));
        }
    }

    public record Snapshot(
        @NotNull DDRemoteDamChannelState state,
        @Nullable Instant connectedAt,
        @NotNull Duration duration,
        long bytesSent,
        long bytesReceived,
        @Nullable String error
    ) {
    }

    private final ArrayDeque<Event> events = new ArrayDeque<>();
    private DDRemoteDamChannelState state = DDRemoteDamChannelState.DISCONNECTED;
    @Nullable
    private Instant connectedAt;
    @Nullable
    private String error;
    private long startedNanos;
    private long durationNanos;
    private boolean open;
    private long bytesSent;
    private long bytesReceived;

    synchronized void opened() {
        connectedAt = Instant.now();
        startedNanos = System.nanoTime();
        durationNanos = 0;
        bytesSent = 0;
        bytesReceived = 0;
        open = true;
    }

    synchronized void disconnected() {
        if (open) {
            durationNanos = System.nanoTime() - startedNanos;
            open = false;
        }
    }

    synchronized void stateChanged(@NotNull DDRemoteDamChannelStatus status) {
        state = status.state();
        if (status.isReady()) {
            error = null;
        }
        record(EventType.valueOf(state.name()), status.session() == null ? "" : status.session().damSessionId());
    }

    synchronized void sent(long bytes) {
        bytesSent += bytes;
    }

    synchronized void received(long bytes) {
        bytesReceived += bytes;
    }

    public synchronized void failed(@NotNull String message) {
        error = limit(message);
        record(EventType.ERROR, error);
    }

    public synchronized void clearError(@NotNull String expectedError) {
        if (expectedError.equals(error)) {
            error = null;
        }
    }

    public synchronized void record(@NotNull EventType type, @NotNull String details) {
        addEvent(new Event(Instant.now(), type, limit(details)));
    }

    public synchronized void recordMcpRequest(@NotNull String operation, @Nullable Map<String, Object> arguments) {
        if (arguments == null) {
            record(EventType.MCP_REQUEST, operation);
            return;
        }
        Map<String, String> parameters = new LinkedHashMap<>();
        int remaining = MAX_DETAILS_LENGTH;
        long valueLength = 0;
        for (var parameter : arguments.entrySet()) {
            if (parameter.getKey().length() >= remaining) {
                break;
            }
            remaining -= parameter.getKey().length();
            String value = parameter.getValue() instanceof String string ? string : JSONUtils.PRETTY_GSON.toJson(parameter.getValue());
            parameters.put(parameter.getKey(), value);
            valueLength += value.length();
        }
        if (valueLength > remaining) {
            // Trim the largest values first, preserving short parameters such as connection identifiers.
            long excess = valueLength - remaining;
            var largestFirst = parameters.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, String>>comparingInt(entry -> entry.getValue().length()).reversed()).toList();
            for (var parameter : largestFirst) {
                int removed = (int) Math.min(excess, parameter.getValue().length());
                parameter.setValue(parameter.getValue().substring(0, parameter.getValue().length() - removed));
                excess -= removed;
                if (excess == 0) {
                    break;
                }
            }
        }
        // Keep connection identifiers separately so a long preceding argument cannot truncate them away.
        var request = new McpRequest(limit(operation),
            argumentId(arguments, DDMcpAiFunctionCatalog.PROJECT_ID_PARAMETER),
            argumentId(arguments, DDMcpAiFunctionCatalog.DATASOURCE_ID_PARAMETER), parameters);
        addEvent(new Event(Instant.now(), EventType.MCP_REQUEST,
            limit(operation + "\n\n" + JSONUtils.PRETTY_GSON.toJson(arguments)), request));
    }

    @Nullable
    private static String argumentId(@NotNull Map<String, Object> arguments, @NotNull String name) {
        return arguments.get(name) instanceof String id ? limit(id) : null;
    }

    private void addEvent(@NotNull Event event) {
        if (events.size() == MAX_EVENTS) {
            events.removeFirst();
        }
        events.addLast(event);
    }

    @NotNull
    public synchronized Snapshot snapshot() {
        return new Snapshot(state, connectedAt, Duration.ofNanos(open ? System.nanoTime() - startedNanos : durationNanos),
            bytesSent, bytesReceived, error);
    }

    @NotNull
    public synchronized List<Event> getEvents() {
        return List.copyOf(events);
    }

    public synchronized void clearEvents() {
        events.clear();
    }

    @NotNull
    private static String limit(@NotNull String text) {
        return text.length() <= MAX_DETAILS_LENGTH ? text : text.substring(0, MAX_DETAILS_LENGTH);
    }
}
